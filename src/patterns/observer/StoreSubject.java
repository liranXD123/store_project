package patterns.observer;

// Interface representing a subject in the observer pattern, allowing observers to register, unregister, and receive notifications about changes in the store's state.
public interface StoreSubject {
    // Methods for managing observers and notifying them of changes in the store's state
    void registerObserver(StoreObserver observer);
    void removeObserver(StoreObserver observer);
    void notifyInventoryChanged(String branchId);
    void notifyCustomerListChanged();
    void notifyUserStatusChanged(String employeeId, boolean isOnline);
}