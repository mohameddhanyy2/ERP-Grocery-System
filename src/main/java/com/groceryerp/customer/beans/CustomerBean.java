package com.groceryerp.customer.beans;

import java.io.Serializable;

/** JavaBean representing a Customer in the Customer module. */
public class CustomerBean implements Serializable {
    private int customerId;
    private String name;
    private String email;
    private String registeredStoreId;

    public CustomerBean() {}

    public CustomerBean(int customerId, String name, String email, String registeredStoreId) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.registeredStoreId = registeredStoreId;
    }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRegisteredStoreId() { return registeredStoreId; }
    public void setRegisteredStoreId(String registeredStoreId) { this.registeredStoreId = registeredStoreId; }

    @Override
    public String toString() {
        return "CustomerBean{" +
                "customerId=" + customerId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", registeredStoreId='" + registeredStoreId + '\'' +
                '}';
    }
}
