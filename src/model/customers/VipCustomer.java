package model.customers;

// לקוח VIP - 20% הנחה קבועה
public class VipCustomer extends Customer {
    public VipCustomer(String id, String fullName, String phone) {
        super(id, fullName, phone);
    }

    @Override
    public double calculateFinalPrice(double originalPrice) {
        return originalPrice * 0.80; // 20% הנחה
    }

    @Override
    public String getCustomerType() {
        return "VIP";
    }
}