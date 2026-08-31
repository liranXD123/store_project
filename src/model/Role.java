package model;

// Enum representing different roles in the store system, each with a specific title and associated permissions.
public enum Role {
    // Different roles in the system, each representing a different level of permissions
    ADMIN("Admin"),
    SHIFT_MANAGER("Shift Manager"),
    CASHIER("Cashier"),
    SELLER("Seller");

    private final String title;

    Role(String title) {
        // Saving the title of the role
        this.title = title;
    }

    public String getTitle() {
        // Returning the title of the role
        return title;
    }
}