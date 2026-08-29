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
            // טיפול במקרה של חריגה בעת קריאה או כתיבה ללקוח
            System.out.println("Client disconnected unexpectedly: " +
                    (currentUser != null ? currentUser.getEmployeeId() : socket.getRemoteSocketAddress()));
        } finally {
            cleanup();
        }
    }

    private void handleAddEmployee(String[] parts) {
        // בדיקה אם יש מספיק פרטים להוספת עובד חדש
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
            // בדיקה אם הסיסמה עומדת במדיניות האבטחה (לפחות 6 תווים)
            out.println("ERROR::Password policy violation - must be at least 6 characters.");
            return;
        }

        try {
            // המרת מחרוזת התפקיד לאובייקט Role, טיפול במקרה של תפקיד לא חוקי
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
        // בדיקה אם יש מספיק פרטים להוספת לקוח חדש
        if (parts.length < 5) {
            out.println("ERROR::Missing customer details.");
            return;
        }
        String id = parts[1];
        String name = parts[2];
        String phone = parts[3];
        String typeStr = parts[4].toUpperCase();

        Customer newCustomer = null;
        switch (typeStr) { // יצירת אובייקט לקוח לפי סוג הלקוח (חדש, חוזר, VIP)
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
            // רישום הלקוח החדש במערכת והוספתו למאגר הלקוחות
            writeToSystemLog("Customer Registered: " + name + " (Type: " + typeStr + ") by employee "
                    + currentUser.getEmployeeId());
            out.println("ADD_CUSTOMER_SUCCESS::" + name);
        } else {
            // טיפול במקרה שבו מזהה הלקוח כבר קיים במערכת
            out.println("ERROR::Customer ID already exists.");
        }
    }

    private void writeToSystemLog(String action) { // כתיבת פעולה ללוג המערכת
        try {
            // יצירת תיקיית הלוגים אם היא לא קיימת
            java.io.File logDir = new java.io.File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("logs/system.log", true))) {
                pw.println(new java.util.Date() + " - " + action);
            }
        } catch (java.io.IOException e) {
            // טיפול במקרה של חריגה בעת כתיבת הלוג
            System.err.println("Failed to write to system log: " + e.getMessage());
        }
    }

    private void writeToChatLog(String chatLine) {
        try {
            // יצירת תיקיית הלוגים אם היא לא קיימת
            java.io.File logDir = new java.io.File("logs");
            if (!logDir.exists())
                logDir.mkdirs();
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("logs/chat.log", true))) {
                pw.println(new java.util.Date() + " - " + chatLine);
            }
        } catch (java.io.IOException e) {
            // טיפול במקרה של חריגה בעת כתיבת הלוג של הצ'אט
            System.err.println("Failed to write to chat log: " + e.getMessage());
        }
    }

    private void processCommand(String commandLine) {
        // פיצול הפקודה למרכיבים לפי "::" והגדרת הפעולה הראשית
        String[] parts = commandLine.split("::");
        String action = parts[0];

        try {
            // טיפול בכל פעולה בהתאם לפקודה שהתקבלה מהלקוח
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
                        // בקשת התחלת שיחה עם משתמש אחר
                        String targetId = parts[1];
                        if (currentUser == null) {
                            out.println("ERROR::User not logged in.");
                            break;
                        }
                        String status = AdvancedChatMediator.getInstance().requestChat(currentUser.getEmployeeId(),
                                targetId);

                        if ("QUEUED".equals(status)) {
                            // אם המשתמש המבוקש עסוק, המשתמש הנוכחי נוסף לתור ההמתנה
                            out.println("CHAT_QUEUED::The user is busy. You were added to the waiting queue.");
                        } else if (status != null) {
                            // אם השיחה נוצרה בהצלחה, המשתמשים מקבלים הודעה על התחלת השיחה
                            out.println("CHAT_STARTED::" + targetId);
                        } else {
                            // אם לא ניתן להתחיל שיחה (למשל, אחד המשתמשים כבר נמצא בשיחה), נשלחת הודעת שגיאה
                            out.println("ERROR::Could not start chat.");
                        }
                    }
                    break;

                case "JOIN_CHAT":
                    if (parts.length > 1) {
                        // בקשת הצטרפות לשיחה קיימת כמנהל
                        String targetUserId = parts[1];
                        boolean joined = AdvancedChatMediator.getInstance()
                                .joinChatAsManager(currentUser.getEmployeeId(), targetUserId);
                        if (joined) {
                            // אם ההצטרפות הצליחה, המשתמש מקבל הודעה על הצטרפותו לשיחה
                            out.println("CHAT_STARTED::" + targetUserId + " (Manager Monitor)");
                        } else {
                            // אם ההצטרפות נכשלה (למשל, המשתמש המבוקש לא נמצא בשיחה), נשלחת הודעת שגיאה
                            out.println("ERROR::User " + targetUserId + " is not in an active chat.");
                        }
                    }
                    break;

                case "CHAT_MSG":
                    // טיפול בשליחת הודעת צ'אט למשתמשים אחרים בחדר הצ'אט
                    String chatMsg = parts.length > 1 ? parts[1] : "";
                    writeToChatLog(currentUser.getEmployeeId() + " (" + currentUser.getBranchId() + "): " + chatMsg);

                    Set<String> participants = AdvancedChatMediator.getInstance()
                            .getRoomParticipants(currentUser.getEmployeeId());

                    for (String participantId : participants) {
                        // שליחת ההודעה לכל המשתתפים בחדר הצ'אט, למעט השולח עצמו
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
                    // טיפול בבקשה לעזיבת חדר הצ'אט
                    if (currentUser != null) {
                        AdvancedChatMediator.getInstance().leaveChat(currentUser.getEmployeeId());
                    }
                    out.println("SYSTEM::You left the chat room.");
                    break;

                default:
                    out.println("ERROR::Command not recognized");
            }
        } catch (Exception e) {
            // טיפול במקרה של חריגה בעת עיבוד הפקודה, שליחת הודעת שגיאה ללקוח
            out.println("ERROR::" + e.getMessage());
        }
    }

    private void handleLogin(String empId, String pass) {
        // טיפול בבקשת התחברות של משתמש, אימות פרטי המשתמש מול מסד הנתונים
        if (currentUser != null) {
            // אם המשתמש כבר מחובר, נשלחת הודעת שגיאה על כך
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
        // טיפול בבקשה לקבלת נתוני המלאי של הסניף שבו המשתמש מחובר
        if (currentUser == null) {
            out.println("ERROR::User not logged in");
            return;
        }
        // קבלת הסניף של המשתמש הנוכחי ושליפת נתוני המלאי שלו
        Branch branch = StoreDataManager.getInstance().getBranches().get(currentUser.getBranchId());
        Map<Product, Integer> inv = branch.getInventorySnapshot();
        StringBuilder sb = new StringBuilder("INVENTORY_DATA::");

        for (Map.Entry<Product, Integer> entry : inv.entrySet()) {
            // בניית מחרוזת נתוני המלאי בפורמט מתאים לשליחה ללקוח
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
        // טיפול בבקשה לרכישת מוצר על ידי לקוח, כולל בדיקות תקינות והפחתת מלאי
        if (currentUser == null) {
            out.println("ERROR::User not logged in");
            return;
        }
        try {
            // עיבוד הרכישה: הפחתת מלאי, חישוב מחיר סופי לפי סוג הלקוח, יצירת רשומת מכירה
            SaleRecord sale = StoreDataManager.getInstance().processPurchase(
                    currentUser.getBranchId(), currentUser.getEmployeeId(), custId, prodId, qty);
            out.println("BUY_SUCCESS::" + sale.getTransactionId() + "::" + sale.getFinalPrice());
            server.broadcastInventoryUpdate(currentUser.getBranchId());
        } catch (OutOfStockException | IllegalArgumentException e) {
            out.println("ERROR::" + e.getMessage());
        }
    }

    private void handleGetCustomers() {
        // טיפול בבקשה לקבלת רשימת הלקוחות במערכת
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
        // טיפול בבקשה ליצירת דוח במבנה JSON, כולל סינון לפי סוג וסינון לפי ערך מסוים
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
        // יצירת מחרוזת JSON מהנתונים המסוננים ושליחתה ללקוח
        String json = ReportGenerator.generateSalesJson(records);
        out.println("REPORT_JSON_DATA::" + json.replace("\n", " "));
    }

    private void handleReportWord(String filterType, String filterValue) {
        // טיפול בבקשה ליצירת דוח במבנה Word, כולל סינון לפי סוג וסינון לפי ערך מסוים
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
            //  יצירת קובץ Word עם הדוח ושמירתו במערכת, שליחת שם הקובץ ללקוח
            String fileName = "Sales_Report_" + filterType + "_" + System.currentTimeMillis() + ".doc";
            ReportGenerator.exportToWordDoc(fileName, "Sales Report - Filter: " + filterType, records);
            out.println("REPORT_WORD_SUCCESS::" + fileName);
        } catch (IOException e) {
            out.println("ERROR:: Failed to create document: " + e.getMessage());
        }
    }

    public void sendMessage(String msg) {
        // שליחת הודעה ללקוח המחובר דרך ה-PrintWriter, אם הוא מחובר
        if (out != null)
            out.println(msg);
    }

    public User getCurrentUser() {
        // החזרת המשתמש הנוכחי המחובר ללקוח זה, אם יש כזה
        return currentUser;
    }

    private void cleanup() {
        // טיפול בניקוי המשאבים כאשר הלקוח מתנתק או מתרחש חריגה
        if (currentUser != null) {
            SessionManager.getInstance().logout(currentUser.getEmployeeId());
            AdvancedChatMediator.getInstance().leaveChat(currentUser.getEmployeeId());
            server.broadcastUserStatus(currentUser.getEmployeeId(), false);
        }
        server.removeClient(this);
        try {
            // סגירת ה-socket של הלקוח כדי לשחרר משאבים
            socket.close();
        } catch (IOException ignored) {
        }
    }
}