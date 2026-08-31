package model;

import exceptions.OutOfStockException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// Class representing a branch in the store system, implementing Serializable to allow branch objects to be serialized for storage or transmission.
public class Branch implements Serializable {
    private static final long serialVersionUID = 1L; // Unique identifier for serialization, ensuring that a deserialized object matches the version of the class used to serialize it.

    // Branch fields
    private String branchId;
    private String branchName;
    // Map storing the inventory of products in the branch
    private final Map<Product, Integer> inventory = new HashMap<>();
    private final Object inventoryLock = new Object();

    public Branch(String branchId, String branchName) {
        // Constructor for the Branch class that accepts all required fields for creating a new branch object
        this.branchId = branchId;
        this.branchName = branchName;
    }

    public String getBranchId() { return branchId; }
    public String getBranchName() { return branchName; }

    public void addStock(Product product, int quantity) {
        // Adding stock for a specific product in the branch, with synchronization to prevent race conditions between threads
        synchronized (inventoryLock) {
            int current = inventory.getOrDefault(product, 0);
            inventory.put(product, current + quantity);
        }
    }

    public void reduceStock(Product product, int quantity) throws OutOfStockException {
        // Reducing stock for a specific product in the branch, with synchronization to prevent race conditions between threads
        synchronized (inventoryLock) {
            int current = inventory.getOrDefault(product, 0);
            if (current < quantity) {
                throw new OutOfStockException("Not enough stock in branch " + branchName + " for " + product.getName());
            }
            inventory.put(product, current - quantity);
        }
    }

    public Map<Product, Integer> getInventorySnapshot() {
        // Returning an unmodifiable copy of the current inventory in the branch, with synchronization to prevent race conditions between threads
        synchronized (inventoryLock) {
            return Collections.unmodifiableMap(new HashMap<>(inventory));
        }
    }
}