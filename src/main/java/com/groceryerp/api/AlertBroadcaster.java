package com.groceryerp.api;

import com.groceryerp.supplier.SupplierRepository;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AlertBroadcaster — container-managed @Singleton EJB that holds open SSE
 * connections and pushes events to keep both UIs live without polling.
 *
 * There are two kinds of subscriber:
 *   - SUPPLIER clients, keyed by supplierId (the supplier portal, :5174). Each
 *     supplier only sees events relevant to them.
 *   - MANAGER clients, a single shared channel (the manager UI Supplier page,
 *     :5173). Every order-lifecycle change is pushed here so the manager view
 *     refreshes immediately.
 *
 * Events are plain SSE "named events" carrying a short data string; the frontend
 * reacts by re-fetching, so the payload only needs to say "something changed"
 * (and which type), not the full object.
 *
 * ConcurrencyManagement is BEAN so the ConcurrentHashMap / CopyOnWriteArrayList
 * structures handle concurrency themselves rather than the container serializing
 * every call.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class AlertBroadcaster {

    /** Synthetic key for the shared manager channel in {@link #clients}. */
    private static final String MANAGER_KEY = "__MANAGER__";

    @EJB
    private SupplierRepository supplierRepository;

    // subscriberKey (supplierId or MANAGER_KEY) → list of open SSE writers
    private final Map<String, List<PrintWriter>> clients = new ConcurrentHashMap<>();

    // ── subscription management ──────────────────────────────────────

    /** Register an SSE writer for a connected supplier. */
    public void subscribe(String supplierId, PrintWriter writer) {
        clients.computeIfAbsent(supplierId, k -> new CopyOnWriteArrayList<>()).add(writer);
    }

    /** Remove a supplier writer when its SSE connection closes. */
    public void unsubscribe(String supplierId, PrintWriter writer) {
        removeWriter(supplierId, writer);
    }

    /** Register an SSE writer for the shared manager channel. */
    public void subscribeManager(PrintWriter writer) {
        clients.computeIfAbsent(MANAGER_KEY, k -> new CopyOnWriteArrayList<>()).add(writer);
    }

    /** Remove a manager writer when its SSE connection closes. */
    public void unsubscribeManager(PrintWriter writer) {
        removeWriter(MANAGER_KEY, writer);
    }

    private void removeWriter(String key, PrintWriter writer) {
        List<PrintWriter> list = clients.get(key);
        if (list != null) { list.remove(writer); }
    }

    // ── event pushing ────────────────────────────────────────────────

    /**
     * Low-stock alert: push an "alert" event to every supplier whose catalogue
     * includes the product, and notify the manager channel so its alert/order
     * views refresh too.
     */
    public void broadcast(String productId) {
        for (String supplierId : supplierRepository.findSupplierIdsForProduct(productId)) {
            send(supplierId, "alert", productId);
        }
        send(MANAGER_KEY, "alert", productId);
    }

    /** Push a named order-lifecycle event to one supplier. */
    public void notifySupplier(String supplierId, String event, String data) {
        send(supplierId, event, data);
    }

    /** Push a named order-lifecycle event to the manager channel. */
    public void notifyManager(String event, String data) {
        send(MANAGER_KEY, event, data);
    }

    /**
     * Notify BOTH sides of an order change so whichever UI didn't initiate it
     * still updates live. Used for quote / accept / outfordelivery / delivery.
     */
    public void notifyOrderChange(String supplierId, String event, String orderId) {
        if (supplierId != null) { send(supplierId, event, orderId); }
        send(MANAGER_KEY, event, orderId);
    }

    /** Write one SSE event to every writer under a key, dropping dead writers. */
    private void send(String key, String event, String data) {
        List<PrintWriter> list = clients.get(key);
        if (list == null) { return; }
        String payload = "event: " + event + "\ndata: " + data + "\n\n";
        list.removeIf(w -> {
            try {
                w.write(payload);
                w.flush();
                return w.checkError();
            } catch (Exception e) {
                return true;
            }
        });
    }
}
