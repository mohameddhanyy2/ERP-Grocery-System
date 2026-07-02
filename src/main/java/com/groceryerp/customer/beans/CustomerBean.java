package com.groceryerp.customer.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * CustomerBean — JPA entity mapped to the {@code customers} table.
 *
 * Was: a plain JavaBean with a nested {@code DAO} static class that hand-wrote
 * SQLite SQL (INSERT OR REPLACE / SELECT). The DAO is gone — persistence is now
 * handled by {@link com.groceryerp.customer.CustomerRepository} via the
 * container-managed EntityManager. The annotations below ARE the table mapping.
 */
@Entity
@Table(name = "customers")
public class CustomerBean implements Serializable {

    @Id
    @Column(name = "customerId")
    private String customerId;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "registeredStoreId")
    private String registeredStoreId;

    public CustomerBean() {}

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRegisteredStoreId() { return registeredStoreId; }
    public void setRegisteredStoreId(String registeredStoreId) { this.registeredStoreId = registeredStoreId; }
}
