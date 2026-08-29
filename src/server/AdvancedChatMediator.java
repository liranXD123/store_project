package server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AdvancedChatMediator {
    private static AdvancedChatMediator instance;

    // מיפוי של חדרי שיחה פעילים (roomId -> set of userIds)
    private final Map<String, Set<String>> activeRooms = new ConcurrentHashMap<>();
    private final Map<String, String> userToRoom = new ConcurrentHashMap<>();
    private final Map<String, Queue<String>> waitingQueues = new ConcurrentHashMap<>();

    private AdvancedChatMediator() {}

    public static synchronized AdvancedChatMediator getInstance() {
        //  יצירת מופע יחיד של AdvancedChatMediator (Singleton Pattern)
        if (instance == null) {
            instance = new AdvancedChatMediator();
        }
        return instance;
    }

    public synchronized String requestChat(String requesterId, String targetId) {
        // בדיקה אם המשתמש שאליו מבקשים את השיחה כבר נמצא בשיחה אחרת
        if (isUserInChat(targetId)) {
            waitingQueues.computeIfAbsent(targetId, k -> new ConcurrentLinkedQueue<>()).add(requesterId);
            return "QUEUED"; 
        }
        return createOneOnOneChat(requesterId, targetId);
    }

    private synchronized String createOneOnOneChat(String userA, String userB) {
        // בדיקה אם אחד המשתמשים כבר נמצא בחדר שיחה אחר
        if (userToRoom.containsKey(userA) || userToRoom.containsKey(userB)) {
            return null; 
        }
        String roomId = "ROOM_" + UUID.randomUUID().toString().substring(0, 6);
        Set<String> participants = Collections.synchronizedSet(new HashSet<>());
        participants.add(userA);
        participants.add(userB);

        activeRooms.put(roomId, participants);
        userToRoom.put(userA, roomId);
        userToRoom.put(userB, roomId);

        return roomId;
    }

    public synchronized boolean joinChatAsManager(String managerId, String targetUserId) {
        // בדיקה אם המשתמש שאליו מבקשים להצטרף כבר נמצא בחדר שיחה
        String roomId = userToRoom.get(targetUserId);
        if (roomId == null) return false;

        Set<String> participants = activeRooms.get(roomId);
        if (participants != null) {
            participants.add(managerId);
            userToRoom.put(managerId, roomId);
            return true;
        }
        return false;
    }

    public synchronized void leaveChat(String userId) {
        // הסרת המשתמש מהרשימה של המשתמשים בחדר השיחה, ובדיקת התור של המשתמשים שממתינים לשיחה
        String roomId = userToRoom.remove(userId);
        if (roomId != null) {
            Set<String> participants = activeRooms.get(roomId);
            if (participants != null) {
                participants.remove(userId);
                checkQueueAndNotify(userId); 

                if (participants.size() <= 1) {
                    for (String remaining : participants) {
                        userToRoom.remove(remaining);
                        checkQueueAndNotify(remaining); 
                    }
                    activeRooms.remove(roomId);
                }
            }
        }
    }

    private void checkQueueAndNotify(String freedUserId) {
        // בדיקה אם יש משתמשים שממתינים לשיחה עם המשתמש ששוחרר, והודעה למשתמשים המתאימים
        Queue<String> queue = waitingQueues.get(freedUserId);
        if (queue != null && !queue.isEmpty()) {
            String waitingUser = queue.poll(); 
            
            ClientHandler handler = SessionManager.getInstance().getHandler(freedUserId);
            if (handler != null) {
                handler.sendMessage("CHAT_QUEUED::User " + waitingUser + " requested a chat while you were busy. You can now start a chat with them.");
            }
        }
    }

    public Set<String> getRoomParticipants(String userId) {
        // החזרת רשימת המשתמשים בחדר השיחה של המשתמש המבוקש, אם הוא נמצא בחדר כזה
        String roomId = userToRoom.get(userId);
        if (roomId != null && activeRooms.containsKey(roomId)) {
            return Collections.unmodifiableSet(activeRooms.get(roomId));
        }
        return Collections.emptySet();
    }

    public boolean isUserInChat(String userId) {
        // בדיקה אם המשתמש נמצא בחדר שיחה כלשהו
        return userToRoom.containsKey(userId);
    }
}