package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import exceptions.AuthenticationException;
import exceptions.DuplicateLoginException;
import exceptions.OutOfStockException;
import model.Branch;
import model.Product;
import model.SaleRecord;
import model.User;
import model.customers.Customer;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final StoreServer server;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;

    public ClientHandler(Socket socket, StoreServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("WELCOME תוכנת ניהול רשת חנויות בגדים");
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equalsIgnoreCase("EXIT") || line.equalsIgnoreCase("QUIT")) {
                    break;
                }
                processCommand(line);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected unexpectedly: " + 
                    (currentUser != null ? currentUser.getEmployeeId() : socket.getRemoteSocketAddress()));
        } finally {
            cleanup();
        }
    }

    private void processCommand(String commandLine) {
        String[] parts = commandLine.split("::");
        String action = parts[0];

        try {
            switch (action) {
                case "LOGIN":
                    handleLogin(parts[1], parts[2]);
                    break;
                case "GET_INVENTORY":
                    handleGetInventory();
                    break;
                case "BUY":
                    handleBuy(parts[1], parts[2], Integer.parseInt(parts[3]));
                    break;
                case "GET_CUSTOMERS":
                    handleGetCustomers();
                    break;
                case "REPORT_JSON":
                    handleReportJson(parts.length > 1 ? parts[1] : "ALL");
                    break;
                case "REPORT_WORD":
                    handleReportWord(parts.length > 1 ? parts[1] : "ALL");
                    break;
                case "CHAT_REQUEST":
                    handleChatRequest(parts[1]); 
                    break;
                case "CHAT_MSG":
                    handleChatMessage(parts[1]);
                    break;
                default:
                    out.println("ERROR::פקודה לא מזוהה");
            }
        } catch (Exception e) {
            out.println("ERROR::" + e.getMessage());
        }
    }

    private void handleLogin(String empId, String pass) {
        try {
            User user = StoreDataManager.getInstance().authenticate(empId, pass);
            SessionManager.getInstance().login(empId);
            this.currentUser = user;
            out.println("LOGIN_SUCCESS::" + user.getFullName() + "::" + user.getRole().name() + "::" + user.getBranchId());
            server.broadcastUserStatus(user.getEmployeeId(), true);
        } catch (AuthenticationException | DuplicateLoginException e) {
            out.println("ERROR::" + e.getMessage());
        }
    }

    private void handleGetInventory() {
        if (currentUser == null) { 
            out.println("ERROR::משתמש לא מחובר"); 
            return; 
        }
        Branch branch = StoreDataManager.getInstance().getBranches().get(currentUser.getBranchId());
        Map<Product, Integer> inv = branch.getInventorySnapshot();
        StringBuilder sb = new StringBuilder("INVENTORY_DATA::");
        
        for (Map.Entry<Product, Integer> entry : inv.entrySet()) {
            Product prod = entry.getKey();
            Integer qty = entry.getValue();
            sb.append(prod.getId()).append(",")
              .append(prod.getName()).append(",")
              .append(prod.getBasePrice()).append(",")
              .append(qty).append(";");
        }
        out.println(sb.toString());
    }

    private void handleBuy(String custId, String prodId, int qty) {
        if (currentUser == null) { 
            out.println("ERROR::משתמש לא מחובר"); 
            return; 
        }
        try {
            SaleRecord sale = StoreDataManager.getInstance().processPurchase(
                    currentUser.getBranchId(), currentUser.getEmployeeId(), custId, prodId, qty);
            out.println("BUY_SUCCESS::" + sale.getTransactionId() + "::" + sale.getFinalPrice());
            server.broadcastInventoryUpdate(currentUser.getBranchId());
        } catch (OutOfStockException | IllegalArgumentException e) {
            out.println("ERROR::" + e.getMessage());
        }
    }

    private void handleGetCustomers() {
        StringBuilder sb = new StringBuilder("CUSTOMERS_DATA::");
        for (Customer c : StoreDataManager.getInstance().getCustomers().values()) {
            sb.append(c.getId()).append(",")
              .append(c.getFullName()).append(",")
              .append(c.getPhone()).append(",")
              .append(c.getCustomerType()).append(";");
        }
        out.println(sb.toString());
    }

    private void handleReportJson(String filterBranch) {
        List<SaleRecord> records = StoreDataManager.getInstance().getSalesHistory();
        if (!filterBranch.equals("ALL")) {
            records = records.stream()
                             .filter(r -> r.getBranchId().equals(filterBranch))
                             .collect(Collectors.toList());
        }
        String json = ReportGenerator.generateSalesJson(records);
        out.println("REPORT_JSON_DATA::" + json.replace("\n", " "));
    }

    private void handleReportWord(String filterBranch) {
    List<SaleRecord> records = StoreDataManager.getInstance().getSalesHistory();
    if (!filterBranch.equals("ALL")) {
        records = records.stream()
                         .filter(r -> r.getBranchId().equals(filterBranch))
                         .collect(Collectors.toList());
    }
    try {
        // נייצר שם קובץ ייחודי לפי הזמן הנוכחי כדי לא לדרוס קבצים ישנים
        String fileName = "Sales_Report_" + System.currentTimeMillis() + ".doc";
        ReportGenerator.exportToWordDoc(fileName, "דוח מכירות - רשת חנויות בגדים", records);
        out.println("REPORT_WORD_SUCCESS::" + fileName);
    } catch (IOException e) {
        out.println("ERROR::שגיאה ביצירת מסמך: " + e.getMessage());
    }
}
    private void handleChatRequest(String targetBranchId) {
        if (currentUser == null) return;
        ClientHandler freeAgent = server.findAvailableUserInBranch(targetBranchId, currentUser.getEmployeeId());
        if (freeAgent != null) {
            ChatManager.getInstance().startChat(currentUser.getEmployeeId(), freeAgent.currentUser.getEmployeeId());
            out.println("CHAT_STARTED::" + freeAgent.currentUser.getFullName());
            freeAgent.out.println("CHAT_STARTED::" + currentUser.getFullName());
        } else {
            ChatManager.getInstance().registerMissedRequest(currentUser.getEmployeeId(), targetBranchId);
            out.println("CHAT_QUEUED::אין נציג פנוי בסניף המבוקש. פנייתך נרשמה בתור.");
        }
    }

    private void handleChatMessage(String msg) {
        if (currentUser == null) return;
        LoggerService.getInstance().log(LoggerService.LogType.CHAT, currentUser.getFullName() + ": " + msg);
        server.routeChatMessage(currentUser.getEmployeeId(), msg);
    }

    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    public User getCurrentUser() { return currentUser; }

    private void cleanup() {
        if (currentUser != null) {
            SessionManager.getInstance().logout(currentUser.getEmployeeId());
            ChatManager.getInstance().endChat(currentUser.getEmployeeId());
            server.broadcastUserStatus(currentUser.getEmployeeId(), false);
        }
        server.removeClient(this);
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}