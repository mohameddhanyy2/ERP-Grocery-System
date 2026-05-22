# Contributing

## Prerequisites

- JDK 21 ([Temurin](https://adoptium.net))
- VS Code with **Extension Pack for Java** (`vscjava.vscode-java-pack`)

## Running the project

**Compile:**
```bash
find src -name "*.java" | xargs javac -d out
```

**Run:**
```bash
cd out && java com.groceryerp.integration.Main
```

No Maven or external server required — pure Java.

## Project structure

```
src/main/java/com/groceryerp/
├── integration/       # Main.java — IoC wiring entry point
├── interfaces/        # All shared interfaces (ISalesData, IStoreInventory, etc.)
├── inventory/         # CentralInventoryBean, StoreInventoryBean, ProductBean
├── pos/               # POSModule + SaleBean, ReceiptBean, PaymentBean, DiscountBean
├── finance/           # FinanceModule + RevenueBean, ExpenseBean, TaxBean
├── hr/                # HRModule + EmployeeBean, PayrollBean, AttendanceBean, ShiftBean
├── customer/          # CustomerModule + CustomerBean, LoyaltyBean, PurchaseHistoryBean
├── supplier/          # SupplierModule + PurchaseOrderBean, DeliveryBean, SupplierBean
└── reporting/         # ReportingModule + ReportBean, SalesSummaryBean, ExportService
```

## IoC rules

- Modules declare required interfaces as fields — never instantiate dependencies inside a module
- Wiring happens exclusively in `Main.java` via setter injection
- Depend on interfaces, never on concrete classes

## Branching

- `main` — protected, production-ready code only
- `dev` — integration branch
- `feature/<module>-<short-desc>` — e.g. `feature/inventory-add-product`
- `fix/<short-desc>` — bug fixes

## Workflow

1. Pull latest `dev`
2. Create your feature branch from `dev`
3. Commit small, focused changes
4. Push and open a PR into `dev`
5. At least 1 reviewer approves before merge

## Commit messages

Follow conventional commits:
- `feat: add product CRUD endpoints`
- `fix: correct stock calculation on sale`
- `docs: update README`
- `refactor: extract auth filter`
- `test: add inventory module tests`

## Code style

- Follow JavaBean conventions — no-arg constructor, getters/setters
- Keep business logic inside the module, not in beans
- Beans are plain data holders — no logic, no dependencies
- New interfaces go in `interfaces/` and must be implemented before wiring in `Main.java`
