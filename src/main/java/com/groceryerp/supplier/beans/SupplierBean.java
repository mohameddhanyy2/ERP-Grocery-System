package com.groceryerp.supplier.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * SupplierBean — JPA entity mapped to the {@code suppliers} table.
 *
 * Was: a plain JavaBean with a nested {@code DAO} static class that hand-wrote
 * SQLite SQL (INSERT OR REPLACE / SELECT). The DAO is gone — persistence is now
 * handled by {@link com.groceryerp.supplier.SupplierRepository} via the
 * container-managed EntityManager. The annotations below ARE the table mapping.
 */
@Entity
@Table(name = "suppliers")
public class SupplierBean implements Serializable {

    @Id
    @Column(name = "supplierId")
    private String supplierId;

    @Column(name = "name")
    private String name;

    @Column(name = "contactEmail")
    private String contactEmail;

    @Column(name = "leadTimeDays")
    private int leadTimeDays;

    public SupplierBean() { /* no-arg constructor required by JPA */ }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public int getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(int leadTimeDays) { this.leadTimeDays = leadTimeDays; }
}
