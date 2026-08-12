package model;

public enum Role {
    ADMIN("Admin"),
    SHIFT_MANAGER("Shift Manager"),
    CASHIER("Cashier"),
    SELLER("Seller");

    private final String title;

    Role(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}