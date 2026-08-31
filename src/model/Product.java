package model;

import java.io.Serializable;
import java.util.Objects;

// Class representing a product in the store system, implementing Serializable to allow product objects to be serialized for storage or transmission.
public class Product implements Serializable {
    private static final long serialVersionUID = 1L; // Unique identifier for serialization, ensuring that a deserialized object matches the version of the class used to serialize it.

    // Product fields
    private String id;
    private String name;
    private String category;
    private double basePrice;

    public Product(String id, String name, String category, double basePrice) {
        // Constructor for the Product class that accepts all required fields for creating a new product object
        this.id = id;
        this.name = name;
        this.category = category;
        this.basePrice = basePrice;
    }

    // Getters for the product fields
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getBasePrice() { return basePrice; }

    @Override
    public boolean equals(Object o) {
        // Checking if the current object is the same object as the one passed, and if not, checking if it's of the same type and comparing the product ID
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        // Returning the hash code based on the product ID, used for storing the object in data structures like HashMap or HashSet
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        // Returning a string representation of the object, including the product ID, name, category, and base price
        return String.format("Product[%s - %s (%s) - ₪%.2f]", id, name, category, basePrice);
    }
}