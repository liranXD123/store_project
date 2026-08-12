package server;

import exceptions.DuplicateLoginException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SessionManager {
    private static SessionManager instance;
    private final Set<String> loggedInUserIds = Collections.synchronizedSet(new HashSet<>());

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public synchronized void login(String userId) throws DuplicateLoginException {
        if (loggedInUserIds.contains(userId)) {
            throw new DuplicateLoginException("User " + userId + " is already logged in from another device!");
        }
        loggedInUserIds.add(userId);
    }

    public synchronized void logout(String userId) {
        loggedInUserIds.remove(userId);
    }

    public synchronized boolean isUserLoggedIn(String userId) {
        return loggedInUserIds.contains(userId);
    }
}