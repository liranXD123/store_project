package model.customers;

// Class representing a new customer in the store system, extending the abstract Customer class.
public class NewCustomer extends Customer {
    public NewCustomer(String id, String fullName, String phone) {
        // Calling the constructor of the parent Customer class with ID, full name, and phone number
        super(id, fullName, phone);
    }

    @Override
    public double calculateFinalPrice(double originalPrice) {
        // Returning the final price after calculating the discount for a new customer - 5% discount
        return originalPrice * 0.95;
    }

    @Override
    public String getCustomerType() {
        // Returning the customer type as NEW
        return "NEW";
    }
}