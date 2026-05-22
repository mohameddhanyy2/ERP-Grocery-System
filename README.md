# Grocery Store ERP System

**JavaBeans · EJB · Component-Based Architecture**
Academic project — built with plain Java, no external frameworks.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Modules](#modules)
- [Key Concepts](#key-concepts)
- [Interface Map](#interface-map)
- [How to Run](#how-to-run)
- [How to Import in Eclipse](#how-to-import-in-eclipse)
- [GitHub Workflow](#github-workflow)
- [Team Responsibilities](#team-responsibilities)

---

## Overview

This is a multi-store grocery chain ERP system built using Component-Based Development (CBD). It manages inventory across multiple store branches, point of sale transactions, supplier purchasing, human resources, finance, customer loyalty, and reporting — all wired together using Inversion of Control (IoC) without any external framework.

The system supports multiple physical store branches under a single brand. Each branch has its own `StoreInventoryBean`, and a `CentralInventoryBean` composite aggregates all branches into a single chain-wide view.

**No Spring Boot. No Hibernate. No external libraries. Plain Java only.**

---

## Architecture

The system is built as a set of self-contained components. Each component:

- Declares what it **provides** — services it offers to other components
- Declares what it **requires** — services it needs from other components
- Never instantiates another component directly — all dependencies are injected from outside (IoC)

```
                        ┌─────────────────────────────────────┐
                        │       CentralInventoryBean          │
                        │            (composite)              │
                        │  ┌─────────┐ ┌─────────┐ ┌───────┐ │
                        │  │ Store A │ │ Store B │ │Store C│ │
                        │  └─────────┘ └─────────┘ └───────┘ │
                        └──────────────┬──────────────────────┘
                                       │ IStoreInventory
              ┌────────────────────────┼────────────────────┐
              ▼                        ▼                     ▼
        ┌───────────┐          ┌──────────────┐     ┌──────────────┐
        │ POSModule │          │SupplierModule│     │   HRModule   │
        │ ISalesData│          │ISupplierSvc  │     │  IStaffData  │
        │ IReceiptSvc│         │IOrderStatus  │     │IPayrollService│
        └─────┬─────┘          └──────┬───────┘     └──────┬───────┘
              │ ISalesData            │ IOrderStatus        │ IStaffData
              ▼                       ▼                     ▼
        ┌─────────────────────────────────────────────────────────┐
        │                    FinanceModule                        │
        │               IFinanceData · IProfitReport              │
        └──────────────────────────┬──────────────────────────────┘
                                   │ IFinanceData
        ┌──────────────────────────▼──────────────────────────────┐
        │                   ReportingModule                       │
        │    (consumer-only — reads from all, writes to none)     │
        └─────────────────────────────────────────────────────────┘
```

---

## Project Structure

```
grocery-erp/
├── src/
│   └── main/
│       ├── java/com/groceryerp/
│       │   ├── interfaces/                  ← 14 shared interface contracts
│       │   │   ├── ISalesData.java
│       │   │   ├── IReceiptService.java
│       │   │   ├── IStoreInventory.java
│       │   │   ├── ITotalStock.java
│       │   │   ├── IStockAlerts.java
│       │   │   ├── IStaffData.java
│       │   │   ├── IPayrollService.java
│       │   │   ├── ISupplierService.java
│       │   │   ├── IOrderStatus.java
│       │   │   ├── IFinanceData.java
│       │   │   ├── IProfitReport.java
│       │   │   ├── ICustomerData.java
│       │   │   ├── ILoyaltyService.java
│       │   │   └── IReportData.java
│       │   │
│       │   ├── pos/                         ← POS Module
│       │   │   ├── POSModule.java
│       │   │   └── beans/
│       │   │       ├── SaleBean.java
│       │   │       ├── SaleItemBean.java
│       │   │       ├── ReceiptBean.java
│       │   │       ├── PaymentBean.java
│       │   │       └── DiscountBean.java
│       │   │
│       │   ├── inventory/                   ← Inventory Module (composite)
│       │   │   ├── StoreInventoryBean.java  ← leaf component
│       │   │   ├── CentralInventoryBean.java← composite component
│       │   │   └── beans/
│       │   │       ├── ProductBean.java
│       │   │       └── StockAlertBean.java
│       │   │
│       │   ├── supplier/                    ← Supplier Module
│       │   │   ├── SupplierModule.java
│       │   │   └── beans/
│       │   │       ├── SupplierBean.java
│       │   │       ├── PurchaseOrderBean.java
│       │   │       ├── OrderLineBean.java
│       │   │       └── DeliveryBean.java
│       │   │
│       │   ├── hr/                          ← HR Module
│       │   │   ├── HRModule.java
│       │   │   └── beans/
│       │   │       ├── EmployeeBean.java
│       │   │       ├── ShiftBean.java
│       │   │       ├── PayrollBean.java
│       │   │       └── AttendanceBean.java
│       │   │
│       │   ├── finance/                     ← Finance Module
│       │   │   ├── FinanceModule.java
│       │   │   └── beans/
│       │   │       ├── ExpenseBean.java
│       │   │       ├── RevenueBean.java
│       │   │       ├── TaxBean.java
│       │   │       └── ProfitSummaryBean.java
│       │   │
│       │   ├── customer/                    ← Customer Module
│       │   │   ├── CustomerModule.java
│       │   │   └── beans/
│       │   │       ├── CustomerBean.java
│       │   │       ├── LoyaltyBean.java
│       │   │       └── PurchaseHistoryBean.java
│       │   │
│       │   ├── reporting/                   ← Reporting Module
│       │   │   ├── ReportingModule.java
│       │   │   ├── beans/
│       │   │   │   ├── ReportBean.java
│       │   │   │   ├── SalesSummaryBean.java
│       │   │   │   └── InventoryReportBean.java
│       │   │   └── services/
│       │   │       ├── DataCollector.java
│       │   │       ├── ReportGenerator.java
│       │   │       └── ExportService.java
│       │   │
│       │   └── integration/
│       │       └── Main.java                ← IoC wiring + demo
│       │
│       └── resources/META-INF/
│           ├── ejb-jar.xml                  ← EJB deployment descriptor
│           └── application.xml             ← EAR assembly descriptor
│
└── docs/
    └── uml/                                 ← UML diagrams (all members)
```

---

## Modules

### 1. POS Module
Handles checkout: scanning items, calculating totals, applying discounts, and issuing receipts.

| | Interfaces |
|---|---|
| **Provides** | `ISalesData`, `IReceiptService` |
| **Requires** | `IStoreInventory`, `ICustomerData` |

### 2. Inventory Module (Composite)
Manages stock across all store branches. Built as a composite structure: `CentralInventoryBean` holds a list of `StoreInventoryBean` instances and exposes chain-wide operations.

| | Interfaces |
|---|---|
| **Provides** | `IStoreInventory`, `ITotalStock`, `IStockAlerts` |
| **Requires** | none — foundation component |

### 3. Supplier Module
Manages vendors, creates purchase orders when stock is low, and records deliveries.

| | Interfaces |
|---|---|
| **Provides** | `ISupplierService`, `IOrderStatus` |
| **Requires** | `IStoreInventory`, `IStockAlerts` |

### 4. HR Module
Manages employee records, shift scheduling, attendance, and payroll calculation.

| | Interfaces |
|---|---|
| **Provides** | `IStaffData`, `IPayrollService` |
| **Requires** | none — foundation component |

### 5. Finance Module
Tracks revenue, expenses, payroll costs, purchase costs, and calculates net profit. The strongest IoC demonstration in the project — depends on three required interfaces.

| | Interfaces |
|---|---|
| **Provides** | `IFinanceData`, `IProfitReport` |
| **Requires** | `ISalesData`, `IStaffData`, `IOrderStatus` |

### 6. Customer Module
Manages customer profiles, purchase history, and loyalty points.

| | Interfaces |
|---|---|
| **Provides** | `ICustomerData`, `ILoyaltyService` |
| **Requires** | `ISalesData` |

### 7. Reporting Module
Consumer-only component. Reads from all other modules and generates sales, inventory, and finance reports. Provides CSV export. Never writes to any other module.

| | Interfaces |
|---|---|
| **Provides** | `IReportData` |
| **Requires** | `ISalesData`, `IStaffData`, `ITotalStock`, `IFinanceData`, `ICustomerData` |

---

## Key Concepts

### Component-Based Development (CBD)
Every module is an independent component with a clearly defined boundary. Components communicate only through declared interfaces — never by calling each other's internal classes directly.

### Provided and Required Interfaces
- **Provided interface** — what a component offers. Declared with `implements` on the module class.
- **Required interface** — what a component needs from outside. Declared as a private field and populated via a setter method.

### Inversion of Control (IoC)
No module ever creates another module with `new`. All dependencies flow inward from `Main.java`, which is the only place in the system that knows about concrete classes. Every module only knows the interface it was given.

```java
// FinanceModule does NOT do this:
ISalesData sales = new POSModule(); // ✗ wrong

// FinanceModule declares a field and waits:
private ISalesData salesData;       // ✓ correct
public void setSalesData(ISalesData s) { this.salesData = s; }

// Main.java does the wiring:
financeModule.setSalesData(posModule); // ✓ IoC
```

### Composite Structures
`CentralInventoryBean` implements the composite pattern. It holds a `List<IStoreInventory>` and treats the whole collection through the same interface that individual stores implement. Neither POS nor Supplier knows whether they are talking to one store or the central composite.

### JavaBeans
All data transfer objects follow the JavaBeans specification:
- Class is `public`
- Has a `public` no-argument constructor
- All fields are `private`
- Every field has a `public getX()` and `public setX()` method
- Implements `java.io.Serializable`

### Enterprise JavaBeans (EJB)
Module lifecycle is declared in `ejb-jar.xml`. Session types:
- `@Stateless` — POS, Supplier, Finance, Customer, Reporting (no state held between calls)
- `@Stateful` — HR (payroll spans multiple steps), CentralInventory (store list maintained)

### Packaging Units
Each module is packaged as an independent `.jar` file. All jars are assembled into a single `grocery-erp.ear` enterprise archive declared by `application.xml`.

### Deployment Descriptors
`ejb-jar.xml` declares every component: its name, class, session type, and local/remote interfaces — without modifying the Java source code.

---

## Interface Map

```
                    PROVIDES                     REQUIRED BY
─────────────────────────────────────────────────────────────────
ISalesData          POSModule              →     FinanceModule
                                           →     CustomerModule
                                           →     ReportingModule

IReceiptService     POSModule              →     (external caller)

IStoreInventory     CentralInventoryBean   →     POSModule
                                           →     SupplierModule

ITotalStock         CentralInventoryBean   →     ReportingModule

IStockAlerts        CentralInventoryBean   →     SupplierModule

IStaffData          HRModule               →     FinanceModule
                                           →     ReportingModule

IPayrollService     HRModule               →     (external caller)

ISupplierService    SupplierModule         →     (external caller)

IOrderStatus        SupplierModule         →     FinanceModule
                                           →     ReportingModule

IFinanceData        FinanceModule          →     ReportingModule

IProfitReport       FinanceModule          →     (external caller)

ICustomerData       CustomerModule         →     POSModule
                                           →     ReportingModule

ILoyaltyService     CustomerModule         →     (external caller)

IReportData         ReportingModule        →     (end consumer)
─────────────────────────────────────────────────────────────────
```

---

## How to Run

**Compile all files:**
```bash
javac -d out $(find src -name "*.java")
```

**Run the demo:**
```bash
java -cp out com.groceryerp.integration.Main
```

**Expected output:**
```
=== Grocery ERP System — Startup ===

[IoC] HRModule instantiated (no required interfaces)
[IoC] CentralInventoryBean assembled with 3 stores
[IoC] POSModule wired with IStoreInventory
[IoC] SupplierModule wired with IStoreInventory + IStockAlerts
[IoC] FinanceModule wired with ISalesData + IStaffData + IOrderStatus
[IoC] CustomerModule wired with ISalesData
[IoC] ReportingModule wired with all 5 required interfaces

=== Demo Scenario ===

Total stock for PROD_001 across all stores: 100
Stores with low stock: [STORE_A]
Sale processed at STORE_A for PROD_001 x2
Report generated: SALES | 0.0 | 2025-...

=== ERP System Running ===
```

---

## How to Import in Eclipse

1. **File → Import → General → Existing Projects into Workspace**
2. Select **Select archive file** → browse to `grocery-erp.zip` → Finish
3. Right-click the project → **Properties → Java Build Path → Source**
4. Click **Add Folder** → check `src/main/java` → OK
5. Right-click `Main.java` → **Run As → Java Application**

If you see red errors: right-click project → **Properties → Java Compiler** → set compliance level to **11** or higher.

---

## GitHub Workflow

**Before starting work — always pull first:**
```bash
git pull origin main
```

**Create your branch:**
```bash
git checkout -b member3-supplier
```

**Commit and push your work:**
```bash
git add .
git commit -m "Member 3: add SupplierBean fields and getters"
git push origin member3-supplier
```

Then open a **Pull Request** on GitHub → another member reviews → merge into main.

**Never push directly to `main`.**

---

## Team Responsibilities

| Member | Module(s) | Branch |
|---|---|---|
| Member 1 | POS Module + all shared interfaces | `member1-pos-interfaces` |
| Member 2 | Inventory Module (composite) | `member2-inventory` |
| Member 3 | Supplier Module | `member3-supplier` |
| Member 4 | HR Module | `member4-hr` |
| Member 5 | Finance Module + Customer Module | `member5-finance-customer` |
| Member 6 | Reporting Module + Main.java integration + system UML | `member6-reporting-integration` |

**Shared tasks (every member):**
- JavaDoc all beans and interfaces you write
- Draw the UML component diagram for your module and save it in `docs/uml/`
- Write a standalone `main()` demo proving your module's provided interface works in isolation

---

## Notes

- Do not add Spring, Spring Boot, Hibernate, Maven, or any external dependency to this project. Plain Java only.
- The `// TODO` comments in stub files mark exactly what each member needs to implement.
- The `interfaces/` package must not be modified after Member 1 delivers it — all other members depend on these contracts. Any change must be discussed with the full team first.
