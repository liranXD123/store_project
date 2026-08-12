package model.customers;

// לקוח חוזר - 10% הנחה
public class ReturningCustomer extends Customer {
    public ReturningCustomer(String id, String fullName, String phone) {
        super(id, fullName, phone);
    }

    @Override
    public double calculateFinalPrice(double originalPrice) {
        return originalPrice * 0.90; // 10% הנחה
    }

    @Override
    public String getCustomerType() {
        return "RETURNING";
    }
}