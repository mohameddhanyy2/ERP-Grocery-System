package com.groceryerp.inventory;

import com.groceryerp.inventory.beans.StockAlertBean;
import java.io.Serializable;

/**
 * Serializable payload carried by the JMS low-stock message. Wraps the
 * StockAlertBean so the real @MessageDriven {@link StockAlertMDB} can rebuild
 * the alert from an ObjectMessage. Replaces the old approach of passing a raw
 * Object payload to a fake MDB method call.
 */
public class StockAlertEvent implements Serializable {

    private StockAlertBean alert;

    public StockAlertEvent() {}

    public StockAlertEvent(StockAlertBean alert) { this.alert = alert; }

    public StockAlertBean getAlert() { return alert; }
    public void setAlert(StockAlertBean alert) { this.alert = alert; }
}
