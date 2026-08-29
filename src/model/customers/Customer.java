package model.customers;

import java.io.Serializable;
import java.util.Objects;

public abstract class Customer implements Serializable {
    private static final long serialVersionUID = 1L; // מזהה ייחודי לגרסה של המחלקה, משמש בעת סריאליזציה כדי לוודא שהגרסה של המחלקה תואמת לגרסה של האובייקט המוסר
    
    // שדות הלקוח
    private String id;
    private String fullName;
    private String phone;

    public Customer(String id, String fullName, String phone) {
        // בנאי למחלקת Customer שמקבל את כל השדות הנדרשים ליצירת אובייקט לקוח חדש
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
    }

    // גטרים לשדות הלקוח
    public void setId(String id) { this.id = id; }
    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }

    // חישוב מחיר סופי בהתאם לסוג הלקוח והמבצע שלו
    public abstract double calculateFinalPrice(double originalPrice);
    public abstract String getCustomerType();

    @Override
    public boolean equals(Object o) {
        // בדיקה אם האובייקט הנוכחי הוא אותו אובייקט כמו האובייקט המועבר, ואם לא, בדיקה אם הוא מאותו סוג והשוואת מזהה הלקוח
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        // החזרת קוד hash מבוסס על מזהה הלקוח, משמש לאחסון האובייקט במבני נתונים כמו HashMap או HashSet
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        // החזרת מחרוזת שמייצגת את האובייקט, כולל סוג הלקוח, מזהה, שם מלא ומספר טלפון
        return String.format("[%s] ID: %s | Name: %s | Phone: %s", 
                getCustomerType(), id, fullName, phone);
    }
}