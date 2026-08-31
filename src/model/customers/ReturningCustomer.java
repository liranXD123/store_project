package model.customers;

// Class representing a returning customer in the store system, extending the abstract Customer class.
public class ReturningCustomer extends Customer {
    // Constructor for the ReturningCustomer class that accepts all required fields for creating a new returning customer object
    public ReturningCustomer(String id, String fullName, String phone) {
        super(id, fullName, phone);
    }

    @Override
    public double calculateFinalPrice(double originalPrice) {
        // Returning the final price after calculating the discount for a returning customer - 10% discount
        return originalPrice * 0.90;
    }

    @Override
    public String getCustomerType() {
        // Returning the customer type as RETURNING
        return "RETURNING";
    }
}