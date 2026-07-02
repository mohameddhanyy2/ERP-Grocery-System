package com.groceryerp.inventory;

import com.groceryerp.inventory.beans.StockAlertBean;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;

/**
 * StockAlertProducer — @Stateless session bean that publishes low-stock events
 * onto a JMS queue. This is the SENDER half of the now-real message-driven flow.
 *
 * Before: StoreInventoryBean held a direct reference to a fake "MDB" object and
 * called stockAlertMDB.onMessage("LOW_STOCK", alert) synchronously. That was a
 * plain method call, not messaging.
 *
 * After: StoreInventoryBean injects this producer and calls fireLowStock(alert);
 * the container delivers the message asynchronously to {@link StockAlertMDB},
 * which is a genuine @MessageDriven bean listening on the same queue. This is
 * the actual loose-coupling/async semantics the @MessageDriven comment always
 * claimed but never had.
 */
@Stateless
public class StockAlertProducer {

    @Inject
    private JMSContext jmsContext;

    /** The destination is configured in the app server (e.g. java:/jms/queue/StockAlert). */
    @Resource(lookup = "java:/jms/queue/StockAlert")
    private Queue stockAlertQueue;

    /**
     * Publishes a low-stock alert as a JMS ObjectMessage. Returns immediately;
     * the MDB processes it on a container-managed thread.
     */
    public void fireLowStock(StockAlertBean alert) {
        jmsContext.createProducer().send(stockAlertQueue, new StockAlertEvent(alert));
    }
}
