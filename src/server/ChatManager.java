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
            // שמירת מזהה המשתמש שביקש את השיחה, מזהה הסניף שאליו הוא פונה, והזמן שבו הבקשה נוצרה
            this.requesterId = requesterId;
            this.targetBranchId = targetBranchId;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private ChatManager() {}

    public static synchronized ChatManager getInstance() {
        // יצירת מופע יחיד של ChatManager (Singleton Pattern)
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }

    public synchronized boolean startChat(String userA, String userB) {
        // התחלת שיחה בין שני משתמשים, אם הם אינם עסוקים כבר בשיחה אחרת
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
        // סיום שיחה עבור משתמש מסוים, הסרתו מהרשימה של המשתמשים העסוקים ומניעת המשך השיחה עם המשתמש השני
        String userB = activeChats.remove(userA);
        if (userB != null) {
            activeChats.remove(userB);
            busyUsers.remove(userB);
        }
        busyUsers.remove(userA);
    }

    public void registerMissedRequest(String requesterId, String targetBranchId) {
        // רישום בקשה לשיחה שלא נענתה, הוספתה לתור המתנה
        missedCallQueue.add(new ChatRequest(requesterId, targetBranchId));
    }

    public List<ChatRequest> getAndClearPendingRequestsForBranch(String branchId) {
        // קבלת כל הבקשות לשיחות שלא נענו עבור סניף מסוים, והסרתן מהתור
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
        // בדיקה אם המשתמש נמצא במצב עסוק (במהלך שיחה)
        return busyUsers.contains(userId);
    }
}