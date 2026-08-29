package model.customers;

// לקוח VIP - 20% הנחה קבועה
public class VipCustomer extends Customer {
    public VipCustomer(String id, String fullName, String phone) {
        // קריאה לבנאי של המחלקה האב Customer עם מזהה, שם מלא ומספר טלפון
        super(id, fullName, phone);
    }

    @Override
    public double calculateFinalPrice(double originalPrice) {
        // החזרת המחיר הסופי לאחר חישוב ההנחה ללקוח VIP - 20% הנחה
        return originalPrice * 0.80; // 20% הנחה
    }

    @Override
    public String getCustomerType() {
        // החזרת סוג הלקוח כ-VIP
        return "VIP";
    }
}