package com.groceryerp.api;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton that holds open SSE (Server-Sent Events) connections from supplier
 * portal clients. When a LOW_STOCK alert is saved, the MDB calls broadcast()
 * with the affected productId so any connected supplier carrying that product
 * receives an "alert" event and refreshes in real time.
 */
public class AlertBroadcaster {

    private static final AlertBroadcaster INSTANCE = new AlertBroadcaster();

    // supplierId → list of open SSE writers
    private final Map<String, List<PrintWriter>> clients = new ConcurrentHashMap<>();

    private AlertBroadcaster() {}

    public static AlertBroadcaster getInstance() { return INSTANCE; }

    /** Register an SSE writer for a connected supplier. */
    public void subscribe(String supplierId, PrintWriter writer) {
        clients.computeIfAbsent(supplierId, k -> new CopyOnWriteArrayList<>()).add(writer);
    }

    /** Remove a writer when the SSE connection closes. */
    public void unsubscribe(String supplierId, PrintWriter writer) {
        List<PrintWriter> list = clients.get(supplierId);
        if (list != null) list.remove(writer);
    }

    /**
     * Push an "alert" event to every supplier whose product catalogue includes
     * the given productId. Stale/closed writers are cleaned up automatically.
     */
    public void broadcast(String productId) {
        // Look up which suppliers carry this product
        List<String> supplierIds = suppliersForProduct(productId);
        for (String supplierId : supplierIds) {
            List<PrintWriter> list = clients.get(supplierId);
            if (list == null) continue;
            list.removeIf(w -> {
                try {
                    w.write("event: alert\ndata: " + productId + "\n\n");
                    w.flush();
                    return w.checkError();
                } catch (Exception e) {
                    return true;
                }
            });
        }
    }

    private List<String> suppliersForProduct(String productId) {
        List<String> ids = new java.util.ArrayList<>();
        String sql = "SELECT supplierId FROM supplier_products WHERE productId = ?";
        try (java.sql.Connection conn = com.groceryerp.db.DatabaseManager.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getString("supplierId"));
            }
        } catch (java.sql.SQLException e) {
            System.out.println("[AlertBroadcaster] DB error: " + e.getMessage());
        }
        return ids;
    }
}
