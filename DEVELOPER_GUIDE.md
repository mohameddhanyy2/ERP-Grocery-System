# ERP Grocery System — Developer Guide

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Technology Stack](#2-technology-stack)
3. [Project Structure](#3-project-structure)
4. [CBSE Architecture](#4-cbse-architecture)
5. [JavaBeans in This System](#5-javabeans-in-this-system)
6. [EJB Session Bean Types](#6-ejb-session-bean-types)
7. [Inversion of Control & Dependency Injection](#7-inversion-of-control--dependency-injection)
8. [Component Interface Catalogue](#8-component-interface-catalogue)
9. [Module-by-Module Reference](#9-module-by-module-reference)
10. [The Supplier Flow End-to-End](#10-the-supplier-flow-end-to-end)
11. [Real-Time Alerts — SSE](#11-real-time-alerts--sse)
12. [Database Schema](#12-database-schema)
13. [REST API Reference](#13-rest-api-reference)
14. [Frontend Architecture](#14-frontend-architecture)
15. [Running the System](#15-running-the-system)

---

## 1. System Overview

ERP Grocery System is a full-stack enterprise resource planning application for a grocery chain. It manages inventory across multiple store branches, supplier procurement, point-of-sale, HR/payroll, finance, customer loyalty, and reporting — all from a single backend with two frontend applications.

The backend is built entirely on Component-Based Software Engineering (CBSE) principles using JavaBeans and simulated EJB patterns. No EJB container is used; the patterns are implemented manually to demonstrate the concepts.

---

## 2. Technology Stack

| Layer | Technology |
|---|---|
| Backend language | Java 17 |
| HTTP server | Eclipse Jetty (embedded) |
| Database | SQLite via JDBC |
| JSON | Jackson ObjectMapper |
| Build tool | Maven |
| Manager frontend | React 18 + Vite + Tailwind CSS (port 5173) |
| Supplier portal | React 18 + Vite + Tailwind CSS (port 5174) |
| Real-time push | Server-Sent Events (SSE) |

---

## 3. Project Structure

```
ERP-Grocery-System/
├── src/main/java/com/groceryerp/
│   ├── integration/        Main.java — IoC assembler (entry point)
│   ├── api/                HTTP servlet layer (REST endpoints + SSE)
│   ├── db/                 DatabaseManager — schema init + JDBC utilities
│   ├── interfaces/         All provided/required component interfaces
│   ├── inventory/          CentralInventoryBean, StoreInventoryBean, StockAlertMDB
│   ├── supplier/           SupplierModule + supplier beans
│   ├── pos/                POSModule + POS beans
│   ├── hr/                 HRModule + HR beans
│   ├── finance/            FinanceModule + finance beans
│   ├── customer/           CustomerModule + customer beans
│   └── reporting/          ReportingModule + reporting beans + services
├── frontend/               Manager React app (port 5173)
├── supplier-portal/        Supplier React app (port 5174)
├── db/                     grocery_erp.db (SQLite file, auto-created)
└── pom.xml
```

---

## 4. CBSE Architecture

### 4.1 What is CBSE?

Component-Based Software Engineering (CBSE) structures a system as a set of independently deployable, self-contained units called **components**. Each component:

- Exposes **provided interfaces** (what it offers to others)
- Declares **required interfaces** (what it needs from others)
- Hides its internal implementation entirely
- Is assembled by an external **IoC assembler**, not by other components

This system applies CBSE at the module level. Every module is a component in the CBSE sense.

### 4.2 Component Diagram

```
                        ┌─────────────────────────────────────────┐
                        │             Main.java (IoC Assembler)    │
                        └───────────────────┬─────────────────────┘
                                            │ wires via setter injection
          ┌──────────────┬─────────────────┬┴────────────────┬─────────────────┐
          ▼              ▼                 ▼                 ▼                 ▼
   ┌─────────────┐ ┌──────────┐   ┌──────────────┐  ┌──────────────┐ ┌──────────────┐
   │  HRModule   │ │POSModule │   │SupplierModule│  │FinanceModule │ │ReportingModule│
   │ @Stateful   │ │@Stateless│   │ @Stateless   │  │ @Stateless   │ │ @Stateless   │
   └──────┬──────┘ └────┬─────┘   └──────┬───────┘  └──────┬───────┘ └──────┬───────┘
          │             │                │                  │                │
          │        ┌────┴─────┐   ┌──────┴──────┐          │                │
          │        │CustomerMod│  │CentralInv.  │          │                │
          │        │@Stateless │  │Bean @Stateful│          │                │
          │        └───────────┘  └──────┬───────┘          │                │
          │                             │                   │                │
          │                     ┌───────┴──────┐            │                │
          │                     │StoreInventory│            │                │
          │                     │Bean @Stateful│            │                │
          │                     └──────┬───────┘            │                │
          │                            │ fires LOW_STOCK     │                │
          │                     ┌──────▼───────┐            │                │
          │                     │StockAlertMDB │            │                │
          │                     │@MessageDriven│            │                │
          │                     └──────┬───────┘            │                │
          │                            │ broadcasts via SSE  │                │
          │                     ┌──────▼───────┐            │                │
          │                     │AlertBroadcast│            │                │
          │                     │er (singleton)│            │                │
          └─────────────────────┴──────────────┴────────────┘────────────────┘
                                    SQLite DB (shared)
```

### 4.3 Composite Pattern in Inventory

The inventory module uses the **Composite structural pattern**:

- `StoreInventoryBean` is the **Leaf** — represents one physical store branch
- `CentralInventoryBean` is the **Composite** — holds a `List<IStoreInventory>` and exposes chain-wide operations

Both implement `IStoreInventory`, so callers cannot tell whether they are addressing one store or the whole chain. This is the core of the Composite pattern.

```java
// CentralInventoryBean — composite
public int getTotalStock(String productId) {
    int total = 0;
    for (IStoreInventory store : stores) {   // delegates to each leaf
        total += store.checkStock(productId);
    }
    return total;
}
```

The composite never creates stores with `new`. Stores are injected via `addStore(IStoreInventory)` — this is IoC applied inside the composite itself.

---

## 5. JavaBeans in This System

### 5.1 The JavaBeans Specification

A class qualifies as a JavaBean when it satisfies three rules:

1. **Public no-argument constructor** — enables instantiation by frameworks and IoC containers
2. **Private fields with public getters and setters** — the `get`/`set` naming convention is what makes fields "properties" that tools can discover and bind
3. **Implements `Serializable`** — allows the bean to be serialized across a network or stored to disk

Every data-carrying class in this system follows all three rules.

### 5.2 Entity Beans (Data Beans)

Entity beans represent one database row. They are annotated `@Entity` / `@Table` in comments to show the intended mapping; actual persistence is handled by a nested `DAO` class rather than a JPA `EntityManager`.

**Example — `SupplierBean`**

```java
// @Entity
// @Table(name="suppliers")
public class SupplierBean implements Serializable {

    // @Id
    private String supplierId;   // primary key
    private String name;
    private String contactEmail;
    private int    leadTimeDays;

    public SupplierBean() {}     // ← JavaBeans rule 1

    // getters and setters for every field ← JavaBeans rule 2
    public String getSupplierId()              { return supplierId; }
    public void   setSupplierId(String id)     { this.supplierId = id; }
    // ...

    public static class DAO { /* JDBC persistence */ }
}
```

All entity beans follow the same pattern. The full list:

| Bean | Table | Module |
|---|---|---|
| `SupplierBean` | `suppliers` | Supplier |
| `PurchaseOrderBean` | `purchase_orders` | Supplier |
| `OrderLineBean` | `order_lines` | Supplier |
| `DeliveryBean` | `deliveries` | Supplier |
| `ProductBean` | `products` | Inventory |
| `StockAlertBean` | `stock_alerts` | Inventory |
| `EmployeeBean` | `employees` | HR |
| `ShiftBean` | `shifts` | HR |
| `PayrollBean` | `payroll` | HR |
| `AttendanceBean` | `attendance` | HR |
| `SaleBean` | `sales` | POS |
| `SaleItemBean` | `sale_items` | POS |
| `ReceiptBean` | `receipts` | POS |
| `PaymentBean` | `payments` | POS |
| `DiscountBean` | `discounts` | POS |
| `CustomerBean` | `customers` | Customer |
| `LoyaltyBean` | `loyalty` | Customer |
| `PurchaseHistoryBean` | `purchase_history` | Customer |
| `ExpenseBean` | `expenses` | Finance |
| `RevenueBean` | `revenue` | Finance |
| `TaxBean` | `tax_records` | Finance |

### 5.3 The Nested DAO Pattern

Every entity bean contains a `public static class DAO` that owns all JDBC operations for that bean's table. This co-locates the bean's schema knowledge with its persistence logic without requiring a separate DAO class per bean.

```java
public class SupplierBean implements Serializable {
    // ... fields and accessors ...

    public static class DAO {
        public void save(SupplierBean s) { /* INSERT OR REPLACE */ }
        public SupplierBean findById(String id) { /* SELECT */ }
        public List<String> findAllIds() { /* SELECT all */ }
    }
}
```

Modules instantiate the DAO directly: `new SupplierBean.DAO()`. Each method opens and closes its own JDBC connection using try-with-resources.

### 5.4 Session Beans vs Entity Beans

The system uses two distinct roles for beans:

| Role | Example | Purpose |
|---|---|---|
| **Entity bean** | `EmployeeBean` | Carries data; maps to a DB row |
| **Session bean** | `HRModule` | Implements business logic; holds or manages state |

Session beans are covered in detail in the next section.

### 5.5 `StoreInventoryBean` — A Bean That Is Also a Component

`StoreInventoryBean` is interesting because it qualifies as both a JavaBean and a CBSE component leaf:

- It follows all JavaBeans rules (no-arg constructor, private fields, public getters/setters, `Serializable`)
- It implements `IStoreInventory` — a provided interface
- It has a required dependency (`StockAlertMDB`) injected via setter
- It is the leaf in the Composite pattern

```java
public class StoreInventoryBean implements IStoreInventory, Serializable {

    private String storeId;
    private String storeName;
    private int    lowStockThreshold = 10;
    private StockAlertMDB stockAlertMDB;   // required — injected

    public StoreInventoryBean() {}         // ← JavaBeans rule 1

    public void setStockAlertMDB(StockAlertMDB mdb) { this.stockAlertMDB = mdb; }

    @Override
    public void updateStock(String productId, int delta) {
        // ... JDBC upsert ...
        if (delta < 0 && stockAlertMDB != null) {
            int newQty = checkStock(productId);
            if (newQty < lowStockThreshold) {
                StockAlertBean alert = new StockAlertBean();
                // populate alert...
                stockAlertMDB.onMessage("LOW_STOCK", alert);  // fire event
            }
        }
    }
}
```

---

## 6. EJB Session Bean Types

The system models three EJB bean types without an actual container. The choice of type for each module is deliberate and documented.

### 6.1 @Stateless Session Bean

**Characteristic:** Every method call is independent. The bean holds no business state between calls. All data comes in through parameters and goes out through return values or DAOs.

**Modules using this pattern:** `SupplierModule`, `POSModule`, `FinanceModule`, `CustomerModule`, `ReportingModule`

**Example — `SupplierModule`**

```java
// @Stateless — every method receives all inputs via parameters,
// reads/writes through DAOs, returns immediately. No order state in memory.
public class SupplierModule implements ISupplierService, IOrderStatus {

    private final PurchaseOrderBean.DAO orderDao = new PurchaseOrderBean.DAO();

    // Required interfaces injected from outside — never instantiated here
    private CentralInventoryBean centralInventory;
    private IStockAlerts stockAlerts;

    public void setCentralInventory(CentralInventoryBean c) { this.centralInventory = c; }
    public void setStockAlerts(IStockAlerts s)              { this.stockAlerts = s; }

    @Override
    public String placeOrder(String supplierId, String productId,
                             int quantity, String storeId) {
        // self-contained: all data arrives as parameters
        PurchaseOrderBean order = new PurchaseOrderBean();
        order.setOrderId("ORD-" + System.currentTimeMillis());
        order.setStatus("ACCEPTED");
        // ...
        orderDao.save(order);
        return order.getOrderId();
    }
}
```

### 6.2 @Stateful Session Bean

**Characteristic:** The bean accumulates state across multiple method calls within a single session. A session has a defined lifecycle: begin → one or more steps → end (`@Remove`).

**Modules using this pattern:** `HRModule`, `CentralInventoryBean`, `StoreInventoryBean`

**Example — `HRModule` payroll session**

`HRModule` is `@Stateful` because payroll is a multi-step conversation. Each step builds on the previous one using fields held in the bean instance:

```java
// @Stateful — intermediate payroll values accumulate across method calls
public class HRModule implements IStaffData, IPayrollService {

    // ── Session state (survives between method calls) ──
    private String       activeEmployeeId;
    private EmployeeBean activeEmployee;
    private List<ShiftBean> activeShifts;
    private double       accumulatedHours;
    private double       computedGrossPay;

    /** Step 1 — begin a payroll session for one employee */
    public void beginPayrollSession(String employeeId) {
        this.activeEmployeeId = employeeId;
        this.activeEmployee   = employeeDao.findById(employeeId);
        this.accumulatedHours = 0.0;
        this.computedGrossPay = 0.0;
    }

    /** Step 2 — load shifts; hours accumulate in session state */
    public void loadShifts(String period) {
        this.activeShifts = shiftDao.findByEmployeeAndPeriod(activeEmployeeId, period);
        for (ShiftBean s : activeShifts) accumulatedHours += s.getHoursWorked();
    }

    /** Step 3 — derive gross pay from session state */
    public void computeGrossPay() {
        computedGrossPay = accumulatedHours * activeEmployee.getHourlyRate();
    }

    /** Step 4 — apply deductions, persist, return result */
    public PayrollBean finalizePayroll(String period) { /* ... */ }

    // @Remove — clears all session state
    public void endSession() {
        activeEmployeeId = null;
        activeEmployee   = null;
        activeShifts     = null;
        accumulatedHours = 0.0;
        computedGrossPay = 0.0;
    }
}
```

The `calculatePayroll()` interface method drives all four steps in sequence, calling `endSession()` at the end — demonstrating correct `@Stateful` lifecycle management.

**`CentralInventoryBean`** is `@Stateful` for a different reason: it holds the store registry (`List<IStoreInventory> stores`) that is built up at startup via repeated `addStore()` calls and must persist across all subsequent requests. `clearStores()` is the `@Remove` method.

**`StoreInventoryBean`** is `@Stateful` because `storeId` and `storeName` are set once at configuration time and implicitly used by every subsequent `checkStock()` and `updateStock()` call — they are session-level configuration, not parameters passed per call.

### 6.3 @MessageDriven Bean

**Characteristic:** Reacts to asynchronous events. Never called directly by business logic — it is triggered by a message. Holds no conversational state; each `onMessage()` call is self-contained.

**Bean:** `StockAlertMDB`

```java
// @MessageDriven — reacts to LOW_STOCK events fired by StoreInventoryBean
public class StockAlertMDB {

    private final StockAlertBean.DAO alertDao = new StockAlertBean.DAO();

    public void onMessage(String messageType, Object payload) {
        if ("LOW_STOCK".equals(messageType)) {
            StockAlertBean alert = (StockAlertBean) payload;
            alertDao.save(alert);                                   // persist
            AlertBroadcaster.getInstance().broadcast(              // notify
                alert.getProductId());
        }
    }
}
```

The MDB is the boundary between the synchronous inventory update and the asynchronous notification system. `StoreInventoryBean.updateStock()` calls `onMessage()` after detecting a threshold breach — the calling code does not wait for or handle the notification result.

**Why @MessageDriven instead of @Stateless?**
A `@Stateless` bean is called by name and returns a value to the caller. A `@MessageDriven` bean is triggered by a message on a channel and returns nothing. The difference is the invocation contract, not the presence of state.

---

## 7. Inversion of Control & Dependency Injection

### 7.1 The IoC Contract

The single rule governing all module wiring in this system:

> **No module instantiates another module.** All cross-module dependencies arrive through setter injection performed exclusively in `Main.java`.

This rule is enforced structurally: modules declare their required dependencies as private fields and expose public setter methods. They never call `new ModuleX()` for any business dependency.

### 7.2 Main.java — The Assembler

`Main.java` is the only class with full knowledge of the object graph. It instantiates every module and wires them in dependency order:

```java
// ── 1. No-dependency foundation ────────────────────────────
HRModule hrModule = new HRModule();

// ── 2. MDB — before stores, so stores can reference it ─────
StockAlertMDB stockAlertMDB = new StockAlertMDB();

// ── 3. Inventory composite ──────────────────────────────────
CentralInventoryBean centralInventory = new CentralInventoryBean();

// ── 4. Supplier — needs inventory ──────────────────────────
SupplierModule supplierModule = new SupplierModule();
supplierModule.setCentralInventory(centralInventory);  // inject
supplierModule.setStockAlerts(centralInventory);       // inject

// ── 5. POS + Customer — mutual back-wire ────────────────────
CustomerModule customerModule = new CustomerModule();
POSModule posModule = new POSModule();
posModule.setCustomerData(customerModule);             // inject
posModule.setLoyaltyService(customerModule);           // inject
customerModule.setSalesData(posModule);                // inject (back-wire)

// ── 6. Finance — needs POS, HR, Supplier ────────────────────
FinanceModule financeModule = new FinanceModule();
financeModule.setSalesData(posModule);
financeModule.setStaffData(hrModule);
financeModule.setOrderStatus(supplierModule);

// ── 7. Reporting — needs everything ─────────────────────────
ReportingModule reportingModule = new ReportingModule();
reportingModule.setSalesData(posModule);
reportingModule.setStaffData(hrModule);
reportingModule.setTotalStock(centralInventory);
reportingModule.setFinanceData(financeModule);
reportingModule.setCustomerData(customerModule);
```

Note the back-wire between `POSModule` and `CustomerModule`. This is a circular dependency resolved by setter injection — neither module knows about the other at instantiation time.

### 7.3 Dependency Graph

```
HRModule ◄────────────────────────────── FinanceModule
                                               ▲
POSModule ◄────────────────────────────────────┤
    ▲  │ provides ISalesData                   │
    │  └──► CustomerModule                     │
    │             │ provides ICustomerData      │
    │             └──────────────────────────► ReportingModule
    │                                               ▲
CentralInventoryBean ──────────────────────────────┤
    │  provides IStockAlerts, ITotalStock           │
    │                                               │
    └──► StoreInventoryBean ──► StockAlertMDB       │
                                     │              │
SupplierModule ◄─────────────────────┘              │
    │  provides ISupplierService, IOrderStatus       │
    └──────────────────────────────────────────────►┘
```

---

## 8. Component Interface Catalogue

All interfaces live in `com.groceryerp.interfaces`. Each interface is either **provided** (the implementing module offers it) or **required** (the using module needs it injected).

| Interface | Provided By | Required By | Methods |
|---|---|---|---|
| `IStoreInventory` | `StoreInventoryBean` | `POSModule`, `SupplierModule` | `checkStock`, `updateStock`, `getLowStockAlerts`, `getStoreId` |
| `IStockAlerts` | `CentralInventoryBean` | `SupplierModule` | `isRestockNeeded`, `getProductsNeedingRestock`, `resolveRestockAlert` |
| `ITotalStock` | `CentralInventoryBean` | `ReportingModule` | `getTotalStock`, `getStoresWithLowStock`, `redistributeStock` |
| `ISupplierService` | `SupplierModule` | *(was used by MDB — now removed)* | `placeOrder`, `getAllSupplierIds` |
| `IOrderStatus` | `SupplierModule` | `FinanceModule` | `getOrderStatus`, `getOrdersByStore` |
| `ISalesData` | `POSModule` | `FinanceModule`, `ReportingModule` | `getTotalRevenueBySale`, `getTransactionCount`, `getTotalSpendByCustomer` |
| `IReceiptService` | `POSModule` | *(API layer)* | `generateReceipt` |
| `ICustomerData` | `CustomerModule` | `POSModule`, `ReportingModule` | `getCustomerName`, `getCustomerCount` |
| `ILoyaltyService` | `CustomerModule` | `POSModule` | `addLoyaltyPoints`, `getLoyaltyTier` |
| `IStaffData` | `HRModule` | `FinanceModule`, `ReportingModule` | `getStaffIdsByStore`, `getTotalPayrollCost`, `getStaffCount` |
| `IPayrollService` | `HRModule` | *(API layer)* | `calculatePayroll` |
| `IFinanceData` | `FinanceModule` | `ReportingModule` | `getRevenueSummary`, `getExpenseSummary` |
| `IReportData` | `ReportingModule` | *(API layer)* | `generateReport` |
| `IProfitReport` | `FinanceModule` | *(API layer)* | `calculateProfit` |

---

## 9. Module-by-Module Reference

### 9.1 InventoryModule

**Classes:** `CentralInventoryBean`, `StoreInventoryBean`, `StockAlertMDB`

**Provided:** `ITotalStock`, `IStockAlerts`, `IStoreInventory`

**Required:** none (foundation layer)

**Key behaviour:**
- `StoreInventoryBean.updateStock()` performs an SQL upsert and, when the delta is negative and the resulting quantity falls below `lowStockThreshold` (default 10), constructs a `StockAlertBean` and passes it to `StockAlertMDB.onMessage("LOW_STOCK", alert)`
- `CentralInventoryBean` aggregates all stores and is the single object injected into all consumers — consumers never hold a reference to individual `StoreInventoryBean` instances
- Stores are loaded from the database at startup inside `ApiServer` and injected via `centralInventory.addStore(store)`

### 9.2 SupplierModule

**Class:** `SupplierModule`

**Provided:** `ISupplierService`, `IOrderStatus`

**Required:** `CentralInventoryBean` (for delivery stock updates), `IStockAlerts`

**Order status lifecycle:**

```
ACCEPTED → OUT_FOR_DELIVERY → DELIVERED
```

Manager-placed orders (`placeOrder()`) start at `ACCEPTED` immediately. Alert-based orders start at `QUOTED` (submitted by the supplier portal) and require manager acceptance before moving to `ACCEPTED`.

### 9.3 HRModule

**Class:** `HRModule`

**Provided:** `IStaffData`, `IPayrollService`

**Required:** none

**Session lifecycle:** `beginPayrollSession()` → `loadShifts()` → `computeGrossPay()` → `finalizePayroll()` → `endSession()`

Deductions are a flat 15% applied in `finalizePayroll()`.

### 9.4 POSModule

**Class:** `POSModule`

**Provided:** `ISalesData`, `IReceiptService`

**Required:** `ICustomerData`, `ILoyaltyService`, `CentralInventoryBean`

**Sale processing sequence:**
1. Validate stock for all items (fail-fast before any deduction)
2. Deduct stock for all items
3. Persist `SaleBean` and `SaleItemBean` records
4. Process payment → persist `PaymentBean` + `ReceiptBean`
5. Persist `RevenueBean` for finance
6. Persist `PurchaseHistoryBean` + award loyalty points (skipped for GUEST)

Tax is 14% applied on subtotal.

### 9.5 FinanceModule

**Class:** `FinanceModule`

**Provided:** `IFinanceData`, `IProfitReport`

**Required:** `ISalesData`, `IStaffData`, `IOrderStatus`

Aggregates revenue from POS, payroll costs from HR, and purchase costs from Supplier to produce profit summaries.

### 9.6 CustomerModule

**Class:** `CustomerModule`

**Provided:** `ICustomerData`, `ILoyaltyService`

**Required:** `ISalesData` (back-wired from POSModule)

Loyalty tiers: BRONZE (0–499 points), SILVER (500–999), GOLD (1000+). Points are awarded as 1 point per EGP spent (floored).

### 9.7 ReportingModule

**Class:** `ReportingModule`

**Provided:** `IReportData`

**Required:** `ISalesData`, `IStaffData`, `ITotalStock`, `IFinanceData`, `ICustomerData`

ReportingModule is the most-connected module — it requires all five data interfaces. It delegates to `DataCollector`, `ReportGenerator`, and `ExportService` internally.

---

## 10. The Supplier Flow End-to-End

This is the most complex flow in the system, involving four components across two frontends.

### 10.1 Alert-Based Flow (Supplier-Initiated)

```
1. POS processes a sale
      │
      ▼
2. POSModule calls store.updateStock(productId, -qty)
      │
      ▼
3. StoreInventoryBean detects qty < threshold
   Creates StockAlertBean, calls stockAlertMDB.onMessage("LOW_STOCK", alert)
      │
      ▼
4. StockAlertMDB.onMessage()
   → alertDao.save(alert)          — persists to stock_alerts table
   → AlertBroadcaster.broadcast()  — pushes SSE event to connected suppliers
      │
      ▼
5. Supplier portal receives SSE event, calls load() to refresh alerts
   Supplier sees the alert in their "Low Stock Alerts" tab
      │
      ▼
6. Supplier submits a quote (quantity + unit price)
   POST /api/supplier/quote
   → Creates purchase_order with status='QUOTED', productAlertId=alertId
      │
      ▼
7. Manager sees "Quote Received" badge in Alerts tab or Orders tab
   Clicks Accept → POST /api/supplier/accept
   → status: QUOTED → ACCEPTED
      │
      ▼
8. Supplier sees order status change to "Accepted" in portal
   Clicks "Mark Out for Delivery" → POST /api/supplier/outfordelivery
   → status: ACCEPTED → OUT_FOR_DELIVERY
      │
      ▼
9. Manager sees "Out for Delivery" badge
   Clicks "Mark Delivered" → POST /api/supplier/delivery (per order line)
   → SupplierModule.recordDelivery() updates stock + records expense
   → status: OUT_FOR_DELIVERY → DELIVERED
```

### 10.2 Manager-Initiated Flow

Manager places an order directly via "Place Order" modal → `POST /api/supplier/order` → `SupplierModule.placeOrder()` → order created with status `ACCEPTED`. Supplier sees it immediately in their "My Orders" tab and can proceed from step 8 above.

### 10.3 Alert Filtering

The `GET /api/supplier/stockalerts?supplierId=X` endpoint uses an `INNER JOIN supplier_products` to return only alerts for products the supplier carries:

```sql
SELECT sa.*, po.orderId, po.status as orderStatus, po.supplierId as assignedSupplierId
FROM stock_alerts sa
INNER JOIN supplier_products sp ON sp.productId = sa.productId AND sp.supplierId = ?
LEFT JOIN purchase_orders po ON po.productAlertId = sa.alertId
WHERE po.orderId IS NULL OR po.supplierId = ?
```

The `WHERE` clause ensures a supplier only sees alerts that are either unquoted (open to all suppliers carrying that product) or already linked to their own order.

---

## 11. Real-Time Alerts — SSE

### 11.1 Architecture

```
StockAlertMDB.onMessage()
    └── AlertBroadcaster.broadcast(productId)
              └── suppliersForProduct(productId)   — DB query: supplier_products
                        └── for each matching supplierId:
                                  └── push "event: alert\ndata: productId\n\n"
                                        to all open PrintWriter connections
```

### 11.2 `AlertBroadcaster`

Singleton holding a `ConcurrentHashMap<String, List<PrintWriter>>` keyed by `supplierId`. Thread-safe because `CopyOnWriteArrayList` is used for each supplier's writer list and `removeIf` is used to clean up dead connections.

### 11.3 SSE Endpoint

`GET /api/supplier/events?supplierId=X`

The servlet holds the HTTP connection open, sends a heartbeat comment every 20 seconds to prevent proxy timeouts, and unregisters the writer when the connection closes:

```java
AlertBroadcaster.getInstance().subscribe(supplierId, writer);
try {
    while (!writer.checkError()) {
        Thread.sleep(20_000);
        writer.write(": keep-alive\n\n");
        writer.flush();
    }
} finally {
    AlertBroadcaster.getInstance().unsubscribe(supplierId, writer);
}
```

### 11.4 Frontend Subscription

```javascript
// supplier-portal/src/api.js
export function subscribeAlerts(supplierId, onAlert) {
    const es = new EventSource(`/api/supplier/events?supplierId=${supplierId}`);
    es.addEventListener('alert', onAlert);
    return () => es.close();   // cleanup function
}

// App.jsx — subscribe on login, unsubscribe on logout
useEffect(() => {
    if (!supplier) return;
    const unsubscribe = subscribeAlerts(supplier.supplierId, () => load(supplier));
    return unsubscribe;
}, [supplier]);
```

---

## 12. Database Schema

All tables are created by `DatabaseManager.initializeDatabase()` using `CREATE TABLE IF NOT EXISTS`, making startup idempotent.

| Table | Primary Key | Notes |
|---|---|---|
| `stores` | `storeId` | |
| `products` | `productId` | |
| `stock` | `(storeId, productId)` | Composite PK; upsert via `ON CONFLICT` |
| `stock_alerts` | `alertId` | Fired when qty < threshold |
| `suppliers` | `supplierId` | |
| `supplier_products` | `(supplierId, productId)` | Links suppliers to their product catalogue |
| `purchase_orders` | `orderId` | `productAlertId` links to `stock_alerts.alertId` |
| `order_lines` | `lineId` | FK → `purchase_orders` |
| `deliveries` | `deliveryId` | FK → `purchase_orders` |
| `sales` | `saleId` | |
| `sale_items` | `itemId` | FK → `sales` |
| `receipts` | `receiptId` | |
| `payments` | `paymentId` | |
| `discounts` | `discountId` | |
| `employees` | `employeeId` | |
| `shifts` | `shiftId` | |
| `payroll` | `payrollId` | |
| `attendance` | `attendanceId` | |
| `expenses` | `expenseId` | |
| `revenue` | `revenueId` | |
| `tax_records` | `taxId` | |
| `customers` | `customerId` | |
| `loyalty` | `customerId` | |
| `purchase_history` | `historyId` | |

### Safe Migration Pattern

When a column is added after the initial schema is deployed, `ALTER TABLE ... ADD COLUMN` is run at startup inside a try-catch that ignores `SQLException` (thrown when the column already exists):

```java
try { stmt.execute("ALTER TABLE purchase_orders ADD COLUMN productAlertId TEXT"); }
catch (SQLException ignored) { /* column already exists */ }
```

---

## 13. REST API Reference

The backend runs on port `8080`. All paths are under `/api`.

### Inventory — `/api/inventory/*`

| Method | Path | Description |
|---|---|---|
| GET | `/stock` | All stock across all stores |
| GET | `/stock?storeId=` | Stock for one store |
| GET | `/available?storeId=` | Products with qty > 0 for a store |
| GET | `/stores` | All stores |
| GET | `/lowstock` | Products below threshold |
| GET | `/alerts` | All stock alert records |
| POST | `/store` | Add a new store |
| POST | `/restock` | Manually restock a product |

### Supplier — `/api/supplier/*`

| Method | Path | Description |
|---|---|---|
| GET | `/suppliers` | All suppliers |
| GET | `/orders` | All purchase orders |
| GET | `/orders?storeId=` | Orders for one store |
| GET | `/orders?supplierId=` | Orders for one supplier |
| GET | `/orderlines?orderId=` | Line items for one order |
| GET | `/status?orderId=` | Status of one order |
| GET | `/stockalerts` | All low-stock alerts (manager view) |
| GET | `/stockalerts?supplierId=` | Alerts for supplier's products only |
| GET | `/events?supplierId=` | SSE stream for real-time alerts |
| GET | `/products?supplierId=` | Products a supplier carries |
| POST | `/order` | Manager places an order directly |
| POST | `/quote` | Supplier submits a quote for an alert |
| POST | `/accept` | Manager accepts a QUOTED order |
| POST | `/outfordelivery` | Supplier marks ACCEPTED → OUT_FOR_DELIVERY |
| POST | `/delivery` | Manager marks delivered (updates stock) |
| POST | `/add` | Add a new supplier |
| POST | `/products` | Assign a product to a supplier |
| POST | `/removeproduct` | Remove a product from a supplier |

### POS — `/api/pos/*`

| Method | Path | Description |
|---|---|---|
| GET | `/sales` | All sales |
| GET | `/sales?storeId=` | Sales for one store |
| GET | `/receipt?saleId=` | Text receipt for a sale |
| POST | `/sale` | Process a new sale |
| POST | `/discount` | Apply a discount to a sale |
| POST | `/payment` | Process payment |

### HR — `/api/hr/*`

| Method | Path | Description |
|---|---|---|
| GET | `/employees` | All employees |
| GET | `/employees?storeId=` | Employees for one store |
| GET | `/shifts` | All shifts |
| GET | `/payroll` | All payroll records |
| GET | `/attendance` | All attendance records |
| POST | `/employee` | Add an employee |
| POST | `/shift` | Add a shift |
| POST | `/runpayroll` | Run payroll for an employee and period |
| POST | `/attendance` | Record attendance |

### Finance — `/api/finance/*`

| Method | Path | Description |
|---|---|---|
| GET | `/summary?period=` | Finance summary for a period |
| GET | `/revenue` | All revenue records |
| GET | `/expenses` | All expense records |
| POST | `/expense` | Add an expense |
| POST | `/revenue` | Add a revenue record |

### Customer — `/api/customer/*`

| Method | Path | Description |
|---|---|---|
| GET | `/customers` | All customers |
| GET | `/loyalty` | All loyalty records |
| GET | `/points?customerId=` | Points for one customer |
| POST | `/add` | Add a customer |
| POST | `/points` | Add loyalty points |

### Reporting — `/api/reporting/*`

| Method | Path | Description |
|---|---|---|
| GET | `/report?type=&period=&storeId=` | Generate a report |

### Admin

| Method | Path | Description |
|---|---|---|
| POST | `/reset` | Delete all data (schema preserved) |

---

## 14. Frontend Architecture

### 14.1 Manager App (port 5173)

Located in `frontend/`. A single-page React application with one route per module:

| Route | Page | Module |
|---|---|---|
| `/` | Dashboard | Aggregated KPIs |
| `/inventory` | Inventory | Stock, alerts, restock |
| `/pos` | POS | Process sales |
| `/supplier` | Supplier | Alerts, orders, suppliers |
| `/hr` | HR | Employees, shifts, payroll |
| `/finance` | Finance | Revenue, expenses, profit |
| `/customers` | Customers | Customer list, loyalty |
| `/reporting` | Reporting | Report generation |

Shared utilities:
- `frontend/src/api/client.js` — all API calls in one place
- `frontend/src/utils/fmt.js` — money formatter (`EGP 1,234.56`)
- `frontend/src/hooks/useStores.js` — React hook for store list

### 14.2 Supplier Portal (port 5174)

Located in `supplier-portal/`. A standalone Vite app with no routing — single-page with tab navigation.

**Login:** Supplier selects their account from a dropdown populated by `GET /api/supplier/suppliers`.

**Tabs:**
- **Low Stock Alerts** — products the supplier carries that are below threshold, with a quote submission form
- **My Orders** — orders assigned to this supplier, with "Mark Out for Delivery" action

**SSE:** On login, `subscribeAlerts(supplierId, callback)` opens an `EventSource` connection to `/api/supplier/events?supplierId=X`. When an `alert` event arrives, the page refreshes its alert list automatically.

Both apps proxy `/api` to `http://localhost:8080` via Vite's dev server proxy config.

---

## 15. Running the System

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+

### Start the backend

```bash
mvn exec:java -Dexec.mainClass=com.groceryerp.integration.Main
```

The database file is created automatically at `db/grocery_erp.db` on first run.

### Start the manager frontend

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

### Start the supplier portal

```bash
cd supplier-portal
npm install
npm run dev        # http://localhost:5174
```

### First-time setup

1. Open the manager app and go to **Inventory → Add Store** to create at least one store
2. Add products via **Inventory → Add Product**
3. Go to **Supplier → Add Supplier**
4. Expand the supplier row and assign products to them
5. Open the supplier portal at port 5174 and log in as that supplier
6. Process a sale in POS to drop stock below 10 — the alert will appear in the supplier portal automatically
