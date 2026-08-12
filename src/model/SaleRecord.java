package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SaleRecord implements Serializable {
    private static final long serialVersionUID = 1L;

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

    public SaleRecord(String transactionId, String branchId, String employeeId, 
                      String customerId, String productId, String productName, 
                      String category, int quantity, double finalPrice) {
        this.transactionId = transactionId;
        this.branchId = branchId;
        this.employeeId = employeeId;
        this.customerId = customerId;
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.quantity = quantity;
        this.finalPrice = finalPrice;
        this.timestamp = LocalDateTime.now();
    }

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

    public String toLogString() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("[%s] Trans: %s | Branch: %s | Emp: %s | Cust: %s | Item: %s (x%d) | Total: ₪%.2f",
                timestamp.format(dtf), transactionId, branchId, employeeId, customerId, productName, quantity, finalPrice);
    }
}