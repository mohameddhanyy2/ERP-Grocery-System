package com.groceryerp.integration;

import com.groceryerp.finance.FinanceModule;
import com.groceryerp.customer.CustomerModule;
import com.groceryerp.stubs.*;
import com.groceryerp.customer.beans.*;
import com.groceryerp.finance.beans.*;
import java.time.LocalDate;
import java.util.List;

/** Demo runner to exercise FinanceModule and CustomerModule with stubs. */
public class DemoMain {
    public static void main(String[] args) {
        // create stubs
        SalesDataStub salesStub = new SalesDataStub();
        StaffDataStub staffStub = new StaffDataStub();
        OrderStatusStub orderStub = new OrderStatusStub();

        // finance module wiring
        FinanceModule finance = new FinanceModule();
        finance.setSalesData(salesStub);
        finance.setStaffData(staffStub);
        finance.setOrderStatus(orderStub);

        // customer module wiring
        CustomerModule customer = new CustomerModule();
        customer.setSalesData(salesStub);

        // seed customer data
        CustomerBean c1 = new CustomerBean(1, "Alice", "alice@example.com", "store-1");
        customer.addCustomer(c1);
        customer.addPurchase(1, new PurchaseHistoryBean(1, "sale-100", LocalDate.now(), 120.0));
        customer.addLoyaltyPoints(1, 120.0);

        // print customer profile and loyalty
        System.out.println("Customer profile: " + customer.getCustomerProfile(1));
        System.out.println("Loyalty points: " + customer.getLoyaltyPoints("1"));
        System.out.println("Loyalty tier: " + customer.getLoyaltyTier("1"));

        // print purchase history
        List<PurchaseHistoryBean> history = customer.getPurchaseHistory(1);
        System.out.println("Purchase history: " + history);

        // finance calculations
        System.out.println("Calc profit: " + finance.calcProfit());
        ProfitSummaryBean summary = finance.getFinancialSummary();
        System.out.println("Profit summary: " + summary);
    }
}
