package server;

import exceptions.DuplicateLoginException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static SessionManager instance;

    // Map to keep track of active sessions, mapping user IDs to their corresponding ClientHandler instances
    private final Map<String, ClientHandler> activeSessions = new ConcurrentHashMap<>();

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    //Function to handle user login, ensuring that a user cannot log in from multiple devices simultaneously. If a duplicate login attempt is detected, a DuplicateLoginException is thrown.
    public synchronized void login(String userId, ClientHandler handler) throws DuplicateLoginException {
        if (activeSessions.containsKey(userId)) {
            throw new DuplicateLoginException("User " + userId + " is already logged in from another device!");
        }
        activeSessions.put(userId, handler);
    }

    public synchronized void logout(String userId) {
        // Removing the user's session from the active sessions map upon logout, allowing them to log in again from another device if desired.
        activeSessions.remove(userId);
    }

    public synchronized boolean isUserLoggedIn(String userId) {
        // Checking if a user is currently logged in by verifying their presence in the active sessions map
        return activeSessions.containsKey(userId);
    }

    // Function to retrieve the ClientHandler instance for a logged-in user by their ID
    public synchronized ClientHandler getClientHandler(String userId) {
        return activeSessions.get(userId);
    }
    // Function to retrieve all active sessions
    public synchronized Map<String, ClientHandler> getActiveSessions() {
        return activeSessions;
    }
    // Function to retrieve the ClientHandler instance for a logged-in user by their ID
    public synchronized ClientHandler getHandler(String userId) {
        return activeSessions.get(userId);
    }
}