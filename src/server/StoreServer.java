package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreServer {
    // הגדרת פורט ברירת מחדל לשרת
    public static final int PORT = 7000;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private boolean isRunning = true; // דגל לשליטה על מצב הריצה של השרת

    public void start() {
        // הודעה על התחלת השרת
        System.out.println("Store Management Server is starting on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (isRunning) {
                // קבלת חיבור חדש מלקוח
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from " + clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            // במקרה של חריגה, נציג הודעת שגיאה
            System.err.println("Server exception: " + e.getMessage());
        } finally {
            // סגירת ה-thread pool בסיום
            threadPool.shutdown();
        }
    }

    public void removeClient(ClientHandler handler) {
        // הסרת הלקוח מהרשימה כאשר הוא מתנתק
        clients.remove(handler);
    }

    public ClientHandler findAvailableUserInBranch(String branchId, String excludeUserId) {
        // חיפוש משתמש פנוי בסניף מסוים, תוך התעלמות ממשתמש מסוים
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
        // שליחת הודעת צ'אט לכל המשתמשים המחוברים, למעט השולח
        for (ClientHandler ch : clients) {
            if (ch.getCurrentUser() != null && !ch.getCurrentUser().getEmployeeId().equals(senderId)) {
                ch.sendMessage("CHAT_INCOMING::" + senderId + "::" + message);
            }
        }
    }

    public void broadcastInventoryUpdate(String branchId) {
        // שליחת הודעה לכל המשתמשים בסניף מסוים על עדכון מלאי
        for (ClientHandler ch : clients) {
            if (ch.getCurrentUser() != null && ch.getCurrentUser().getBranchId().equals(branchId)) {
                ch.sendMessage("INVENTORY_UPDATED");
            }
        }
    }

    public void broadcastUserStatus(String employeeId, boolean isOnline) {
        // שליחת הודעה לכל המשתמשים על מצב המשתמש (מחובר/מנותק)
        String msg = "USER_STATUS::" + employeeId + "::" + (isOnline ? "ONLINE" : "OFFLINE");
        for (ClientHandler ch : clients) {
            ch.sendMessage(msg);
        }
    }

    public static void main(String[] args) {
        // הפעלת השרת
        new StoreServer().start();
    }
}