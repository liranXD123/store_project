package server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

// Class responsible for mediating advanced chat functionalities, including managing chat rooms, participants, and waiting queues for users attempting to initiate chats with busy users.
public class AdvancedChatMediator {
    private static AdvancedChatMediator instance;

    // Map to keep track of active chat rooms and their participants
    private final Map<String, Set<String>> activeRooms = new ConcurrentHashMap<>();
    private final Map<String, String> userToRoom = new ConcurrentHashMap<>();
    private final Map<String, Queue<String>> waitingQueues = new ConcurrentHashMap<>();

    private AdvancedChatMediator() {}

    public static synchronized AdvancedChatMediator getInstance() {
        // Ensuring that only one instance of the AdvancedChatMediator class is created (Singleton pattern)
        if (instance == null) {
            instance = new AdvancedChatMediator();
        }
        return instance;
    }

    public synchronized String requestChat(String requesterId, String targetId) {
        // Checking if the user to whom the chat request is sent is already in another chat
        if (isUserInChat(targetId)) {
            waitingQueues.computeIfAbsent(targetId, k -> new ConcurrentLinkedQueue<>()).add(requesterId);
            return "QUEUED"; 
        }
        return createOneOnOneChat(requesterId, targetId);
    }

    private synchronized String createOneOnOneChat(String userA, String userB) {
        // Checking if either user is already in another chat room
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
        // Checking if the user to whom the request is sent is already in a chat room
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
        // Removing the user from the list of participants in the chat room, and checking the queue of users waiting for a chat
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
        // Checking if there are users waiting to chat with the freed user, and notifying the appropriate users
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
        // Returning the list of users in the chat room of the requested user, if they are in such a room
        String roomId = userToRoom.get(userId);
        if (roomId != null && activeRooms.containsKey(roomId)) {
            return Collections.unmodifiableSet(activeRooms.get(roomId));
        }
        return Collections.emptySet();
    }

    public boolean isUserInChat(String userId) {
        // Checking if the user is in any chat room
        return userToRoom.containsKey(userId);
    }
}