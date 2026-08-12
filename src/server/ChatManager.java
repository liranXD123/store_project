package server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChatManager {
    private static ChatManager instance;

    // עובדים במצב פעיל (עסוקים בשיחה)
    private final Set<String> busyUsers = Collections.synchronizedSet(new HashSet<>());
    // תור משתמשים שלא קיבלו מענה וממתינים לשיחה חוזרת
    private final Queue<ChatRequest> missedCallQueue = new ConcurrentLinkedQueue<>();
    // מיפוי של שיחות פעילות (Client1 -> Client2)
    private final Map<String, String> activeChats = new ConcurrentHashMap<>();

    public static class ChatRequest {
        public final String requesterId;
        public final String targetBranchId;
        public final long timestamp;

        public ChatRequest(String requesterId, String targetBranchId) {
            this.requesterId = requesterId;
            this.targetBranchId = targetBranchId;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private ChatManager() {}

    public static synchronized ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }

    public synchronized boolean startChat(String userA, String userB) {
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
        String userB = activeChats.remove(userA);
        if (userB != null) {
            activeChats.remove(userB);
            busyUsers.remove(userB);
        }
        busyUsers.remove(userA);
    }

    public void registerMissedRequest(String requesterId, String targetBranchId) {
        missedCallQueue.add(new ChatRequest(requesterId, targetBranchId));
    }

    public List<ChatRequest> getAndClearPendingRequestsForBranch(String branchId) {
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
        return busyUsers.contains(userId);
    }
}