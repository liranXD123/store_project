package model.customers;

// לקוח חדש - ללא הנחה מיוחדת או הנחת הצטרפות חד פעמית (5%)
public class NewCustomer extends Customer {
    public NewCustomer(String id, String fullName, String phone) {
        super(id, fullName, phone);
    }

    @Override
    public double calculateFinalPrice(double originalPrice) {
        return originalPrice * 0.95; // 5% הנחה
    }

    @Override
    public String getCustomerType() {
        return "NEW";
    }
}