package patterns.observer;

public interface StoreSubject {
    //  פונקציות לניהול רשימת המאזינים (Observers) והודעות על שינויים
    void registerObserver(StoreObserver observer);
    void removeObserver(StoreObserver observer);
    void notifyInventoryChanged(String branchId);
    void notifyCustomerListChanged();
    void notifyUserStatusChanged(String employeeId, boolean isOnline);
}