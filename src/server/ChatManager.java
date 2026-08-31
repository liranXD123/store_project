package server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

// Class responsible for managing chat sessions, including tracking busy users, missed chat requests, and active chats between users.
public class ChatManager {
    private static ChatManager instance;

    // Set to keep track of users who are currently busy in a chat
    private final Set<String> busyUsers = Collections.synchronizedSet(new HashSet<>());
    // Queue to keep track of missed chat requests (requests that were not answered)
    private final Queue<ChatRequest> missedCallQueue = new ConcurrentLinkedQueue<>();
    // Map to keep track of active chats between users
    private final Map<String, String> activeChats = new ConcurrentHashMap<>();

    public static class ChatRequest {
        public final String requesterId;
        public final String targetBranchId;
        public final long timestamp;

        public ChatRequest(String requesterId, String targetBranchId) {
            // Saving the ID of the user who requested the chat, the ID of the branch they are contacting, and the time the request was created
            this.requesterId = requesterId;
            this.targetBranchId = targetBranchId;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private ChatManager() {}

    public static synchronized ChatManager getInstance() {
        // Ensuring that only one instance of the ChatManager class is created (Singleton pattern)
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }

    public synchronized boolean startChat(String userA, String userB) {
        // Starting a chat between two users, checking if either user is already busy, and if not, marking them as busy and adding them to the active chats map
        if (busyUsers.contains(userA) || busyUsers.contains(userB)) {
            return false;
        }
        busyUsers.add(userA);
        busyUsers.add(userB);
        activeChats.put(userA, userB);
        activeChats.put(userB, userA);
        return true;
    }

    public synchronized void endChat(String userA) {
        // Ending a chat for a specific user, removing them from the list of busy users and preventing further communication with the other user
        String userB = activeChats.remove(userA);
        if (userB != null) {
            activeChats.remove(userB);
            busyUsers.remove(userB);
        }
        busyUsers.remove(userA);
    }

    public void registerMissedRequest(String requesterId, String targetBranchId) {
        // Registering a missed chat request, adding it to the queue of pending requests
        missedCallQueue.add(new ChatRequest(requesterId, targetBranchId));
    }

    public List<ChatRequest> getAndClearPendingRequestsForBranch(String branchId) {
        // Retrieving all pending chat requests for a specific branch and removing them from the queue
        List<ChatRequest> pending = new ArrayList<>();
        Iterator<ChatRequest> it = missedCallQueue.iterator();
        while (it.hasNext()) {
            ChatRequest req = it.next();
            if (req.targetBranchId.equals(branchId)) {
                pending.add(req);
                it.remove();
            }
        }
        return pending;
    }

    public boolean isUserBusy(String userId) {
        // Checking if the user is in a busy state (in the middle of a chat)
        return busyUsers.contains(userId);
    }
}