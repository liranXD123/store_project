package model;

import exceptions.OutOfStockException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Branch implements Serializable {
    private static final long serialVersionUID = 1L;

    private String branchId;
    private String branchName;
    // Map מסונכרן בין מוצר לכמות במלאי
    private final Map<Product, Integer> inventory = new HashMap<>();
    private final Object inventoryLock = new Object();

    public Branch(String branchId, String branchName) {
        this.branchId = branchId;
        this.branchName = branchName;
    }

    public String getBranchId() { return branchId; }
    public String getBranchName() { return branchName; }

    public void addStock(Product product, int quantity) {
        synchronized (inventoryLock) {
            int current = inventory.getOrDefault(product, 0);
            inventory.put(product, current + quantity);
        }
    }

    public void reduceStock(Product product, int quantity) throws OutOfStockException {
        synchronized (inventoryLock) {
            int current = inventory.getOrDefault(product, 0);
            if (current < quantity) {
                throw new OutOfStockException("Not enough stock in branch " + branchName + " for " + product.getName());
            }
            inventory.put(product, current - quantity);
        }
    }

    public Map<Product, Integer> getInventorySnapshot() {
        synchronized (inventoryLock) {
            return Collections.unmodifiableMap(new HashMap<>(inventory));
        }
    }
}