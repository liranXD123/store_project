package server;

import exceptions.DuplicateLoginException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static SessionManager instance;
    
    // שינינו ל-Map: עכשיו שומרים גם את מזהה העובד וגם את אובייקט התקשורת שלו
    private final Map<String, ClientHandler> activeSessions = new ConcurrentHashMap<>();

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    // עדכנו את הפונקציה כך שתקבל גם את ה-handler של מי שמתחבר
    public synchronized void login(String userId, ClientHandler handler) throws DuplicateLoginException {
        if (activeSessions.containsKey(userId)) {
            throw new DuplicateLoginException("User " + userId + " is already logged in from another device!");
        }
        activeSessions.put(userId, handler);
    }

    public synchronized void logout(String userId) {
        // הסרת המשתמש מהרשימה של המשתמשים המחוברים
        activeSessions.remove(userId);
    }

    public synchronized boolean isUserLoggedIn(String userId) {
        // בדיקה אם המשתמש מחובר כרגע
        return activeSessions.containsKey(userId);
    }

    // פונקציה חדשה שמחזירה את ה-handler של המשתמש המחובר לפי מזהה העובד
    public synchronized ClientHandler getClientHandler(String userId) {
        return activeSessions.get(userId);
    }
    // פונקציה חדשה שמחזירה את כל המשתמשים המחוברים
    public synchronized Map<String, ClientHandler> getActiveSessions() {
        return activeSessions;
    }
    // פונקציה חדשה שמחזירה את ה-handler של המשתמש לפי מזהה העובד   
    public synchronized ClientHandler getHandler(String userId) {
        return activeSessions.get(userId);
    }
}