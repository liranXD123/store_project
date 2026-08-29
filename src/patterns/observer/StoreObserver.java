package patterns.observer;

public interface StoreObserver {
    //  פונקציות שמאזינים (Observers) צריכים לממש כדי לקבל עדכונים מהנושא (Subject)
    void onInventoryChanged(String branchId);
    void onCustomerListChanged();
    void onUserStatusChanged(String employeeId, boolean isOnline);
}