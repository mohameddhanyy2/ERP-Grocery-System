package com.groceryerp.inventory;

import com.groceryerp.api.AlertBroadcaster;
import com.groceryerp.inventory.beans.StockAlertBean;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;

/**
 * StockAlertMDB — now a REAL Message-Driven Bean (previously a plain object with
 * an onMessage(String,Object) method that was simply called like any other
 * method, holding none of the asynchronous semantics it claimed).
 *
 * It listens on the StockAlert JMS queue. When {@link StockAlertProducer} sends a
 * low-stock event, the container delivers it here on a separate thread, the bean
 * persists the alert via the injected {@link InventoryRepository}, then pushes an
 * SSE notification through the {@link AlertBroadcaster}.
 *
 * Flow: StoreInventoryBean.updateStock() detects low stock
 *       -> StockAlertProducer.fireLowStock() sends JMS message
 *       -> (async) this MDB.onMessage() persists + broadcasts.
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType",
                              propertyValue = "jakarta.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destinationLookup",
                              propertyValue = "java:/jms/queue/StockAlert")
})
public class StockAlertMDB implements MessageListener {

    @EJB
    private InventoryRepository inventoryRepository;

    @Inject
    private AlertBroadcaster alertBroadcaster;

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof ObjectMessage objectMessage) {
                Object body = objectMessage.getObject();
                if (body instanceof StockAlertEvent event) {
                    StockAlertBean alert = event.getAlert();
                    inventoryRepository.saveAlert(alert);
                    alertBroadcaster.broadcast(alert.getProductId());
                    System.out.println("[MDB] Low stock alert saved: product=" + alert.getProductId()
                            + " store=" + alert.getStoreId()
                            + " qty=" + alert.getCurrentQty()
                            + " threshold=" + alert.getThreshold()
                            + " — awaiting supplier quote");
                }
            }
        } catch (JMSException e) {
            System.out.println("[MDB] Failed to process stock alert message: " + e.getMessage());
        }
    }
}
