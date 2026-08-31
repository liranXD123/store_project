package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// StoreServer class responsible for managing client connections, routing messages, and broadcasting updates in the store management system. It handles incoming client connections, manages active clients, and provides methods for routing chat messages and broadcasting inventory updates.
public class StoreServer {
    // Port number on which the server listens for incoming client connections
    public static final int PORT = 7000;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private boolean isRunning = true; // Flag to control the running state of the server

    public void start() {
        // Notification about the server startup
        System.out.println("Store Management Server is starting on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (isRunning) {
                // Accepting a new client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from " + clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            // Displaying an error message in case of an exception
            System.err.println("Server exception: " + e.getMessage());
        } finally {
            // Closing the thread pool when the server shuts down
            threadPool.shutdown();
        }
    }

    public void removeClient(ClientHandler handler) {
        // Removing the client from the list when they disconnect
        clients.remove(handler);
    }

    public ClientHandler findAvailableUserInBranch(String branchId, String excludeUserId) {
        // Searching for an available user in a specific branch, excluding a specific user
        for (ClientHandler ch : clients) {
            if (ch.getCurrentUser() != null
                    && ch.getCurrentUser().getBranchId().equals(branchId)
                    && !ch.getCurrentUser().getEmployeeId().equals(excludeUserId)
                    && !ChatManager.getInstance().isUserBusy(ch.getCurrentUser().getEmployeeId())) {
                return ch;
            }
        }
        return null;
    }

    public void routeChatMessage(String senderId, String message) {
        // Sending a chat message to all connected users, excluding the sender
        for (ClientHandler ch : clients) {
            if (ch.getCurrentUser() != null && !ch.getCurrentUser().getEmployeeId().equals(senderId)) {
                ch.sendMessage("CHAT_INCOMING::" + senderId + "::" + message);
            }
        }
    }

    public void broadcastInventoryUpdate(String branchId) {
        // Sending a message to all clients in a specific branch about an inventory update
        for (ClientHandler ch : clients) {
            if (ch.getCurrentUser() != null && ch.getCurrentUser().getBranchId().equals(branchId)) {
                ch.sendMessage("INVENTORY_UPDATED");
            }
        }
    }

    public void broadcastUserStatus(String employeeId, boolean isOnline) {
        // Sending a message to all clients about a user's online/offline status
        String msg = "USER_STATUS::" + employeeId + "::" + (isOnline ? "ONLINE" : "OFFLINE");
        for (ClientHandler ch : clients) {
            ch.sendMessage(msg);
        }
    }

    public static void main(String[] args) {
        // Starting the StoreServer
        new StoreServer().start();
    }
}