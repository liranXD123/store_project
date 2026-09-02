package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Class representing a sale record in the store system, implementing Serializable to allow sale record objects to be serialized for storage or transmission.
// It also implements Comparable so that a collection of sales can be sorted by the time of the sale.
public class SaleRecord implements Serializable, Comparable<SaleRecord> {
    private static final long serialVersionUID = 1L; // Unique identifier for the class version, used during serialization to ensure the class version matches the serialized object

    // Sale record fields
    private String transactionId;
    private String branchId;
    private String employeeId;
    private String customerId;
    private String productId;
    private String productName;
    private String category;
    private int quantity;
    private double finalPrice;
    private LocalDateTime timestamp;

    // Constructor for a sale that is happening right now, so the time of the sale is the current time
    public SaleRecord(String transactionId, String branchId, String employeeId,
                      String customerId, String productId, String productName,
                      String category, int quantity, double finalPrice) {
        this(transactionId, branchId, employeeId, customerId, productId, productName,
                category, quantity, finalPrice, LocalDateTime.now());
    }

    // Constructor that also receives the time of the sale, used when an old sale is read
    // back from the database and has to keep the time it originally happened
    public SaleRecord(String transactionId, String branchId, String employeeId,
                      String customerId, String productId, String productName,
                      String category, int quantity, double finalPrice, LocalDateTime timestamp) {
        this.transactionId = transactionId;
        this.branchId = branchId;
        this.employeeId = employeeId;
        this.customerId = customerId;
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.quantity = quantity;
        this.finalPrice = finalPrice;
        this.timestamp = timestamp;
    }

    // Getters for the sale record fields
    public String getTransactionId() { return transactionId; }
    public String getBranchId() { return branchId; }
    public String getEmployeeId() { return employeeId; }
    public String getCustomerId() { return customerId; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getCategory() { return category; }
    public int getQuantity() { return quantity; }
    public double getFinalPrice() { return finalPrice; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // Comparing two sales by the time they were made, so that a report is ordered chronologically
    @Override
    public int compareTo(SaleRecord other) {
        return this.timestamp.compareTo(other.timestamp);
    }

    // Returning the date of the sale as yyyy-MM-dd, used for filtering a daily report
    public String getSaleDate() {
        return timestamp.toLocalDate().toString();
    }

    // Returning a string representation of the SaleRecord object, including all the key fields
    public String toLogString() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("[%s] Trans: %s | Branch: %s | Emp: %s | Cust: %s | Item: %s (x%d) | Total: ₪%.2f",
                timestamp.format(dtf), transactionId, branchId, employeeId, customerId, productName, quantity, finalPrice);
    }
}