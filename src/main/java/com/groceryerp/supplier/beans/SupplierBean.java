package com.groceryerp.supplier.beans;

import java.io.Serializable;

/**
 * JavaBean — represents a supplier company.
 * Follows JavaBeans spec: public no-arg constructor, private fields, public getters/setters.
 */
public class SupplierBean implements Serializable {

    private String supplierId;
    private String name;
    private String contactEmail;
    private int leadTimeDays;

    public SupplierBean() {}

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public int getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(int leadTimeDays) { this.leadTimeDays = leadTimeDays; }

    @Override
    public String toString() {
        return "SupplierBean{supplierId='" + supplierId + "', name='" + name +
               "', contactEmail='" + contactEmail + "', leadTimeDays=" + leadTimeDays + "}";
    }
}
