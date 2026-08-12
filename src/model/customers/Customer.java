package model.customers;

import java.io.Serializable;
import java.util.Objects;

public abstract class Customer implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String fullName;
    private String phone;

    public Customer(String id, String fullName, String phone) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }

    // חישוב מחיר סופי בהתאם לסוג הלקוח והמבצע שלו
    public abstract double calculateFinalPrice(double originalPrice);
    public abstract String getCustomerType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] ID: %s | Name: %s | Phone: %s", 
                getCustomerType(), id, fullName, phone);
    }
}