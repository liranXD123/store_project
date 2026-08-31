package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import exceptions.AuthenticationException;
import exceptions.DuplicateLoginException;
import exceptions.OutOfStockException;
import model.Branch;
import model.Product;
import model.Role;
import model.SaleRecord;
import model.User;
import model.customers.Customer;
import model.customers.NewCustomer;
import model.customers.ReturningCustomer;
import model.customers.VipCustomer;
import patterns.observer.StoreObserver;

// Class responsible for handling a single client connection and processing the commands that
// arrive from it. Every connection runs in a thread of its own, and the handler is also an
// observer of the server, so it is told about changes made by the other connected employees.
public class ClientHandler implements Runnable, StoreObserver {
    private final Socket socket;
    private final StoreServer server;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;

    public ClientHandler(Socket socket, StoreServer server) {
        // store the socket and server reference for later use
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // thread-safe initialization of input and output streams for client communication
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

    private void processCommand(String commandLine) {
        // Splitting the command into components by "::" and setting the main action
        String[] parts = commandLine.split("::");
        String action = parts[0];

        // Every command except the login itself is allowed only to a user that already
        // authenticated on this connection. The client hides those options from the menu,
        // but the server must not trust the client and checks it again here
        if (!action.equals("LOGIN") && currentUser == null) {
            out.println("ERROR::You must log in before using this action.");
            return;
        }

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
                case "ADD_PRODUCT":
                    handleAddProduct(parts);
                    break;
                case "RESTOCK":
                    handleRestock(parts[1], Integer.parseInt(parts[2]));
                    break;
                case "GET_CUSTOMERS":
                    handleGetCustomers();
                    break;
                case "GET_EMPLOYEES":
                    handleGetEmployees();
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
                        handleChatRequest(parts[1]);
                    }
                    break;
                case "GET_ACTIVE_CHATS":
                    handleGetActiveChats();
                    break;
                case "JOIN_CHAT":
                    if (parts.length > 1) {
                        handleJoinChat(parts[1]);
                    }
                    break;
                case "CHAT_MSG":
                    handleChatMessage(parts.length > 1 ? parts[1] : "");
                    break;
                case "LEAVE_CHAT":
                    handleLeaveChat();
                    break;
                default:
                    out.println("ERROR::Command not recognized");
            }
        } catch (NumberFormatException e) {
            // Handling a quantity that was not written as a whole number
            out.println("ERROR::Quantity must be a whole number.");
        } catch (Exception e) {
            // Handling any other exception that occurs during command processing
            out.println("ERROR::" + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Login and employees
    // ------------------------------------------------------------------

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
            LoggerService.getInstance().log(LoggerService.LogType.SYSTEM,
                    "Login: " + user.getEmployeeId() + " (" + user.getRole().name() + ") at branch "
                            + user.getBranchId());
            server.notifyUserStatusChanged(user.getEmployeeId(), true);
            // An employee that just connected is free, so somebody may already be waiting for their branch
            notifyWaitingRequester();
        } catch (AuthenticationException | DuplicateLoginException e) {
            out.println("ERROR::" + e.getMessage());
        }
    }

    private void handleAddEmployee(String[] parts) {
        // Registering an employee is an administrator action only
        if (currentUser.getRole() != Role.ADMIN) {
            out.println("ERROR::Permission denied. Admins only.");
            return;
        }
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

        // An employee must belong to a branch that really exists, otherwise they would not
        // be able to see any inventory after logging in
        if (!StoreDataManager.getInstance().branchExists(branch)) {
            out.println("ERROR::Unknown branch " + branch + ".");
            return;
        }

        try {
            // Checking that the password answers the password policy of the system
            PasswordPolicyValidator.validatePassword(pwd);
        } catch (AuthenticationException e) {
            out.println("ERROR::Password policy violation - " + e.getMessage());
            return;
        }

        try {
            // Converting the role string to a Role object, handling invalid roles
            Role role = Role.valueOf(roleStr.toUpperCase());
            User newUser = new User(empId, name, tz, phone, bank, branch, role, pwd);
            boolean added = StoreDataManager.getInstance().addUser(newUser);
            if (added) {
                LoggerService.getInstance().log(LoggerService.LogType.EMPLOYEES,
                        "Employee registered: " + newUser + " by admin " + currentUser.getEmployeeId());
                out.println("ADD_EMP_SUCCESS::" + name);
            } else {
                out.println("ERROR::Employee ID already exists.");
            }
        } catch (IllegalArgumentException e) {
            out.println("ERROR::Invalid Role specified. Use ADMIN, SHIFT_MANAGER, CASHIER or SELLER.");
        }
    }

    private void handleGetEmployees() {
        // Viewing the employees of the network is part of the administrator screen
        if (currentUser.getRole() != Role.ADMIN) {
            out.println("ERROR::Permission denied. Admins only.");
            return;
        }
        StringBuilder sb = new StringBuilder("EMPLOYEES_DATA::");
        for (User u : StoreDataManager.getInstance().getUsers().values()) {
            sb.append(u.getEmployeeId()).append(",")
                    .append(u.getFullName()).append(",")
                    .append(u.getIdNumber()).append(",")
                    .append(u.getPhone()).append(",")
                    .append(u.getBankAccountNumber()).append(",")
                    .append(u.getBranchId()).append(",")
                    .append(u.getRole().getTitle()).append(";");
        }
        out.println(sb.toString());
    }

    // ------------------------------------------------------------------
    // Customers
    // ------------------------------------------------------------------

    private void handleAddCustomer(String[] parts) {
        // Checking if there are enough details to add a new customer
        if (parts.length < 6) {
            out.println("ERROR::Missing customer details.");
            return;
        }
        String id = parts[1];
        String name = parts[2];
        String tz = parts[3];
        String phone = parts[4];
        String typeStr = parts[5].toUpperCase();

        Customer newCustomer = null;
        switch (typeStr) { // Creating a new customer object based on the specified type (NEW, RETURNING, VIP)
            case "NEW":
                newCustomer = new NewCustomer(id, name, tz, phone);
                break;
            case "RETURNING":
                newCustomer = new ReturningCustomer(id, name, tz, phone);
                break;
            case "VIP":
                newCustomer = new VipCustomer(id, name, tz, phone);
                break;
            default:
                out.println("ERROR::Invalid customer type. Use NEW, RETURNING, or VIP.");
                return;
        }

        boolean added = StoreDataManager.getInstance().addCustomer(newCustomer);
        if (added) {
            // Logging the customer registration action, including the employee who performed it
            LoggerService.getInstance().log(LoggerService.LogType.CUSTOMERS,
                    "Customer registered: " + newCustomer + " by employee " + currentUser.getEmployeeId());
            out.println("ADD_CUSTOMER_SUCCESS::" + name);
            // The customer list belongs to the whole network, so every connected employee is told about it
            server.notifyCustomerListChanged();
        } else {
            LoggerService.getInstance().log(LoggerService.LogType.CUSTOMERS,
                    "Failed to register customer " + id + " by employee " + currentUser.getEmployeeId()
                            + " - ID already exists");
            out.println("ERROR::Customer ID already exists.");
        }
    }

    private void handleGetCustomers() {
        // Handling a request to retrieve the list of customers in the system
        StringBuilder sb = new StringBuilder("CUSTOMERS_DATA::");
        for (Customer c : StoreDataManager.getInstance().getCustomers().values()) {
            sb.append(c.getId()).append(",")
                    .append(c.getFullName()).append(",")
                    .append(c.getIdNumber()).append(",")
                    .append(c.getPhone()).append(",")
                    .append(c.getCustomerType()).append(";");
        }
        out.println(sb.toString());
    }

    // ------------------------------------------------------------------
    // Inventory
    // ------------------------------------------------------------------

    private void handleGetInventory() {
        // Handling a request to retrieve the inventory data for the branch of the currently logged-in user
        Branch branch = StoreDataManager.getInstance().getBranch(currentUser.getBranchId());
        if (branch == null) {
            out.println("ERROR::Your branch " + currentUser.getBranchId() + " is not defined in the system.");
            return;
        }
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
        // Handling the sale of a product to a customer, including validation checks and stock reduction
        try {
            // Reducing stock, calculating the final price by the type of the customer and creating a sale record
            SaleRecord sale = StoreDataManager.getInstance().processPurchase(
                    currentUser.getBranchId(), currentUser.getEmployeeId(), custId, prodId, qty);
            out.println("BUY_SUCCESS::" + sale.getTransactionId() + "::" + sale.getFinalPrice());
            server.notifyInventoryChanged(currentUser.getBranchId());
        } catch (OutOfStockException | IllegalArgumentException e) {
            out.println("ERROR::" + e.getMessage());
        }
    }

    // Adding a product that the network did not sell until now. The product itself belongs to
    // the whole network, while the quantity that arrived belongs to the branch that added it
    private void handleAddProduct(String[] parts) {
        if (currentUser.getRole() != Role.SHIFT_MANAGER && currentUser.getRole() != Role.ADMIN) {
            out.println("ERROR::Permission denied. Managers only.");
            return;
        }
        if (parts.length < 6) {
            out.println("ERROR::Missing product details.");
            return;
        }
        String id = parts[1];
        String name = parts[2];
        String category = parts[3];

        if (id.isEmpty() || name.isEmpty() || category.isEmpty()) {
            out.println("ERROR::The product ID, name and category cannot be empty.");
            return;
        }

        // The price and the quantity are checked separately, so the employee is told
        // exactly which of the two was not written as a number
        double basePrice;
        try {
            basePrice = Double.parseDouble(parts[4]);
        } catch (NumberFormatException e) {
            out.println("ERROR::The price must be a number, for example 149.90");
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(parts[5]);
        } catch (NumberFormatException e) {
            out.println("ERROR::The starting quantity must be a whole number.");
            return;
        }

        if (basePrice <= 0) {
            out.println("ERROR::The price must be greater than zero.");
            return;
        }
        if (quantity < 0) {
            out.println("ERROR::The starting quantity cannot be negative.");
            return;
        }

        Product product = new Product(id, name, category, basePrice);
        boolean added = StoreDataManager.getInstance()
                .addProduct(product, currentUser.getBranchId(), quantity);
        if (!added) {
            out.println("ERROR::Product ID already exists.");
            return;
        }

        LoggerService.getInstance().log(LoggerService.LogType.TRANSACTIONS,
                "Product added: " + product + " with " + quantity + " units in branch "
                        + currentUser.getBranchId() + " by employee " + currentUser.getEmployeeId());
        out.println("ADD_PRODUCT_SUCCESS::" + id + "::" + name);

        if (quantity > 0) {
            server.notifyInventoryChanged(currentUser.getBranchId());
        }
    }

    private void handleRestock(String prodId, int qty) {
        // Buying goods into the branch is a stock management action, allowed to a shift manager or an administrator
        if (currentUser.getRole() != Role.SHIFT_MANAGER && currentUser.getRole() != Role.ADMIN) {
            out.println("ERROR::Permission denied. Managers only.");
            return;
        }
        try {
            StoreDataManager.getInstance().restockProduct(currentUser.getBranchId(), prodId, qty);
            out.println("RESTOCK_SUCCESS::" + prodId + "::" + qty);
            server.notifyInventoryChanged(currentUser.getBranchId());
        } catch (IllegalArgumentException e) {
            out.println("ERROR::" + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Reports
    // ------------------------------------------------------------------

    // Building the list of the sales that answer the requested filter.
    // Both report types use it, so the filtering is written only once
    private List<SaleRecord> filterRecords(String filterType, String filterValue) {
        List<SaleRecord> all = StoreDataManager.getInstance().getSalesHistory();
        List<SaleRecord> result = new ArrayList<SaleRecord>();

        for (int i = 0; i < all.size(); i++) {
            SaleRecord r = all.get(i);
            boolean keep;
            if (filterType.equals("BRANCH")) {
                keep = r.getBranchId().equals(filterValue);
            } else if (filterType.equals("PRODUCT")) {
                keep = r.getProductId().equals(filterValue);
            } else if (filterType.equals("CATEGORY")) {
                keep = r.getCategory().equalsIgnoreCase(filterValue);
            } else if (filterType.equals("DATE")) {
                keep = r.getSaleDate().equals(filterValue);
            } else {
                keep = true;
            }
            if (keep) {
                result.add(r);
            }
        }

        // Ordering the sales chronologically, using the natural order defined in SaleRecord
        Collections.sort(result);
        return result;
    }

    private void handleReportJson(String filterType, String filterValue) {
        // Handling a request to create a report in JSON format, including filtering by type and value
        List<SaleRecord> records = filterRecords(filterType, filterValue);
        String json = ReportGenerator.generateSalesJson(records);
        out.println("REPORT_JSON_DATA::" + json.replace("\n", " "));
    }

    private void handleReportWord(String filterType, String filterValue) {
        // Handling a request to create a report in Word format, including filtering by type and value
        List<SaleRecord> records = filterRecords(filterType, filterValue);
        try {
            // Creating a Word document with the report and saving it, sending the file name to the client
            String fileName = "Sales_Report_" + filterType + "_" + System.currentTimeMillis() + ".doc";
            ReportGenerator.exportToWordDoc(fileName, "Sales Report - Filter: " + filterType, records);
            out.println("REPORT_WORD_SUCCESS::" + fileName);
        } catch (IOException e) {
            out.println("ERROR:: Failed to create document: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Chat
    // ------------------------------------------------------------------

    // Opening a chat against another branch. The employee asks for a branch, and the server
    // looks for an employee of that branch who is connected and not busy. When nobody is free,
    // the request is kept in the waiting queue of that branch
    private void handleChatRequest(String targetBranchId) {
        if (targetBranchId.equals(currentUser.getBranchId())) {
            out.println("ERROR::A chat has to be opened against a different branch.");
            return;
        }
        if (!StoreDataManager.getInstance().branchExists(targetBranchId)) {
            out.println("ERROR::Unknown branch " + targetBranchId + ".");
            return;
        }
        if (AdvancedChatMediator.getInstance().isUserInChat(currentUser.getEmployeeId())) {
            out.println("ERROR::You are already in a chat. Leave it before opening a new one.");
            return;
        }

        ClientHandler target = server.findAvailableUserInBranch(targetBranchId, currentUser.getEmployeeId());
        if (target == null) {
            // Nobody is free in that branch, so the request is remembered until somebody becomes free
            AdvancedChatMediator.getInstance().addToBranchQueue(targetBranchId, currentUser.getEmployeeId());
            out.println("CHAT_QUEUED::No employee is free in branch " + targetBranchId
                    + " right now. You were added to the waiting queue.");
            LoggerService.getInstance().log(LoggerService.LogType.CHAT,
                    "Chat request queued: " + currentUser.getEmployeeId() + " -> branch " + targetBranchId);
            return;
        }

        String targetId = target.getCurrentUser().getEmployeeId();
        String roomId = AdvancedChatMediator.getInstance()
                .createOneOnOneChat(currentUser.getEmployeeId(), targetId);
        if (roomId == null) {
            out.println("ERROR::Could not start chat.");
            return;
        }

        // Both sides are told that the conversation has started
        out.println("CHAT_STARTED::" + targetId);
        target.sendMessage("CHAT_STARTED::" + currentUser.getEmployeeId());
        LoggerService.getInstance().log(LoggerService.LogType.CHAT,
                "Chat opened: " + currentUser.getEmployeeId() + " (" + currentUser.getBranchId() + ") <-> "
                        + targetId + " (" + targetBranchId + ") in " + roomId);
    }

    // Listing the conversations a manager is allowed to join: the ones open in their own
    // branch. An administrator sees the conversations of the whole network
    private void handleGetActiveChats() {
        if (currentUser.getRole() != Role.SHIFT_MANAGER && currentUser.getRole() != Role.ADMIN) {
            out.println("ERROR::Permission denied. Managers only.");
            return;
        }
        Map<String, User> allUsers = StoreDataManager.getInstance().getUsers();
        List<String> busyUsers = AdvancedChatMediator.getInstance().getUsersInChat();
        StringBuilder sb = new StringBuilder("ACTIVE_CHATS_DATA::");

        for (int i = 0; i < busyUsers.size(); i++) {
            String employeeId = busyUsers.get(i);
            User employee = allUsers.get(employeeId);
            if (employee == null || !mayManageChatOf(employee)) {
                continue;
            }
            // Listing who this employee is talking to
            StringBuilder partners = new StringBuilder();
            for (String participant : AdvancedChatMediator.getInstance().getRoomParticipants(employeeId)) {
                if (!participant.equals(employeeId)) {
                    if (partners.length() > 0)
                        partners.append("/");
                    partners.append(participant);
                }
            }
            sb.append(employeeId).append(",")
                    .append(employee.getFullName()).append(",")
                    .append(employee.getBranchId()).append(",")
                    .append(partners.length() == 0 ? "-" : partners.toString()).append(";");
        }
        out.println(sb.toString());
    }

    // A shift manager is responsible for their own branch only, an administrator for all of them
    private boolean mayManageChatOf(User employee) {
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }
        return employee.getBranchId().equals(currentUser.getBranchId());
    }

    private void handleJoinChat(String targetUserId) {
        // Joining the chat of another employee is allowed only to a shift manager or an administrator
        if (currentUser.getRole() != Role.SHIFT_MANAGER && currentUser.getRole() != Role.ADMIN) {
            out.println("ERROR::Permission denied. Managers only.");
            return;
        }
        // A shift manager may follow the conversations of their own branch only
        User target = StoreDataManager.getInstance().getUsers().get(targetUserId);
        if (target == null) {
            out.println("ERROR::Unknown employee " + targetUserId + ".");
            return;
        }
        if (!mayManageChatOf(target)) {
            out.println("ERROR::You may only join chats of employees in your own branch ("
                    + currentUser.getBranchId() + ").");
            return;
        }
        boolean joined = AdvancedChatMediator.getInstance()
                .joinChatAsManager(currentUser.getEmployeeId(), targetUserId);
        if (joined) {
            out.println("CHAT_STARTED::" + targetUserId + " (Manager Monitor)");
            LoggerService.getInstance().log(LoggerService.LogType.CHAT,
                    "Manager " + currentUser.getEmployeeId() + " joined the chat of " + targetUserId);
        } else {
            out.println("ERROR::User " + targetUserId + " is not in an active chat.");
        }
    }

    private void handleChatMessage(String chatMsg) {
        // Writing the content of the conversation to the chat log
        LoggerService.getInstance().log(LoggerService.LogType.CHAT,
                currentUser.getEmployeeId() + " (" + currentUser.getBranchId() + "): " + chatMsg);

        Set<String> participants = AdvancedChatMediator.getInstance()
                .getRoomParticipants(currentUser.getEmployeeId());

        for (String participantId : participants) {
            // Sending the message to all participants in the chat room, except the sender
            if (!participantId.equals(currentUser.getEmployeeId())) {
                ClientHandler targetHandler = SessionManager.getInstance().getHandler(participantId);
                if (targetHandler != null) {
                    targetHandler.sendMessage("CHAT_INCOMING::" + currentUser.getEmployeeId() + "::" + chatMsg);
                }
            }
        }
    }

    private void handleLeaveChat() {
        // Handling the request to leave the chat room
        AdvancedChatMediator.getInstance().leaveChat(currentUser.getEmployeeId());
        out.println("SYSTEM::You left the chat room.");
        // This employee is free again, so somebody who could not reach the branch may be told about it
        notifyWaitingRequester();
    }

    // Telling this employee about somebody who tried to reach their branch while everybody was busy,
    // so they can open a chat back with them
    private void notifyWaitingRequester() {
        String waitingUser = AdvancedChatMediator.getInstance().pollBranchQueue(currentUser.getBranchId());
        if (waitingUser != null) {
            out.println("CHAT_QUEUED::Employee " + waitingUser
                    + " tried to reach your branch while everybody was busy. You can open a chat back with them.");
            LoggerService.getInstance().log(LoggerService.LogType.CHAT,
                    "Missed chat request from " + waitingUser + " delivered to " + currentUser.getEmployeeId());
        }
    }

    // ------------------------------------------------------------------
    // Observer of the store
    // ------------------------------------------------------------------

    @Override
    public void onInventoryChanged(String branchId) {
        // The inventory is separate per branch, so only the employees of that branch are interested
        if (currentUser != null && currentUser.getBranchId().equals(branchId)) {
            sendMessage("INVENTORY_UPDATED");
        }
    }

    @Override
    public void onCustomerListChanged() {
        // The customer list belongs to the whole network, so every connected employee is interested
        if (currentUser != null) {
            sendMessage("CUSTOMERS_UPDATED");
        }
    }

    @Override
    public void onUserStatusChanged(String employeeId, boolean isOnline) {
        // An employee does not need an announcement about themselves
        if (currentUser != null && !currentUser.getEmployeeId().equals(employeeId)) {
            sendMessage("USER_STATUS::" + employeeId + "::" + (isOnline ? "ONLINE" : "OFFLINE"));
        }
    }

    // ------------------------------------------------------------------

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
            AdvancedChatMediator.getInstance().removeFromAllQueues(currentUser.getEmployeeId());
            LoggerService.getInstance().log(LoggerService.LogType.SYSTEM,
                    "Logout: " + currentUser.getEmployeeId());
            server.notifyUserStatusChanged(currentUser.getEmployeeId(), false);
        }
        server.removeObserver(this);
        try {
            // Closing the client's socket to release resources
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
