package com.groceryerp.integration;

import com.groceryerp.customer.beans.CustomerBean;
import com.groceryerp.customer.beans.LoyaltyBean;
import com.groceryerp.db.DatabaseManager;
import com.groceryerp.hr.beans.EmployeeBean;
import com.groceryerp.inventory.beans.ProductBean;
import com.groceryerp.supplier.beans.SupplierBean;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Seeds the database with initial reference data.
 * Safe to call on every startup — checks before inserting.
 */
public class DataSeeder {

    public static void seed() {
        if (alreadySeeded()) {
            System.out.println("[DataSeeder] Data already present — skipping seed.");
            return;
        }

        System.out.println("[DataSeeder] Seeding initial data...");
        seedProducts();
        seedStock();
        seedCustomers();
        seedEmployees();
        seedSuppliers();
        System.out.println("[DataSeeder] Seed complete.");
    }

    private static boolean alreadySeeded() {
        String sql = "SELECT COUNT(*) FROM customers";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private static void seedProducts() {
        ProductBean.DAO dao = new ProductBean.DAO();

        String[][] products = {
            {"PROD_001", "Whole Milk 1L",       "Dairy",   "12.50", "2026-08-01"},
            {"PROD_002", "White Bread Loaf",     "Bakery",  "8.75",  "2026-05-30"},
            {"PROD_003", "Cheddar Cheese 500g",  "Dairy",   "35.00", "2026-07-15"},
            {"PROD_004", "Orange Juice 1L",      "Beverages","18.00","2026-09-01"},
            {"PROD_005", "Basmati Rice 2kg",     "Grains",  "45.00", null}
        };

        for (String[] row : products) {
            ProductBean p = new ProductBean();
            p.setProductId(row[0]);
            p.setName(row[1]);
            p.setCategory(row[2]);
            p.setPrice(Double.parseDouble(row[3]));
            p.setExpiryDate(row[4]);
            dao.save(p);
        }
        System.out.println("[DataSeeder] Products seeded.");
    }

    private static void seedStock() {
        String sql = "INSERT OR REPLACE INTO stock (storeId,productId,quantity) VALUES (?,?,?)";
        Object[][] rows = {
            {"STORE_A", "PROD_001", 50},
            {"STORE_A", "PROD_002", 5},
            {"STORE_A", "PROD_003", 20},
            {"STORE_A", "PROD_004", 30},
            {"STORE_A", "PROD_005", 40},
            {"STORE_B", "PROD_001", 30},
            {"STORE_B", "PROD_002", 15},
            {"STORE_B", "PROD_003", 8},
            {"STORE_B", "PROD_004", 25},
            {"STORE_B", "PROD_005", 60},
            {"STORE_C", "PROD_001", 20},
            {"STORE_C", "PROD_002", 12},
            {"STORE_C", "PROD_003", 18},
            {"STORE_C", "PROD_004", 7},
            {"STORE_C", "PROD_005", 35}
        };

        try (Connection conn = DatabaseManager.getConnection()) {
            for (Object[] row : rows) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, (String) row[0]);
                    ps.setString(2, (String) row[1]);
                    ps.setInt(3, (int) row[2]);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.out.println("Failed to seed stock: " + e.getMessage());
        }
        System.out.println("[DataSeeder] Stock seeded.");
    }

    private static void seedCustomers() {
        CustomerBean.DAO customerDao = new CustomerBean.DAO();
        LoyaltyBean.DAO loyaltyDao = new LoyaltyBean.DAO();

        String[][] customers = {
            {"CUST_001", "Ahmed Hassan",   "ahmed@example.com",   "STORE_A"},
            {"CUST_002", "Sara Mohamed",   "sara@example.com",    "STORE_B"},
            {"CUST_003", "Omar Khalifa",   "omar@example.com",    "STORE_C"}
        };

        for (String[] row : customers) {
            CustomerBean c = new CustomerBean();
            c.setCustomerId(row[0]);
            c.setName(row[1]);
            c.setEmail(row[2]);
            c.setRegisteredStoreId(row[3]);
            customerDao.save(c);

            LoyaltyBean l = new LoyaltyBean();
            l.setCustomerId(row[0]);
            l.setPoints(0);
            l.setTier("BRONZE");
            loyaltyDao.save(l);
        }
        System.out.println("[DataSeeder] Customers and loyalty records seeded.");
    }

    private static void seedEmployees() {
        EmployeeBean.DAO dao = new EmployeeBean.DAO();

        String[][] employees = {
            {"EMP_A1", "STORE_A", "Nour Salah",    "MANAGER",     "75.0",  "2023-01-10"},
            {"EMP_A2", "STORE_A", "Karim Adel",    "CASHIER",     "50.0",  "2023-03-15"},
            {"EMP_A3", "STORE_A", "Mona Fathi",    "STOCK_CLERK", "45.0",  "2023-06-01"},
            {"EMP_B1", "STORE_B", "Tamer Hossam",  "MANAGER",     "75.0",  "2022-11-20"},
            {"EMP_B2", "STORE_B", "Dina Ramzy",    "CASHIER",     "50.0",  "2024-01-05"},
            {"EMP_B3", "STORE_B", "Yasser Nabil",  "STOCK_CLERK", "45.0",  "2024-02-14"},
            {"EMP_C1", "STORE_C", "Hana Essam",    "MANAGER",     "75.0",  "2023-07-01"},
            {"EMP_C2", "STORE_C", "Samy Gamal",    "CASHIER",     "50.0",  "2023-09-10"},
            {"EMP_C3", "STORE_C", "Rania Tarek",   "STOCK_CLERK", "45.0",  "2024-03-01"}
        };

        for (String[] row : employees) {
            EmployeeBean e = new EmployeeBean();
            e.setEmployeeId(row[0]);
            e.setStoreId(row[1]);
            e.setName(row[2]);
            e.setRole(row[3]);
            e.setHourlyRate(Double.parseDouble(row[4]));
            e.setStartDate(row[5]);
            dao.save(e);
        }
        System.out.println("[DataSeeder] Employees seeded.");
    }

    private static void seedSuppliers() {
        SupplierBean.DAO dao = new SupplierBean.DAO();

        SupplierBean s1 = new SupplierBean();
        s1.setSupplierId("SUP_001");
        s1.setName("Nile Fresh Distributors");
        s1.setContactEmail("orders@nilefresh.eg");
        s1.setLeadTimeDays(3);
        dao.save(s1);

        SupplierBean s2 = new SupplierBean();
        s2.setSupplierId("SUP_002");
        s2.setName("Cairo Wholesale Co.");
        s2.setContactEmail("supply@cairowholesale.eg");
        s2.setLeadTimeDays(5);
        dao.save(s2);

        System.out.println("[DataSeeder] Suppliers seeded.");
    }
}
