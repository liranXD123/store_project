package server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Mediator of all the chats in the system. No employee talks to another employee directly:
// everybody asks this single object to open a chat, to join one or to leave one, and it is
// the only place that knows who is talking with whom.
// It also keeps, for every branch, the list of employees that asked for a chat while all the
// employees of that branch were busy, so they can be called back once somebody becomes free.
public class AdvancedChatMediator {
    private static AdvancedChatMediator instance;

    // Map holding the participants of every active chat room
    private final Map<String, Set<String>> activeRooms = new HashMap<String, Set<String>>();
    // Map telling, for every employee, the room they are currently sitting in
    private final Map<String, String> userToRoom = new HashMap<String, String>();
    // Map holding, for every branch, the employees waiting for a free employee of that branch
    private final Map<String, List<String>> branchWaitingQueues = new HashMap<String, List<String>>();

    private AdvancedChatMediator() {
    }

    public static synchronized AdvancedChatMediator getInstance() {
        // Ensuring that only one instance of the AdvancedChatMediator class is created (Singleton pattern)
        if (instance == null) {
            instance = new AdvancedChatMediator();
        }
        return instance;
    }

    // Opening a chat room between two employees.
    // Returning the ID of the new room, or null when one of them is already inside another chat
    public synchronized String createOneOnOneChat(String userA, String userB) {
        if (userToRoom.containsKey(userA) || userToRoom.containsKey(userB)) {
            return null;
        }
        String roomId = "ROOM_" + UUID.randomUUID().toString().substring(0, 6);
        Set<String> participants = new HashSet<String>();
        participants.add(userA);
        participants.add(userB);

        activeRooms.put(roomId, participants);
        userToRoom.put(userA, roomId);
        userToRoom.put(userB, roomId);

        return roomId;
    }

    // Adding a manager into the room of one of the employees, so they can follow the conversation
    public synchronized boolean joinChatAsManager(String managerId, String targetUserId) {
        // Checking if the requested employee is inside a chat room at all
        String roomId = userToRoom.get(targetUserId);
        if (roomId == null)
            return false;

        Set<String> participants = activeRooms.get(roomId);
        if (participants != null) {
            participants.add(managerId);
            userToRoom.put(managerId, roomId);
            return true;
        }
        return false;
    }

    // Removing an employee from their chat room, and closing the room when nobody is left to talk to
    public synchronized void leaveChat(String userId) {
        String roomId = userToRoom.remove(userId);
        if (roomId == null)
            return;

        Set<String> participants = activeRooms.get(roomId);
        if (participants == null)
            return;

        participants.remove(userId);
        // A room with a single participant left is no longer a conversation, so it is closed
        if (participants.size() <= 1) {
            for (String remaining : participants) {
                userToRoom.remove(remaining);
            }
            activeRooms.remove(roomId);
        }
    }

    // Returning the employees that share the chat room of the given employee
    public synchronized Set<String> getRoomParticipants(String userId) {
        String roomId = userToRoom.get(userId);
        if (roomId != null && activeRooms.containsKey(roomId)) {
            return new HashSet<String>(activeRooms.get(roomId));
        }
        return Collections.emptySet();
    }

    // Checking whether the employee is currently inside a chat room
    public synchronized boolean isUserInChat(String userId) {
        return userToRoom.containsKey(userId);
    }

    // Returning every employee that is currently sitting in some chat room,
    // so a manager can be shown which conversations are open
    public synchronized List<String> getUsersInChat() {
        return new ArrayList<String>(userToRoom.keySet());
    }

    // Remembering an employee that asked to talk to a branch in which nobody was free.
    // The same employee is not written into the queue of the same branch twice.
    public synchronized void addToBranchQueue(String branchId, String requesterId) {
        List<String> queue = branchWaitingQueues.get(branchId);
        if (queue == null) {
            queue = new LinkedList<String>();
            branchWaitingQueues.put(branchId, queue);
        }
        if (!queue.contains(requesterId)) {
            queue.add(requesterId);
        }
    }

    // Taking the employee that has been waiting the longest for the given branch.
    // Returning null when nobody is waiting for that branch
    public synchronized String pollBranchQueue(String branchId) {
        List<String> queue = branchWaitingQueues.get(branchId);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        return queue.remove(0);
    }

    // Removing an employee from every waiting queue, used when they disconnect
    public synchronized void removeFromAllQueues(String userId) {
        for (List<String> queue : branchWaitingQueues.values()) {
            queue.remove(userId);
        }
    }
}
