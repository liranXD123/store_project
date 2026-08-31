package model.customers;

import java.io.Serializable;
import java.util.Objects;



// Abstract class representing a customer in the store system.
// It implements Serializable to allow customer objects to be serialized for storage or transmission.
public abstract class Customer implements Serializable {
    private static final long serialVersionUID = 1L; // Unique identifier for serialization, ensuring that a deserialized object matches the version of the class used to serialize it.
    // Customer fields
    private String id;
    private String fullName;
    private String phone;

    public Customer(String id, String fullName, String phone) {
        // Constructor for the Customer class that accepts all required fields for creating a new customer object
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
    }

    // Getters for the customer fields
    public void setId(String id) { this.id = id; }
    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }

    // Calculation of final price based on customer type and their offers
    public abstract double calculateFinalPrice(double originalPrice);
    public abstract String getCustomerType();

    @Override
    public boolean equals(Object o) {
        // Checking if the current object is the same as the passed object, and if not, checking if it's of the same type and comparing customer IDs
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        // Generating a hash code based on the customer ID, ensuring that equal objects have the same hash code
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        // Returning a string representation of the object, including customer type, ID, full name, and phone number
        return String.format("[%s] ID: %s | Name: %s | Phone: %s", 
                getCustomerType(), id, fullName, phone);
    }
}