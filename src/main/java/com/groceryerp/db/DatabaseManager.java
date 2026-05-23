package com.groceryerp.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class for managing SQLite database connections and schema initialization.
 * Opens a fresh connection per call — callers must close it with try-with-resources.
 */
public class DatabaseManager {

    private static final String URL = "jdbc:sqlite:db/grocery_erp.db";

    /** Returns a fresh JDBC connection. Caller must close it. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /**
     * Creates all tables using CREATE TABLE IF NOT EXISTS.
     * Call once at application startup before any DAO operations.
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // ── POS ────────────────────────────────────────────────
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS sales (" +
                "  saleId TEXT PRIMARY KEY," +
                "  storeId TEXT NOT NULL," +
                "  customerId TEXT," +
                "  totalAmount REAL NOT NULL," +
                "  paymentMethod TEXT," +
                "  timestamp TEXT NOT NULL," +
                "  discountRate REAL DEFAULT 0.0" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS sale_items (" +
                "  itemId TEXT PRIMARY KEY," +
                "  saleId TEXT NOT NULL," +
                "  productId TEXT NOT NULL," +
                "  quantity INTEGER NOT NULL," +
                "  unitPrice REAL NOT NULL," +
                "  lineTotal REAL NOT NULL," +
                "  FOREIGN KEY (saleId) REFERENCES sales(saleId)" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS receipts (" +
                "  receiptId TEXT PRIMARY KEY," +
                "  saleId TEXT NOT NULL," +
                "  storeId TEXT NOT NULL," +
                "  totalAmount REAL NOT NULL," +
                "  taxAmount REAL NOT NULL," +
                "  grandTotal REAL NOT NULL," +
                "  issuedAt TEXT NOT NULL" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS payments (" +
                "  paymentId TEXT PRIMARY KEY," +
                "  saleId TEXT NOT NULL," +
                "  method TEXT NOT NULL," +
                "  amountPaid REAL NOT NULL," +
                "  change REAL NOT NULL," +
                "  processedAt TEXT NOT NULL" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS discounts (" +
                "  discountId TEXT PRIMARY KEY," +
                "  saleId TEXT NOT NULL," +
                "  discountType TEXT NOT NULL," +
                "  discountValue REAL NOT NULL," +
                "  description TEXT" +
                ")"
            );

            // ── Inventory ──────────────────────────────────────────
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS products (" +
                "  productId TEXT PRIMARY KEY," +
                "  name TEXT NOT NULL," +
                "  category TEXT," +
                "  price REAL NOT NULL," +
                "  expiryDate TEXT" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS stock (" +
                "  storeId TEXT NOT NULL," +
                "  productId TEXT NOT NULL," +
                "  quantity INTEGER NOT NULL DEFAULT 0," +
                "  PRIMARY KEY (storeId, productId)" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS stock_alerts (" +
                "  alertId TEXT PRIMARY KEY," +
                "  productId TEXT NOT NULL," +
                "  storeId TEXT NOT NULL," +
                "  currentQty INTEGER NOT NULL," +
                "  threshold INTEGER NOT NULL," +
                "  alertDate TEXT NOT NULL" +
                ")"
            );

            // ── Supplier ───────────────────────────────────────────
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS suppliers (" +
                "  supplierId TEXT PRIMARY KEY," +
                "  name TEXT NOT NULL," +
                "  contactEmail TEXT," +
                "  leadTimeDays INTEGER DEFAULT 3" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS purchase_orders (" +
                "  orderId TEXT PRIMARY KEY," +
                "  supplierId TEXT NOT NULL," +
                "  storeId TEXT NOT NULL," +
                "  orderDate TEXT NOT NULL," +
                "  totalCost REAL DEFAULT 0.0," +
                "  status TEXT NOT NULL DEFAULT 'PENDING'" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS order_lines (" +
                "  lineId TEXT PRIMARY KEY," +
                "  orderId TEXT NOT NULL," +
                "  productId TEXT NOT NULL," +
                "  quantity INTEGER NOT NULL," +
                "  unitPrice REAL NOT NULL," +
                "  FOREIGN KEY (orderId) REFERENCES purchase_orders(orderId)" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS deliveries (" +
                "  deliveryId TEXT PRIMARY KEY," +
                "  orderId TEXT NOT NULL," +
                "  deliveryDate TEXT," +
                "  deliveryStatus TEXT NOT NULL DEFAULT 'PENDING'" +
                ")"
            );

            // ── HR ─────────────────────────────────────────────────
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS employees (" +
                "  employeeId TEXT PRIMARY KEY," +
                "  storeId TEXT NOT NULL," +
                "  name TEXT NOT NULL," +
                "  role TEXT NOT NULL," +
                "  hourlyRate REAL NOT NULL," +
                "  startDate TEXT" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS shifts (" +
                "  shiftId TEXT PRIMARY KEY," +
                "  employeeId TEXT NOT NULL," +
                "  storeId TEXT NOT NULL," +
                "  shiftStart TEXT NOT NULL," +
                "  shiftEnd TEXT NOT NULL," +
                "  hoursWorked REAL NOT NULL" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS payroll (" +
                "  payrollId TEXT PRIMARY KEY," +
                "  employeeId TEXT NOT NULL," +
                "  period TEXT NOT NULL," +
                "  grossPay REAL NOT NULL," +
                "  deductions REAL NOT NULL," +
                "  netPay REAL NOT NULL" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS attendance (" +
                "  attendanceId TEXT PRIMARY KEY," +
                "  employeeId TEXT NOT NULL," +
                "  storeId TEXT NOT NULL," +
                "  date TEXT NOT NULL," +
                "  present INTEGER NOT NULL DEFAULT 1" +
                ")"
            );

            // ── Finance ────────────────────────────────────────────
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS expenses (" +
                "  expenseId TEXT PRIMARY KEY," +
                "  storeId TEXT NOT NULL," +
                "  category TEXT," +
                "  amount REAL NOT NULL," +
                "  date TEXT NOT NULL" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS revenue (" +
                "  revenueId TEXT PRIMARY KEY," +
                "  period TEXT NOT NULL," +
                "  storeId TEXT NOT NULL," +
                "  grossRevenue REAL NOT NULL" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS tax_records (" +
                "  taxId TEXT PRIMARY KEY," +
                "  period TEXT NOT NULL," +
                "  taxRate REAL NOT NULL," +
                "  taxableAmount REAL NOT NULL," +
                "  taxOwed REAL NOT NULL" +
                ")"
            );

            // ── Customer ───────────────────────────────────────────
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS customers (" +
                "  customerId TEXT PRIMARY KEY," +
                "  name TEXT NOT NULL," +
                "  email TEXT," +
                "  registeredStoreId TEXT" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS loyalty (" +
                "  customerId TEXT PRIMARY KEY," +
                "  points INTEGER NOT NULL DEFAULT 0," +
                "  tier TEXT NOT NULL DEFAULT 'BRONZE'" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS purchase_history (" +
                "  historyId TEXT PRIMARY KEY," +
                "  customerId TEXT NOT NULL," +
                "  saleId TEXT NOT NULL," +
                "  date TEXT NOT NULL," +
                "  amount REAL NOT NULL" +
                ")"
            );

            System.out.println("[DB] Database initialized successfully.");
        } catch (SQLException e) {
            System.out.println("[DB] Failed to initialize database: " + e.getMessage());
        }
    }
}
