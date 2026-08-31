package patterns.observer;

// Interface representing an observer in the observer pattern, allowing it to receive notifications about changes in the store's state.
public interface StoreObserver {
    // Method to be called when the inventory of a specific branch changes
    void onInventoryChanged(String branchId);
    void onCustomerListChanged();
    void onUserStatusChanged(String employeeId, boolean isOnline);
}