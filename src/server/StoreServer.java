package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import patterns.observer.StoreObserver;
import patterns.observer.StoreSubject;

// StoreServer class responsible for managing client connections and for telling every connected
// client about changes in the data of the store.
// It plays the role of the subject in the Observer pattern: each connected ClientHandler registers
// itself as an observer, and the server only announces that something changed, without knowing
// what each client decides to do with the announcement.
public class StoreServer implements StoreSubject {
    // Port number on which the server listens for incoming client connections
    public static final int PORT = 7000;
    // The connected clients, which are also the observers of the server
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<ClientHandler>());
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private boolean isRunning = true; // Flag to control the running state of the server

    public void start() {
        // Notification about the server startup
        System.out.println("Store Management Server is starting on port " + PORT + "...");

        // Opening the database before the first client connects, so that a missing or a damaged
        // data file is reported here and not in the middle of serving somebody
        try {
            StoreDataManager.getInstance();
        } catch (RuntimeException e) {
            System.err.println("Could not open the database: " + e.getMessage());
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (isRunning) {
                // Accepting a new client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from " + clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                registerObserver(handler);
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

    // Searching for an employee of the requested branch who is connected and is not busy in a chat.
    // Returning null when every employee of that branch is busy or nobody from it is connected
    public ClientHandler findAvailableUserInBranch(String branchId, String excludeUserId) {
        // The iteration over a synchronized collection has to sit inside a synchronized block
        synchronized (clients) {
            for (ClientHandler ch : clients) {
                if (ch.getCurrentUser() != null
                        && ch.getCurrentUser().getBranchId().equals(branchId)
                        && !ch.getCurrentUser().getEmployeeId().equals(excludeUserId)
                        && !AdvancedChatMediator.getInstance().isUserInChat(ch.getCurrentUser().getEmployeeId())) {
                    return ch;
                }
            }
        }
        return null;
    }

    // Registering a client as an observer of the store
    @Override
    public void registerObserver(StoreObserver observer) {
        if (observer instanceof ClientHandler) {
            clients.add((ClientHandler) observer);
        }
    }

    // Removing a client from the observers of the store
    @Override
    public void removeObserver(StoreObserver observer) {
        clients.remove(observer);
    }

    // Announcing that the inventory of a branch has changed
    @Override
    public void notifyInventoryChanged(String branchId) {
        synchronized (clients) {
            for (ClientHandler ch : clients) {
                ch.onInventoryChanged(branchId);
            }
        }
    }

    // Announcing that the customer list of the network has changed
    @Override
    public void notifyCustomerListChanged() {
        synchronized (clients) {
            for (ClientHandler ch : clients) {
                ch.onCustomerListChanged();
            }
        }
    }

    // Announcing that an employee has connected to the system or left it
    @Override
    public void notifyUserStatusChanged(String employeeId, boolean isOnline) {
        synchronized (clients) {
            for (ClientHandler ch : clients) {
                ch.onUserStatusChanged(employeeId, isOnline);
            }
        }
    }

    public static void main(String[] args) {
        // Starting the StoreServer
        new StoreServer().start();
    }
}
