package model;

import java.io.Serializable;
import java.util.Objects;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L; // מזהה ייחודי לגרסה של המחלקה, משמש בעת סריאליזציה כדי לוודא שהגרסה של המחלקה תואמת לגרסה של האובייקט המוסר

    // שדות המוצר   
    private String id;
    private String name;
    private String category;
    private double basePrice;

    public Product(String id, String name, String category, double basePrice) {
        // בנאי למחלקת Product שמקבל את כל השדות הנדרשים ליצירת אובייקט מוצר חדש
        this.id = id;
        this.name = name;
        this.category = category;
        this.basePrice = basePrice;
    }

    // גטרים לשדות המוצר
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getBasePrice() { return basePrice; }

    @Override
    public boolean equals(Object o) {
        // בדיקה אם האובייקט הנוכחי הוא אותו אובייקט כמו האובייקט המועבר, ואם לא, בדיקה אם הוא מאותו סוג והשוואת מזהה המוצר
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        // החזרת קוד hash מבוסס על מזהה המוצר, משמש לאחסון האובייקט במבני נתונים כמו HashMap או HashSet
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        // החזרת מחרוזת שמייצגת את האובייקט, כולל מזהה המוצר, שם, קטגוריה ומחיר בסיס    
        return String.format("Product[%s - %s (%s) - ₪%.2f]", id, name, category, basePrice);
    }
}