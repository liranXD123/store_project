package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import exceptions.AuthenticationException;
import exceptions.DuplicateLoginException;
import exceptions.OutOfStockException;
import model.Branch;
import model.Product;
import model.SaleRecord;
import model.User;
import model.Role;
import model.customers.Customer;
import model.customers.NewCustomer;
import model.customers.ReturningCustomer;
import model.customers.VipCustomer;


// Class responsible for handling client connections and processing commands from clients in a multi-threaded environment.
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final StoreServer server;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;

    public ClientHandler(Socket socket, StoreServer server) {
        //store the socket and server reference for later use
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            //thread-safe initialization of input and output streams for client communication
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("WELCOME to the Clothing Store Management System");
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equalsIgnoreCase("EXIT") || line.equalsIgnoreCase("QUIT")) {
                    break;
                }
                processCommand(line);
            }
        } catch (IOException e) {
            // Handling unexpected client disconnection
            System.out.println("Client disconnected unexpectedly: " +
                    (currentUser != null ? currentUser.getEmployeeId() : socket.getRemoteSocketAddress()));
        } finally {
            cleanup();
        }
    }

    private void handleAddEmployee(String[] parts) {
        // Checking if there are enough details to add a new employee
        if (parts.length < 9) {
            out.println("ERROR::Missing employee details.");
            return;
        }
        String empId = parts[1];
        String name = parts[2];
        String tz = parts[3];
        String phone = parts[4];
        String bank = parts[5];
        String branch = parts[6];
        String roleStr = parts[7];
        String pwd = parts[8];

        if (pwd.length() < 6) {
            // Checking if the password meets the security policy (at least 6 characters)
            out.println("ERROR::Password policy violation - must be at least 6 characters.");
            return;
        }

        try {
            // Converting the role string to a Role object, handling invalid roles
            Role role = Role.valueOf(roleStr.toUpperCase());
            User newUser = new User(empId, name, tz, phone, bank, branch, role, pwd);
            boolean added = StoreDataManager.getInstance().addUser(newUser);
            if (added) {
                writeToSystemLog("Employee Registered: " + name + " (Role: " + roleStr + ") by Admin "
                        + currentUser.getEmployeeId());
                out.println("ADD_EMP_SUCCESS::" + name);
            } else {
                out.println("ERROR::Employee ID already exists.");
            }
        } catch (IllegalArgumentException e) {
            out.println("ERROR::Invalid Role specified. Use ADMIN, SHIFT_MANAGER, or CASHIER.");
        }
    }

    private void handleAddCustomer(String[] parts) {
        // Checking if there are enough details to add a new customer
        if (parts.length < 5) {
            out.println("ERROR::Missing customer details.");
            return;
        }
        String id = parts[1];
        String name = parts[2];
        String phone = parts[3];
        String typeStr = parts[4].toUpperCase();

        Customer newCustomer = null;
        switch (typeStr) { // Creating a new customer object based on the specified type (NEW, RETURNING, VIP)
            case "NEW":
                newCustomer = new NewCustomer(id, name, phone);
                break;
            case "RETURNING":
                newCustomer = new ReturningCustomer(id, name, phone);
                break;
            case "VIP":
                newCustomer = new VipCustomer(id, name, phone);
                break;
            default:
                out.println("ERROR::Invalid customer type. Use NEW, RETURNING, or VIP.");
                return;
        }

        boolean added = StoreDataManager.getInstance().addCustomer(newCustomer);
        if (added) {
            // Logging the customer registration action to the system log, including the employee who performed the action
            writeToSystemLog("Customer Registered: " + name + " (Type: " + typeStr + ") by employee "
                    + currentUser.getEmployeeId());
            out.println("ADD_CUSTOMER_SUCCESS::" + name);
        } else {
            // Logging the failed customer registration attempt to the system log
            writeToSystemLog("Failed to register customer: " + name + " (ID: " + id + ") by employee "
                    + currentUser.getEmployeeId());
            out.println("ERROR::Customer ID already exists.");
        }
    }

    private void writeToSystemLog(String action) { // Writing action to the system log
        try {
            // Creating the logs directory if it doesn't exist
            java.io.File logDir = new java.io.File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("logs/system.log", true))) {
                pw.println(new java.util.Date() + " - " + action);
            }
        } catch (java.io.IOException e) {
            // Handling the case where an exception occurs while writing to the log
            System.err.println("Failed to write to system log: " + e.getMessage());
        }
    }

    private void writeToChatLog(String chatLine) {
        try {
            // Creating the logs directory if it doesn't exist
            java.io.File logDir = new java.io.File("logs");
            if (!logDir.exists())
                logDir.mkdirs();
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("logs/chat.log", true))) {
                pw.println(new java.util.Date() + " - " + chatLine);
            }
        } catch (java.io.IOException e) {
            // Handling the case where an exception occurs while writing to the chat log
            System.err.println("Failed to write to chat log: " + e.getMessage());
        }
    }

    private void processCommand(String commandLine) {
        // Splitting the command into components by "::" and setting the main action
        String[] parts = commandLine.split("::");
        String action = parts[0];

        try {
            // Handling each action based on the received command
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
                    handleReportJson(parts.length > 1 ? parts[1] : "ALL", parts.length > 2 ? parts[2] : "");
                    break;
                case "REPORT_WORD":
                    handleReportWord(parts.length > 1 ? parts[1] : "ALL", parts.length > 2 ? parts[2] : "");
                    break;
                case "ADD_EMP":
                    handleAddEmployee(parts);
                    break;
                case "ADD_CUSTOMER":
                    handleAddCustomer(parts);
                    break;

                case "CHAT_REQUEST":
                    if (parts.length > 1) {
                        // Handling a chat request from the current user to another user, checking if the target user is available for chat
                        String targetId = parts[1];
                        if (currentUser == null) {
                            out.println("ERROR::User not logged in.");
                            break;
                        }
                        String status = AdvancedChatMediator.getInstance().requestChat(currentUser.getEmployeeId(),
                                targetId);

                        if ("QUEUED".equals(status)) {
                            // If the target user is busy, the current user is added to the waiting queue
                            out.println("CHAT_QUEUED::The user is busy. You were added to the waiting queue.");
                        } else if (status != null) {
                            // If the chat was started successfully, users receive a notification about the start of the chat
                            out.println("CHAT_STARTED::" + targetId);
                        } else {
                            // If the chat could not be started (for example, if one of the users is already in a chat), an error message is sent
                            out.println("ERROR::Could not start chat.");
                        }
                    }
                    break;

                case "JOIN_CHAT":
                    if (parts.length > 1) {
                        // Handling a request to join an existing chat as a manager
                        String targetUserId = parts[1];
                        boolean joined = AdvancedChatMediator.getInstance()
                                .joinChatAsManager(currentUser.getEmployeeId(), targetUserId);
                        if (joined) {
                            // If the join was successful, the user receives a notification about their participation in the chat
                            out.println("CHAT_STARTED::" + targetUserId + " (Manager Monitor)");
                        } else {
                            // If the join failed (for example, if the requested user is not in a chat), an error message is sent
                            out.println("ERROR::User " + targetUserId + " is not in an active chat.");
                        }
                    }
                    break;

                case "CHAT_MSG":
                    // Handling the sending of a chat message to other users in the chat room
                    String chatMsg = parts.length > 1 ? parts[1] : "";
                    writeToChatLog(currentUser.getEmployeeId() + " (" + currentUser.getBranchId() + "): " + chatMsg);

                    Set<String> participants = AdvancedChatMediator.getInstance()
                            .getRoomParticipants(currentUser.getEmployeeId());

                    for (String participantId : participants) {
                        // Sending the message to all participants in the chat room, except the sender
                        if (!participantId.equals(currentUser.getEmployeeId())) {
                            ClientHandler targetHandler = SessionManager.getInstance().getHandler(participantId);
                            if (targetHandler != null) {
                                targetHandler
                                        .sendMessage("CHAT_INCOMING::" + currentUser.getEmployeeId() + "::" + chatMsg);
                            }
                        }
                    }
                    break;

                case "LEAVE_CHAT":
                    // Handling the request to leave the chat room
                    if (currentUser != null) {
                        AdvancedChatMediator.getInstance().leaveChat(currentUser.getEmployeeId());
                    }
                    out.println("SYSTEM::You left the chat room.");
                    break;

                default:
                    out.println("ERROR::Command not recognized");
            }
        } catch (Exception e) {
            // Handling any exceptions that occur during command processing, sending an error message to the client
            out.println("ERROR::" + e.getMessage());
        }
    }

    private void handleLogin(String empId, String pass) {
        // Handling a user login request, authenticating the user against the database
        if (currentUser != null) {
            // If the user is already logged in, an error message is sent
            out.println("ERROR::Already logged in as " + currentUser.getEmployeeId());
            return;
        }
        try {
            User user = StoreDataManager.getInstance().authenticate(empId, pass);
            SessionManager.getInstance().login(empId, this);
            this.currentUser = user;
            out.println(
                    "LOGIN_SUCCESS::" + user.getFullName() + "::" + user.getRole().name() + "::" + user.getBranchId());
            server.broadcastUserStatus(user.getEmployeeId(), true);
        } catch (AuthenticationException | DuplicateLoginException e) {
            out.println("ERROR::" + e.getMessage());
        }
    }

    private void handleGetInventory() {
        // Handling a request to retrieve the inventory data for the branch of the currently logged-in user
        if (currentUser == null) {
            out.println("ERROR::User not logged in");
            return;
        }
        // Retrieving the branch of the currently logged-in user and fetching its inventory data
        Branch branch = StoreDataManager.getInstance().getBranches().get(currentUser.getBranchId());
        Map<Product, Integer> inv = branch.getInventorySnapshot();
        StringBuilder sb = new StringBuilder("INVENTORY_DATA::");

        for (Map.Entry<Product, Integer> entry : inv.entrySet()) {
            // Building the inventory data string in the appropriate format for sending to the client
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
        // Handling a request to purchase a product by a customer, including validation checks and stock reduction
        if (currentUser == null) {
            out.println("ERROR::User not logged in");
            return;
        }
        try {
            // Handling the purchase: reducing stock, calculating final price based on customer type, creating a sale record
            SaleRecord sale = StoreDataManager.getInstance().processPurchase(
                    currentUser.getBranchId(), currentUser.getEmployeeId(), custId, prodId, qty);
            out.println("BUY_SUCCESS::" + sale.getTransactionId() + "::" + sale.getFinalPrice());
            server.broadcastInventoryUpdate(currentUser.getBranchId());
        } catch (OutOfStockException | IllegalArgumentException e) {
            out.println("ERROR::" + e.getMessage());
        }
    }

    private void handleGetCustomers() {
        // Handling a request to retrieve the list of customers in the system
        StringBuilder sb = new StringBuilder("CUSTOMERS_DATA::");
        for (Customer c : StoreDataManager.getInstance().getCustomers().values()) {
            sb.append(c.getId()).append(",")
                    .append(c.getFullName()).append(",")
                    .append(c.getPhone()).append(",")
                    .append(c.getCustomerType()).append(";");
        }
        out.println(sb.toString());
    }

    private void handleReportJson(String filterType, String filterValue) {
        // Handling a request to create a report in JSON format, including filtering by type and value
        List<SaleRecord> records = StoreDataManager.getInstance().getSalesHistory();
        if (!filterType.equals("ALL")) {
            records = records.stream()
                    .filter(r -> {
                        if (filterType.equals("BRANCH"))
                            return r.getBranchId().equals(filterValue);
                        if (filterType.equals("PRODUCT"))
                            return r.getProductId().equals(filterValue);
                        if (filterType.equals("CATEGORY"))
                            return r.getCategory().equalsIgnoreCase(filterValue);
                        return true;
                    })
                    .collect(Collectors.toList());
        }
        // Generating the JSON report using the ReportGenerator utility and sending it to the client
        String json = ReportGenerator.generateSalesJson(records);
        out.println("REPORT_JSON_DATA::" + json.replace("\n", " "));
    }

    private void handleReportWord(String filterType, String filterValue) {
        // Handling a request to create a report in Word format, including filtering by type and value
        List<SaleRecord> records = StoreDataManager.getInstance().getSalesHistory();
        if (!filterType.equals("ALL")) {
            records = records.stream()
                    .filter(r -> {
                        if (filterType.equals("BRANCH"))
                            return r.getBranchId().equals(filterValue);
                        if (filterType.equals("PRODUCT"))
                            return r.getProductId().equals(filterValue);
                        if (filterType.equals("CATEGORY"))
                            return r.getCategory().equalsIgnoreCase(filterValue);
                        return true;
                    })
                    .collect(Collectors.toList());
        }
        try {
            // Creating a Word document with the report and saving it to the system, sending the file name to the client
            String fileName = "Sales_Report_" + filterType + "_" + System.currentTimeMillis() + ".doc";
            ReportGenerator.exportToWordDoc(fileName, "Sales Report - Filter: " + filterType, records);
            out.println("REPORT_WORD_SUCCESS::" + fileName);
        } catch (IOException e) {
            out.println("ERROR:: Failed to create document: " + e.getMessage());
        }
    }

    public void sendMessage(String msg) {
        // Sending a message to the client associated with this handler, ensuring that the output stream is not null before sending
        if (out != null)
            out.println(msg);
    }

    public User getCurrentUser() {
        // Returning the current user associated with this handler, if any
        return currentUser;
    }

    private void cleanup() {
        // Cleaning up resources when the client disconnects or an exception occurs
        if (currentUser != null) {
            SessionManager.getInstance().logout(currentUser.getEmployeeId());
            AdvancedChatMediator.getInstance().leaveChat(currentUser.getEmployeeId());
            server.broadcastUserStatus(currentUser.getEmployeeId(), false);
        }
        server.removeClient(this);
        try {
            // Closing the client's socket to release resources
            socket.close();
        } catch (IOException ignored) {
        }
    }
}