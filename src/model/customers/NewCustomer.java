package model.customers;

// לקוח חדש - ללא הנחה מיוחדת או הנחת הצטרפות חד פעמית (5%)
public class NewCustomer extends Customer {
    public NewCustomer(String id, String fullName, String phone) {
        // קריאה לבנאי של המחלקה האב Customer עם מזהה, שם מלא ומספר טלפון
        super(id, fullName, phone);
    }

    @Override
    public double calculateFinalPrice(double originalPrice) {
        // החזרת המחיר הסופי לאחר חישוב ההנחה ללקוח חדש - 5% הנחה
        return originalPrice * 0.95;
    }

    @Override
    public String getCustomerType() {
        // החזרת סוג הלקוח כ-NEW
        return "NEW";
    }
}