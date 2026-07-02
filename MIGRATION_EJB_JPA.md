# Migration to Full EJB + JPA (Jakarta EE 10)

This document explains the migration of the ERP Grocery System from a hand-wired,
embedded-Jetty, raw-JDBC application into a **full Jakarta EE 10** application
(EJB + JPA + CDI + JMS + JAX-RS) packaged as a WAR and deployed to an application
server (WildFly / Payara).

It is written for review: it covers **what changed**, **how the system works now**,
**how that differs from the original approach**, the **schema divergences** introduced
by Hibernate-generated DDL, the **residual couplings** that remain, and exactly what
**still needs to be done to deploy and run** it.

Build status: `mvn package` → **BUILD SUCCESS**, producing `target/grocery-erp.war`
(all 72 classes compile against the Jakarta EE 10 APIs). "Compiles + packages" is
verified; "runs end-to-end" requires the app-server setup in the last section.

---

## 1. Why this migration (the original problem)

The original beans embedded a nested `DAO` class each (e.g. `CustomerBean.DAO`,
`ProductBean.DAO`). Every DAO:

- opened its own JDBC `Connection` via `DatabaseManager.getConnection()`,
- wrote **SQLite-specific SQL** by hand (`INSERT OR REPLACE`, `ON CONFLICT ... DO UPDATE`,
  `substr()`, `LIKE 'period%'`),
- managed its own commit/close.

This made every data-owning component **tightly coupled to both the database engine
(SQLite) and the persistence mechanism (raw JDBC)** — a violation of the loose-coupling
goal of component-based design. The chosen fix (Option 3) was to replace JDBC + DAOs
with **JPA entities + a per-module repository EJB**, and to let the **container** own
transactions, wiring, and the database dialect.

---

## 2. What changed — at a glance

| Concern | Before | After |
|---|---|---|
| Packaging | Fat JAR (`maven-shade-plugin`) | **WAR** (`maven-war-plugin`) |
| Runtime | Embedded **Jetty** (`ApiServer`, `main()`) | **WildFly / Payara** app server |
| Persistence | Raw JDBC in nested `*.DAO` classes | **JPA** `@Entity` + `EntityManager` |
| Transactions | Manual `Connection`/commit/close | **Container-managed JTA** (EJB default `REQUIRED`) |
| Wiring (IoC) | 8-step manual setter wiring in `Main` | **CDI** `@Inject` / **EJB** `@EJB` |
| Web layer | `HttpServlet` subclasses + Jetty | **JAX-RS** `@Path` resources |
| Schema | Hand-written DDL in `DatabaseManager` | **Hibernate `hbm2ddl.auto=update`** from `@Entity` |
| Messaging | Fake "MDB" — a plain object called synchronously | **Real JMS** `@MessageDriven` + producer |
| SSE broadcaster | Hand-rolled `getInstance()` singleton | **`@Singleton` EJB** |
| DB engine coupling | Spread across every DAO | **One line** in `persistence.xml` (`hibernate.dialect`) |

### Files deleted (obsolete bootstrap / web plumbing)
`integration/Main.java`, `api/ApiServer.java`, `db/DatabaseManager.java`,
`db/Database.java`, `api/BaseServlet.java`, `api/CorsServlet.java`, and all legacy
servlets (`CustomerServlet`, `DashboardServlet`, `HrServlet`, `ReportingServlet`,
`SupplierServlet`, plus `PosServlet`/`InventoryServlet`/`ProductServlet`/`FinanceServlet`
removed during their module conversions).

### Files added (infrastructure)
- `src/main/resources/META-INF/persistence.xml` — single JTA persistence unit, 21 entities.
- `src/main/webapp/WEB-INF/beans.xml` — `bean-discovery-mode="all"` (CDI replaces manual wiring).
- `api/JaxRsApplication.java` — `@ApplicationPath("/api")` (replaces Jetty servlet registration).
- `api/CorsFilter.java` — `@Provider ContainerResponseFilter` (replaces `CorsServlet`).
- One `*Repository` EJB per module + one `*Resource` JAX-RS class per module.

---

## 3. How the system works now

### 3.1 Persistence: entities + repositories

Each formerly-DAO-bearing bean is now a plain **`@Entity`** — a mapped table row with
getters/setters and a no-arg constructor, no SQL inside it. Example:

```java
@Entity @Table(name = "customers")
public class CustomerBean implements Serializable {
    @Id @Column(name = "customerId") private String customerId;
    @Column(name = "name")           private String name;
    // ... getters/setters only
}
```

All persistence for a module lives in a **`@Stateless` repository EJB** holding a
container-injected `EntityManager`:

```java
@Stateless
public class CustomerRepository {
    @PersistenceContext(unitName = "groceryerp")
    private EntityManager em;

    public void saveCustomer(CustomerBean c) { em.merge(c); }
    public CustomerBean findCustomerById(String id) { return em.find(CustomerBean.class, id); }
    public List<CustomerBean> findAllCustomers() {
        return em.createQuery("SELECT c FROM CustomerBean c ORDER BY c.name",
                              CustomerBean.class).getResultList();
    }
}
```

`em.merge()` emits the **dialect-correct upsert**, so the hand-written
`INSERT OR REPLACE` is gone. The 21 entities and the SQLite dialect are declared once in
`persistence.xml`.

### 3.2 Transactions: container-managed JTA

There is no `Connection`/`commit`/`close` anywhere in business code. Every public method
on a `@Stateless`/`@Stateful` bean runs in a JTA transaction (EJB default propagation
`REQUIRED`). The app server begins the transaction on the method boundary and commits or
rolls back on return.

### 3.3 Wiring: CDI + EJB injection (no more `Main`)

The original `Main` did ~8 steps of manual `setXxx()` wiring to connect modules through
their `I*` interfaces. That is **deleted**. The container now wires everything:

```java
@Stateless
public class POSModule implements ISalesData, IReceiptService {
    @Inject private IStoreInventory storeInventory;     // was setStoreInventory(...)
    @Inject private ICustomerData   customerData;        // was setCustomerData(...)
    @EJB    private POSRepository    repository;          // was new ...DAO()
}
```

`beans.xml` with `bean-discovery-mode="all"` turns on CDI discovery for the whole module.

### 3.4 Web layer: JAX-RS resources

Each `*Servlet` became a `@Path` resource. The servlets used to **carry their own raw
SQL** (cross-table JOINs); that SQL moved into the module repositories, so no SQL strings
remain in the web layer. The API is mounted at `/api` by
`@ApplicationPath("/api")` (same external path as the old Jetty context).

### 3.5 Messaging: a real Message-Driven Bean

Before, `StockAlertMDB` was a plain object whose `onMessage(String, Object)` was **called
synchronously** like any method — it had the `@MessageDriven` *name* but none of the
semantics. Now it is genuine asynchronous JMS:

```
StoreInventoryBean.updateStock() detects low stock
  → StockAlertProducer.fireLowStock(alert)         // sends ObjectMessage to the queue, returns immediately
  → (container delivers on another thread)
  → StockAlertMDB.onMessage(Message)               // @MessageDriven, implements MessageListener
       → InventoryRepository.saveAlert(alert)        // persists
       → AlertBroadcaster.broadcast(productId)       // pushes SSE to supplier portal
```

- **Producer** (`StockAlertProducer`, `@Stateless`): injects `JMSContext`, sends a
  `StockAlertEvent` (Serializable) to `java:/jms/queue/StockAlert`.
- **Consumer** (`StockAlertMDB`, `@MessageDriven implements MessageListener`): bound to the
  same queue via `@ActivationConfigProperty`.
- **Broadcaster** (`AlertBroadcaster`): now a **`@Singleton`** EJB with
  `@ConcurrencyManagement(BEAN)` so its `ConcurrentHashMap` of SSE writers keeps handling
  concurrency itself. It looks up which suppliers stock a product via
  `SupplierRepository.findSupplierIdsForProduct(productId)`.

---

## 4. How it differs from the original approach (conceptually)

| Dimension | Original (JDBC + DAO + Jetty) | Now (EJB + JPA + app server) |
|---|---|---|
| Who owns the DB connection | the application (DAO) | the **container** (JTA datasource) |
| Who owns transactions | the application (manual) | the **container** (declarative) |
| Who wires components | `Main` (by hand) | the **container** (CDI/EJB) |
| Where the SQL lives | inside every bean + every servlet | inside **repositories only**, mostly as JPQL |
| DB-engine coupling | in every DAO string | one `hibernate.dialect` line |
| Schema source of truth | hand-written DDL | **`@Entity` annotations** (Hibernate-generated) |
| "MDB" | a synchronous method call | a real **asynchronous** JMS consumer |
| Deploy unit | self-contained fat JAR you `java -jar` | a **WAR** you drop into an app server |

The headline trade-off: the system gained **loose coupling and container services** but
lost **self-containment**. It no longer has a `main()` you can run directly; it needs an
app server that provides the datasource, JMS broker, JPA provider, and CDI/EJB containers.

---

## 5. Schema divergences (Hibernate-generated vs hand-written DDL)

`hibernate.hbm2ddl.auto=update` builds the schema from the `@Entity` mappings. This is
**not byte-for-byte identical** to the old `DatabaseManager` DDL:

1. **No FK constraints / DEFAULTs unless annotated.** `update` creates tables/columns from
   the entity fields but will not reproduce the original `FOREIGN KEY` or `DEFAULT` clauses
   unless they are expressed via `@JoinColumn`/`@ForeignKey`/`columnDefinition`. The
   entities here map columns by name only, so referential integrity that was enforced in DDL
   is now enforced only by application logic.
2. **Tables with no `@Entity` are NOT created by Hibernate.** The following are still
   accessed via **native SQL** and must exist in the DB (created by app logic or a seed
   script): `stock`, `stores`, `supplier_products`. They have composite/managed keys that
   were never modelled as entities.
3. **`update` never drops or narrows.** Removing a field will not drop its column; it only
   adds what's missing. For a clean schema, start from an empty DB file.
4. **The SQLite-specific upserts moved to Java or native queries** (see §6), so the runtime
   SQL Hibernate emits for the entity tables is dialect-portable; only the explicitly
   native queries remain SQLite-flavoured.

---

## 6. Residual couplings (documented, intentional)

These are the places the migration could **not** make fully portable without changing
behaviour. Each is reproduced as a **native query** with the original SQL preserved in a
comment:

- **`DashboardRepository`** — cross-module read-only aggregates spanning `sales`, `stores`,
  `stock`, `products` (`substr(timestamp,1,10)`, `COALESCE`). Dialect-specific by design.
- **`InventoryRepository`** — `stock` upsert (`ON CONFLICT(storeId,productId) DO UPDATE`),
  `stock` reads, `stores` load/save, and the `purchase_orders`/`order_lines` restock guard.
  These tables have no `@Entity`.
- **`SupplierRepository`** — joins touching `stores` and `supplier_products` (no entity),
  including `findSupplierIdsForProduct` used by the SSE broadcaster.
- **`DataCollector`** (reporting) — two aggregates over `stock` / `stock_alerts`. These were
  raw JDBC via `DatabaseManager`; they now run through the `EntityManager` as native
  queries (same SQL, no manual JDBC, no `DatabaseManager` dependency).
- **Non-portable upserts moved to Java** rather than native SQL where business logic allowed:
  `CustomerRepository.addPoints()` reimplements the SQLite `ON CONFLICT ... CASE` loyalty-tier
  recalculation as a read-modify-write inside the JTA transaction (thresholds 500 / 1500
  preserved exactly).

To make the native-query spots portable later, give the orphan tables (`stock`, `stores`,
`supplier_products`) JPA entities and rewrite those queries as JPQL.

---

## 7. What still needs to be done to deploy and run

The WAR **compiles and packages**. To make it **run**, configure the app server (the steps
below are for WildFly; Payara is analogous):

1. **Install an app server** (WildFly 31+ or Payara 6, both Jakarta EE 10).
2. **Register the JTA datasource** named `java:/GroceryErpDS` (referenced in
   `persistence.xml`) pointing at the SQLite DB file `db/grocery_erp.db`. This needs a JDBC
   driver module for `org.xerial:sqlite-jdbc` and the
   `org.hibernate.community.dialect.SQLiteDialect` (the `hibernate-community-dialects`
   dependency). Add it in `standalone.xml` under `<datasources>`.
3. **Create the JMS queue** `java:/jms/queue/StockAlert` (referenced by the producer and
   the MDB's `destinationLookup`). In WildFly this is a `jms-queue` entry under the
   `messaging-activemq` subsystem.
4. **Seed the non-entity tables** `stock`, `stores`, `supplier_products` (Hibernate's
   `update` will create the 21 entity tables but not these). Either create them with a small
   DDL/seed script or let the relevant code paths create rows on first write.
5. **Deploy** `target/grocery-erp.war`. The REST API comes up at
   `http://<host>:8080/grocery-erp/api/...`.
6. **Frontend is unchanged.** The `frontend/` and `supplier-portal/` SPA builds are served
   independently (as before — the old `ApiServer` only served `/api`, never the UI). Point
   their API base URL at the deployed `/grocery-erp/api` context.

### Caveats worth knowing
- SQLite + JTA + a connection pool is fine for a single-node demo but SQLite's single-writer
  locking will serialize concurrent writes; for real concurrency move the dialect/datasource
  to PostgreSQL (one-line dialect swap + a new datasource).
- Field injection (`@EJB`/`@Inject` on fields) is used throughout; this is idiomatic for EJB
  components and matches the reference Customer module. IDE "use constructor injection"
  hints (`java:S6813`) are intentional and can be ignored.

---

## 8. Module-by-module status

All six business modules follow the Customer reference pattern (entities + repository EJB +
JAX-RS resource); the JMS and dashboard slices are cross-cutting.

| Module | Entities (→ table) | Repository | Resource | Notes |
|---|---|---|---|---|
| Customer | CustomerBean→customers, LoyaltyBean→loyalty, PurchaseHistoryBean→purchase_history | CustomerRepository | CustomerResource | loyalty-tier upsert moved to Java |
| POS | Sale/SaleItem/Receipt/Payment/Discount | POSRepository | PosResource | injects Inventory/Finance/Customer repos cross-module |
| Inventory | ProductBean→products, StockAlertBean→stock_alerts | InventoryRepository | InventoryResource | owns `stock`/`stores` native access |
| Finance | Expense/Revenue/Tax | FinanceRepository | FinanceResource | `SUM()→0.0` coalescing; ProfitSummaryBean stays a DTO |
| HR | Employee/Payroll/Shift/Attendance | HRRepository | HrResource | HRModule kept `@Stateful` (multi-step payroll flow + `@Remove`) |
| Supplier | Supplier/PurchaseOrder/OrderLine/Delivery | SupplierRepository | SupplierResource | joins on `stores`/`supplier_products` native |
| Reporting | Report/SalesSummary/InventoryReport (plain DTOs — no tables) | — (none) | ReportingResource | DataCollector aggregates via native queries |
| Dashboard | — | DashboardRepository | DashboardResource | cross-module read-only native aggregates |
