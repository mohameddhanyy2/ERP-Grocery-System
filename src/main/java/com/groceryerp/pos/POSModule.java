package com.groceryerp.pos;

import com.groceryerp.customer.CustomerRepository;
import com.groceryerp.customer.beans.PurchaseHistoryBean;
import com.groceryerp.finance.FinanceRepository;
import com.groceryerp.finance.beans.RevenueBean;
import com.groceryerp.interfaces.ICustomerData;
import com.groceryerp.interfaces.ILoyaltyService;
import com.groceryerp.interfaces.IReceiptService;
import com.groceryerp.interfaces.ISalesData;
import com.groceryerp.interfaces.IStoreInventory;
import com.groceryerp.inventory.CentralInventoryBean;
import com.groceryerp.inventory.InventoryRepository;
import com.groceryerp.inventory.beans.*;
import com.groceryerp.pos.beans.DiscountBean;
import com.groceryerp.pos.beans.PaymentBean;
import com.groceryerp.pos.beans.ReceiptBean;
import com.groceryerp.pos.beans.SaleBean;
import com.groceryerp.pos.beans.SaleItemBean;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;

/**
 * POSModule — now a real @Stateless session bean (previously a plain object
 * manually instantiated and wired in Main.java).
 *
 * Every method is self-contained: it receives its inputs, reads/writes through the
 * injected {@link POSRepository}, and returns a result. No sale data is stored in
 * memory between calls — safe to share across callers.
 *
 * PROVIDED interfaces: ISalesData, IReceiptService
 * REQUIRED interfaces: IStoreInventory, ICustomerData, ILoyaltyService — injected
 *                      by CDI via @Inject instead of the old setter calls in Main.
 *                      CentralInventoryBean is likewise container-injected.
 *
 * Persistence for the five POS beans is delegated to the injected @Stateless
 * {@link POSRepository} (the old SaleBean.DAO / ReceiptBean.DAO / PaymentBean.DAO /
 * DiscountBean.DAO fields).
 *
 * Bean type: @Stateless — no conversational state, all data flows through
 * parameters and the repository.
 */
@Stateless
@LocalBean
public class POSModule implements ISalesData, IReceiptService {

    // ── Injected required interfaces (replaces the old setXxx() setters) ──
    @Inject
    private IStoreInventory storeInventory;
    @Inject
    private ICustomerData customerData;
    @Inject
    private ILoyaltyService loyaltyService;
    @Inject
    private CentralInventoryBean centralInventory;

    /** Container-injected persistence service (replaces the 4 nested POS DAO fields). */
    @EJB
    private POSRepository repository;

    /**
     * Cross-module persistence services. The original code reached into other
     * modules' beans via {@code new ProductBean.DAO()}, {@code new RevenueBean.DAO()}
     * and {@code new PurchaseHistoryBean.DAO()}. Those nested DAOs were removed when
     * those modules migrated to JPA, so the same operations now go through each
     * module's @Stateless repository, injected here by the container.
     */
    @EJB
    private InventoryRepository inventoryRepository;
    @EJB
    private FinanceRepository financeRepository;
    @EJB
    private CustomerRepository customerRepository;

    public POSModule() {}

    // ── Core POS operations ───────────────────────────────────────

    /**
     * Processes a multi-item sale: validates and deducts stock for each item,
     * persists SaleBean and all SaleItemBeans, then processes payment and issues receipt.
     *
     * @param items         list of {productId, quantity} line items
     * @param storeId       ID of the store where the sale happens
     * @param customerId    ID of the customer (or "GUEST")
     * @param paymentMethod CASH, CARD, or WALLET
     * @param amountPaid    amount the customer paid
     * @return completed SaleBean with items populated
     * @throws IllegalStateException if stock is insufficient for any item
     */
    public SaleBean processSale(List<SaleItemBean> items, String storeId, String customerId, String paymentMethod, double amountPaid) {
        IStoreInventory store = storeInventory;
        if (centralInventory != null) {
            store = centralInventory.getStore(storeId);
        }
        if (store == null) throw new IllegalStateException("Unknown store: " + storeId);

        String saleId = "SALE-" + System.currentTimeMillis();

        double subtotal = 0;
        for (SaleItemBean item : items) {
            int available = store.checkStock(item.getProductId());
            if (available < item.getQuantity()) {
                ProductBean p = inventoryRepository.findProductById(item.getProductId());
                String name = p != null ? p.getName() : item.getProductId();
                throw new IllegalStateException("Insufficient stock for \"" + name + "\": need " + item.getQuantity() + ", have " + available);
            }
            ProductBean p = inventoryRepository.findProductById(item.getProductId());
            double unitPrice = p != null ? p.getPrice() : 0;
            item.setItemId("ITEM-" + System.currentTimeMillis() + "-" + item.getProductId());
            item.setSaleId(saleId);
            item.setUnitPrice(unitPrice);
            item.setLineTotal(unitPrice * item.getQuantity());
            subtotal += item.getLineTotal();
        }

        // Deduct stock only after all validations pass
        for (SaleItemBean item : items) {
            store.updateStock(item.getProductId(), -item.getQuantity());
        }

        double taxAmount  = Math.round(subtotal * 0.14 * 100.0) / 100.0;
        double grandTotal = Math.round((subtotal + taxAmount) * 100.0) / 100.0;

        if (amountPaid < grandTotal) {
            throw new IllegalArgumentException("Amount paid (" + amountPaid + ") is less than grand total (" + grandTotal + ")");
        }

        String customerName = customerData.getCustomerName(customerId);
        System.out.println("Processing sale for customer: " + customerName);

        SaleBean sale = new SaleBean();
        sale.setSaleId(saleId);
        sale.setStoreId(storeId);
        sale.setCustomerId(customerId);
        sale.setTotalAmount(subtotal);
        sale.setPaymentMethod(paymentMethod);
        sale.setTimestamp(LocalDateTime.now().toString());
        sale.setDiscountRate(0.0);
        sale.setItems(items);

        repository.saveSale(sale);
        for (SaleItemBean item : items) {
            repository.saveItem(item);
        }

        // Auto-process payment and receipt
        processPayment(sale, amountPaid);

        String today = LocalDateTime.now().toLocalDate().toString();
        String period = today.substring(0, 7); // YYYY-MM

        // Record revenue for finance — use amountPaid (the actual final amount after discounts)
        RevenueBean revenue = new RevenueBean();
        revenue.setRevenueId("REV-" + System.currentTimeMillis());
        revenue.setStoreId(storeId);
        revenue.setPeriod(period);
        revenue.setGrossRevenue(amountPaid);
        revenue.setSaleId(saleId);
        financeRepository.saveRevenue(revenue);

        // Record purchase history and award loyalty points (skip GUEST)
        if (!"GUEST".equals(customerId)) {
            PurchaseHistoryBean history = new PurchaseHistoryBean();
            history.setHistoryId("HIST-" + System.currentTimeMillis());
            history.setCustomerId(customerId);
            history.setSaleId(saleId);
            history.setDate(today);
            history.setAmount(grandTotal);
            customerRepository.savePurchaseHistory(history);

            if (loyaltyService != null) {
                loyaltyService.addLoyaltyPoints(customerId, grandTotal);
            }
        }

        return sale;
    }

    /**
     * Applies a discount to a sale, updates the sale in the DB, and persists the DiscountBean.
     *
     * @param sale          the sale to discount
     * @param discountType  PERCENTAGE or FIXED
     * @param discountValue percentage (0-100) or fixed amount
     * @return the DiscountBean that was applied
     */
    public DiscountBean applyDiscount(SaleBean sale, String discountType, double discountValue) {
        double original = sale.getTotalAmount();
        double discounted;

        if ("PERCENTAGE".equals(discountType)) {
            discounted = original * (1 - discountValue / 100);
            sale.setDiscountRate(discountValue / 100);
        } else {
            discounted = original - discountValue;
            sale.setDiscountRate(discountValue / original);
        }

        sale.setTotalAmount(Math.round(discounted * 100.0) / 100.0);
        repository.saveSale(sale);

        DiscountBean discount = new DiscountBean();
        discount.setDiscountId("DISC-" + System.currentTimeMillis());
        discount.setSaleId(sale.getSaleId());
        discount.setDiscountType(discountType);
        discount.setDiscountValue(discountValue);
        discount.setDescription(discountType + " discount of " + discountValue + " applied");
        repository.saveDiscount(discount);
        return discount;
    }

    /**
     * Processes payment for a sale, persists PaymentBean and ReceiptBean.
     *
     * @param sale       the completed sale
     * @param amountPaid amount tendered by the customer
     * @return the ReceiptBean for this transaction
     * @throws IllegalArgumentException if amountPaid is less than grandTotal
     */
    public ReceiptBean processPayment(SaleBean sale, double amountPaid) {
        double taxAmount  = Math.round(sale.getTotalAmount() * 0.14 * 100.0) / 100.0;
        double grandTotal = Math.round((sale.getTotalAmount() + taxAmount) * 100.0) / 100.0;

        if (amountPaid < grandTotal) {
            throw new IllegalArgumentException(
                    "Amount paid (" + amountPaid + ") is less than grand total (" + grandTotal + ")");
        }

        PaymentBean payment = new PaymentBean();
        payment.setPaymentId("PAY-" + System.currentTimeMillis());
        payment.setSaleId(sale.getSaleId());
        payment.setMethod(sale.getPaymentMethod());
        payment.setAmountPaid(amountPaid);
        payment.setChange(Math.round((amountPaid - grandTotal) * 100.0) / 100.0);
        payment.setProcessedAt(LocalDateTime.now().toString());
        repository.savePayment(payment);

        ReceiptBean receipt = new ReceiptBean();
        receipt.setReceiptId("RECEIPT-" + System.currentTimeMillis());
        receipt.setSaleId(sale.getSaleId());
        receipt.setStoreId(sale.getStoreId());
        receipt.setTotalAmount(sale.getTotalAmount());
        receipt.setTaxAmount(taxAmount);
        receipt.setGrandTotal(grandTotal);
        receipt.setIssuedAt(LocalDateTime.now().toString());
        repository.saveReceipt(receipt);

        return receipt;
    }

    // ── ISalesData (provided) ─────────────────────────────────────

    /** Returns the total revenue for a given store on a given date (date prefix match). */
    @Override
    public double getTotalRevenueBySale(String storeId, String date) {
        return repository.sumRevenueByStoreAndDate(storeId, date);
    }

    /** Returns the number of transactions on a given date (date prefix match). */
    @Override
    public int getTransactionCount(String date) {
        return repository.countByDate(date);
    }

    /** Returns the total amount spent by a customer across all their sales. */
    @Override
    public double getTotalSpendByCustomer(String customerId) {
        return repository.sumRevenueByCustomer(customerId);
    }

    // ── IReceiptService (provided) ────────────────────────────────

    /** Generates a formatted receipt string for a given sale ID, including all line items. */
    @Override
    public String generateReceipt(String saleId) {
        SaleBean sale = repository.findSaleById(saleId);
        if (sale == null) return "RECEIPT NOT FOUND for sale: " + saleId;

        String customerName = customerData.getCustomerName(sale.getCustomerId());
        String storeName    = inventoryRepository.loadStores().stream()
                .filter(m -> sale.getStoreId().equals(m.get("storeId")))
                .map(m -> m.get("storeName"))
                .findFirst()
                .orElse(sale.getStoreId());
        double subtotal     = sale.getTotalAmount();
        double taxAmount    = Math.round(subtotal * 0.14 * 100.0) / 100.0;
        double grandTotal   = Math.round((subtotal + taxAmount) * 100.0) / 100.0;

        // Fetch line items
        java.util.List<SaleItemBean> items = repository.findItemsBySaleId(saleId);

        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("           ERP GROCERY STORE            \n");
        sb.append("========================================\n");
        sb.append(String.format("Store     : %s%n", storeName));
        sb.append(String.format("Sale ID   : %s%n", sale.getSaleId()));
        sb.append(String.format("Customer  : %s%n", customerName));
        sb.append(String.format("Time      : %s%n", sale.getTimestamp().replace("T", " ").substring(0, 19)));
        sb.append(String.format("Payment   : %s%n", sale.getPaymentMethod()));
        sb.append("----------------------------------------\n");
        sb.append(String.format("%-22s %4s %7s %9s%n", "Item", "Qty", "Unit", "Total"));
        sb.append("----------------------------------------\n");
        for (SaleItemBean item : items) {
            ProductBean p = inventoryRepository.findProductById(item.getProductId());
            String name = p != null ? p.getName() : item.getProductId();
            if (name.length() > 22) name = name.substring(0, 19) + "...";
            sb.append(String.format("%-22s %4d %7.2f %9.2f%n",
                name, item.getQuantity(), item.getUnitPrice(), item.getLineTotal()));
        }
        sb.append("----------------------------------------\n");
        sb.append(String.format("%-30s %9.2f%n", "Subtotal (EGP)", subtotal));
        sb.append(String.format("%-30s %9.2f%n", "Tax 14% (EGP)", taxAmount));
        sb.append("========================================\n");
        sb.append(String.format("%-30s %9.2f%n", "TOTAL (EGP)", grandTotal));
        sb.append("========================================\n");
        sb.append("      Thank you for shopping with us!   \n");

        return sb.toString();
    }
}

// reviewed by: Omar Khalifa
