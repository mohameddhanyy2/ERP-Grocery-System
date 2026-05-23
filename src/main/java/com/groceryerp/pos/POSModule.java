package com.groceryerp.pos;

// @Stateless
// Chosen because every method receives all data it needs through parameters,
// reads/writes through DAOs, and returns a result immediately. No business state
// accumulates between calls. Safe to share across callers without conflict.

import com.groceryerp.interfaces.ICustomerData;
import com.groceryerp.interfaces.IReceiptService;
import com.groceryerp.interfaces.ISalesData;
import com.groceryerp.interfaces.IStoreInventory;
import com.groceryerp.inventory.beans.*;
import com.groceryerp.pos.beans.DiscountBean;
import com.groceryerp.pos.beans.PaymentBean;
import com.groceryerp.pos.beans.ReceiptBean;
import com.groceryerp.pos.beans.SaleBean;
import com.groceryerp.pos.beans.SaleItemBean;

import java.time.LocalDateTime;

/**
 * POSModule — Stateless Session Bean for sales processing, discounts, payments, and receipts.
 *
 * Every method is self-contained: it receives its inputs, reads/writes through DAOs,
 * and returns a result. No sale data is stored in memory between calls.
 *
 * PROVIDED interfaces: ISalesData, IReceiptService
 * REQUIRED interfaces: IStoreInventory, ICustomerData (injected via IoC setters)
 *
 * Bean type: @Stateless — no conversational state, all data flows through parameters and DAOs.
 */
public class POSModule implements ISalesData, IReceiptService {

    // ── Injected required interfaces ──
    private IStoreInventory storeInventory;
    private ICustomerData customerData;

    // ── DAO fields (infrastructure) ───────────
    private final SaleBean.DAO saleDao       = new SaleBean.DAO();
    private final ReceiptBean.DAO receiptDao = new ReceiptBean.DAO();
    private final PaymentBean.DAO paymentDao = new PaymentBean.DAO();
    private final DiscountBean.DAO discountDao = new DiscountBean.DAO();

    public POSModule() {}

    // ── Setters ───────────────────────────────────────────────

    /** store inventory dependency. */
    public void setStoreInventory(IStoreInventory storeInventory) {
        this.storeInventory = storeInventory;
    }

    /** customer data dependency. */
    public void setCustomerData(ICustomerData customerData) {
        this.customerData = customerData;
    }

    // ── Core POS operations ───────────────────────────────────────

    /**
     * Processes a sale: checks stock, deducts inventory, persists SaleBean and SaleItemBean.
     *
     * @param productId     ID of the product being sold
     * @param quantity      number of units to sell
     * @param storeId       ID of the store
     * @param customerId    ID of the customer
     * @param paymentMethod CASH, CARD, or WALLET
     * @param amountPaid    amount the customer paid
     * @return completed SaleBean
     * @throws IllegalStateException if stock is insufficient
     */
    public SaleBean processSale(String productId, int quantity, String storeId, String customerId, String paymentMethod, double amountPaid) {
        int stock = storeInventory.checkStock(productId);
        if (stock < quantity) {
            throw new IllegalStateException(
                    "Insufficient stock for " + productId + ": requested " + quantity + ", available " + stock);
        }

        storeInventory.updateStock(productId, -quantity);

        String customerName = customerData.getCustomerName(customerId);
        System.out.println("Processing sale for customer: " + customerName);

        ProductBean productBean = new ProductBean.DAO().findById(productId);
        double unitPrice = productBean != null ? productBean.getPrice() : 0;

        SaleBean sale = new SaleBean();
        sale.setSaleId("SALE-" + System.currentTimeMillis());
        sale.setStoreId(storeId);
        sale.setCustomerId(customerId);
        sale.setTotalAmount(unitPrice * quantity);
        sale.setPaymentMethod(paymentMethod);
        sale.setTimestamp(LocalDateTime.now().toString());
        sale.setDiscountRate(0.0);

        SaleItemBean item = new SaleItemBean();
        item.setItemId("ITEM-" + System.currentTimeMillis());
        item.setSaleId(sale.getSaleId());
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setLineTotal(unitPrice * quantity);

        sale.getItems().add(item);
        saleDao.save(sale);
        saleDao.saveItem(item);

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
        saleDao.save(sale);

        DiscountBean discount = new DiscountBean();
        discount.setDiscountId("DISC-" + System.currentTimeMillis());
        discount.setSaleId(sale.getSaleId());
        discount.setDiscountType(discountType);
        discount.setDiscountValue(discountValue);
        discount.setDescription(discountType + " discount of " + discountValue + " applied");
        discountDao.save(discount);
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
        paymentDao.save(payment);

        ReceiptBean receipt = new ReceiptBean();
        receipt.setReceiptId("RECEIPT-" + System.currentTimeMillis());
        receipt.setSaleId(sale.getSaleId());
        receipt.setStoreId(sale.getStoreId());
        receipt.setTotalAmount(sale.getTotalAmount());
        receipt.setTaxAmount(taxAmount);
        receipt.setGrandTotal(grandTotal);
        receipt.setIssuedAt(LocalDateTime.now().toString());
        receiptDao.save(receipt);

        return receipt;
    }

    // ── ISalesData (provided) ─────────────────────────────────────

    /** Returns the total revenue for a given store on a given date (date prefix match). */
    @Override
    public double getTotalRevenueBySale(String storeId, String date) {
        return saleDao.sumRevenueByStoreAndDate(storeId, date);
    }

    /** Returns the number of transactions on a given date (date prefix match). */
    @Override
    public int getTransactionCount(String date) {
        return saleDao.countByDate(date);
    }

    /** Returns the total amount spent by a customer across all their sales. */
    @Override
    public double getTotalSpendByCustomer(String customerId) {
        return saleDao.sumRevenueByCustomer(customerId);
    }

    // ── IReceiptService (provided) ────────────────────────────────

    /** Generates a formatted receipt string for a given sale ID, fetched from DB. */
    @Override
    public String generateReceipt(String saleId) {
        SaleBean sale = saleDao.findById(saleId);
        if (sale == null) {
            return "RECEIPT-NOT-FOUND for sale: " + saleId;
        }

        String customerName = customerData.getCustomerName(sale.getCustomerId());
        double taxAmount  = Math.round(sale.getTotalAmount() * 0.14 * 100.0) / 100.0;
        double grandTotal = Math.round((sale.getTotalAmount() + taxAmount) * 100.0) / 100.0;

        return "===== RECEIPT =====" + "\n" +
               "Sale ID   : " + sale.getSaleId() + "\n" +
               "Store     : " + sale.getStoreId() + "\n" +
               "Customer  : " + customerName + "\n" +
               "Total     : " + sale.getTotalAmount() + "\n" +
               "Tax (14%) : " + taxAmount + "\n" +
               "Grand Total: " + grandTotal + "\n" +
               "Issued At : " + LocalDateTime.now() + "\n" +
               "==================";
    }
}

// reviewed by: Omar Khalifa