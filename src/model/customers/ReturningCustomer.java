package model.customers;

// לקוח חוזר - 10% הנחה
public class ReturningCustomer extends Customer {
    // בנאי למחלקת ReturningCustomer שמקבל מזהה, שם מלא ומספר טלפון
    public ReturningCustomer(String id, String fullName, String phone) {
        super(id, fullName, phone);
    }

    @Override
    public double calculateFinalPrice(double originalPrice) {
        // החזרת המחיר הסופי לאחר חישוב ההנחה ללקוח חוזר - 10% הנחה
        return originalPrice * 0.90;
    }

    @Override
    public String getCustomerType() {
        // החזרת סוג הלקוח כ-Returning
        return "RETURNING";
    }
}