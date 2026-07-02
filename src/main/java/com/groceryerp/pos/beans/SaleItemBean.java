package com.groceryerp.pos.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

/**
 * SaleItemBean — JPA entity mapped to the {@code sale_items} table.
 * One row per line item within a sale.
 *
 * Column names follow the camelCase convention used by the original
 * SaleBean.DAO (the DAO that processSale() actually drove): itemId, saleId,
 * productId, quantity, unitPrice, lineTotal. The nested DAO is gone — persistence
 * is handled by {@link com.groceryerp.pos.POSRepository}.
 *
 * Bean type: @Entity.
 */
@Entity
@Table(name = "sale_items")
public class SaleItemBean implements Serializable {

    @Id
    @Column(name = "itemId")
    private String itemId;
    @Column(name = "saleId")
    private String saleId;
    @Column(name = "productId")
    private String productId;
    @Column(name = "quantity")
    private int quantity;
    @Column(name = "unitPrice")
    private double unitPrice;
    @Column(name = "lineTotal")
    private double lineTotal;

    public SaleItemBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }
}

// reviewed by: Omar Khalifa
