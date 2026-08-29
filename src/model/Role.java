package model;

public enum Role {
    // תפקידים שונים במערכת, כל תפקיד מייצג רמת הרשאות שונה
    ADMIN("Admin"),
    SHIFT_MANAGER("Shift Manager"),
    CASHIER("Cashier"),
    SELLER("Seller");

    private final String title;

    Role(String title) {
        // שמירת הכותרת של התפקיד
        this.title = title;
    }

    public String getTitle() {
        // החזרת הכותרת של התפקיד
        return title;
    }
}