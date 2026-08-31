package client;

import java.io.PrintWriter;

// Class holding the state of a single client connection: the channel towards the server
// and the details of the user that is currently logged in through this client.
// The listening thread updates this state while the main thread reads it,
// therefore every field is reached only through synchronized methods.
public class ClientSession {
    // Roles used by the system for deciding what a user is allowed to do
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SHIFT_MANAGER = "SHIFT_MANAGER";

    // Session fields
    private PrintWriter out;
    private boolean loggedIn;
    private String fullName;
    private String role;
    private String branchId;
    private boolean inChatMode;
    private boolean running;

    // Constructor for the ClientSession class, receiving the stream on which commands are sent to the server
    public ClientSession(PrintWriter out) {
        this.out = out;
        this.loggedIn = false;
        this.fullName = "";
        this.role = "";
        this.branchId = "";
        this.inChatMode = false;
        this.running = true;
    }

    // Sending a command to the server
    public synchronized void send(String command) {
        out.println(command);
    }

    // Saving the details of the user after a successful login
    public synchronized void login(String fullName, String role, String branchId) {
        this.loggedIn = true;
        this.fullName = fullName;
        this.role = role;
        this.branchId = branchId;
    }

    // Clearing the details of the user when the session ends
    public synchronized void logout() {
        this.loggedIn = false;
        this.fullName = "";
        this.role = "";
        this.branchId = "";
    }

    // Getters and setters for the session fields
    public synchronized boolean isLoggedIn() {
        return loggedIn;
    }

    public synchronized String getFullName() {
        return fullName;
    }

    public synchronized String getRole() {
        return role;
    }

    public synchronized String getBranchId() {
        return branchId;
    }

    public synchronized boolean isInChatMode() {
        return inChatMode;
    }

    public synchronized void setInChatMode(boolean inChatMode) {
        this.inChatMode = inChatMode;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    // Asking the main loop of the client to stop
    public synchronized void stop() {
        this.running = false;
    }
}
