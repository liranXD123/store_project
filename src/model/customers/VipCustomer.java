package model.customers;

// Class representing a VIP customer in the store system, extending the abstract Customer class.
public class VipCustomer extends Customer {
    public VipCustomer(String id, String fullName, String idNumber, String phone) {
        // Calling the constructor of the parent Customer class with ID, full name, ID number and phone number
        super(id, fullName, idNumber, phone);
    }

    @Override
    public double calculateFinalPrice(double originalPrice) {
        // Returning the final price after calculating the discount for a VIP customer - 20% discount
        return originalPrice * 0.80; // 20% discount
    }

    @Override
    public String getCustomerType() {
        // Returning the customer type as VIP
        return "VIP";
    }
}