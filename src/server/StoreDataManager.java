package server;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import exceptions.AuthenticationException;
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

public class StoreDataManager {
    private static StoreDataManager instance;

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Branch> branches = new ConcurrentHashMap<>();
    private final Map<String, Customer> customers = new ConcurrentHashMap<>();
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final List<SaleRecord> salesHistory = new CopyOnWriteArrayList<>();

    private StoreDataManager() {
        initDefaultData();
    }

    public static synchronized StoreDataManager getInstance() {
        // יצירת מופע יחיד של StoreDataManager (Singleton Pattern)
        if (instance == null) {
            instance = new StoreDataManager();
        }
        return instance;
    }

    private void initDefaultData() {
        // סניפים
        Branch b1 = new Branch("B1", "Tel Aviv");
        Branch b2 = new Branch("B2", "Haifa");
        branches.put("B1", b1);
        branches.put("B2", b2);

        // משתמשי מערכת
        User admin = new User("E101", "Avi Cohen", "012345678", "050-1111111", "12-345-678", "B1", Role.ADMIN,
                "admin123");
        User shiftMgr = new User("E102", "Dana Levi", "023456789", "052-2222222", "12-345-679", "B1",
                Role.SHIFT_MANAGER, "mgr123");
        User cashier = new User("E103", "Yossi Sharon", "034567890", "054-3333333", "12-345-680", "B2", Role.CASHIER,
                "cash123");
        users.put(admin.getEmployeeId(), admin);
        users.put(shiftMgr.getEmployeeId(), shiftMgr);
        users.put(cashier.getEmployeeId(), cashier);

        // מוצרים ומלאי
        Product p1 = new Product("P01", " Polo Shirt", "Shirts", 120.0);
        Product p2 = new Product("P02", "Jeans", "Pants", 250.0);
        Product p3 = new Product("P03", "Leather Jacket", "Jackets", 450.0);
        products.put(p1.getId(), p1);
        products.put(p2.getId(), p2);
        products.put(p3.getId(), p3);

        b1.addStock(p1, 20);
        b1.addStock(p2, 15);
        b2.addStock(p1, 10);
        b2.addStock(p3, 8);

        // לקוחות ראשוניים
        Customer c1 = new NewCustomer("C01", "Ronnie Kline", "050-9999991");
        Customer c2 = new ReturningCustomer("C02", "Michal Ziv", "050-9999992");
        Customer c3 = new VipCustomer("C03", "Alon Doron", "050-9999993");
        customers.put(c1.getId(), c1);
        customers.put(c2.getId(), c2);
        customers.put(c3.getId(), c3);
    }

    public User authenticate(String employeeId, String password) throws AuthenticationException {
        // אימות משתמש לפי מזהה וסיסמה
        User u = users.get(employeeId);
        if (u == null || !u.validatePassword(password)) {
            throw new AuthenticationException("Invalid user ID or password.");
        }
        return u;
    }

    public synchronized boolean addUser(User user) {
        // הוספת משתמש חדש למערכת, אם הוא כבר קיים לפי מזהה העובד, מחזירים false
        if (users.containsKey(user.getEmployeeId())) {
            return false;
        }
        users.put(user.getEmployeeId(), user);
        return true;
    }

    public synchronized boolean addCustomer(Customer customer) {
        // הוספת לקוח חדש למערכת, אם הוא כבר קיים לפי מזהה הלקוח, מחזירים false
        if (customers.containsKey(customer.getId())) {
            return false;
        }
        customers.put(customer.getId(), customer);
        return true;
    }

    public synchronized void registerCustomer(Customer customer) {
        // רישום לקוח חדש במערכת והוספתו למאגר הלקוחות
        customers.put(customer.getId(), customer);
        LoggerService.getInstance().log(LoggerService.LogType.CUSTOMERS, "Registered customer: " + customer);
    }

    public synchronized void registerEmployee(User user) {
        // רישום עובד חדש במערכת והוספתו למאגר המשתמשים
        users.put(user.getEmployeeId(), user);
        LoggerService.getInstance().log(LoggerService.LogType.EMPLOYEES, "Registered employee: " + user);
    }

    public synchronized SaleRecord processPurchase(String branchId, String empId, String custId,
            String prodId, int qty) throws OutOfStockException {
                // עיבוד רכישה: הפחתת מלאי, חישוב מחיר סופי לפי סוג הלקוח, יצירת רשומת מכירה
        Branch branch = branches.get(branchId);
        Product prod = products.get(prodId);
        Customer cust = customers.get(custId);

        if (branch == null || prod == null || cust == null) {
            // בדיקה אם הסניף, המוצר או הלקוח אינם קיימים במערכת
            throw new IllegalArgumentException("Invalid branch, product, or customer ID");
        }

        // הפחתת מלאי מסונכרנת
        branch.reduceStock(prod, qty);

        // חישוב מחיר סופי בהתאם ל-Strategy/Polymorphism של הלקוח
        double baseTotal = prod.getBasePrice() * qty;
        double finalPrice = cust.calculateFinalPrice(baseTotal);

        SaleRecord record = new SaleRecord(UUID.randomUUID().toString().substring(0, 8),
                branchId, empId, custId, prodId, prod.getName(), prod.getCategory(), qty, finalPrice);

        salesHistory.add(record);
        LoggerService.getInstance().log(LoggerService.LogType.TRANSACTIONS, record.toLogString());
        return record;
    }

    public List<SaleRecord> getSalesHistory() {
        // החזרת רשימת כל רשומות המכירה שנעשו במערכת
        return Collections.unmodifiableList(salesHistory);
    }

    public Map<String, Customer> getCustomers() {
        // החזרת מפת כל הלקוחות במערכת
        return customers;
    }

    public Map<String, Branch> getBranches() {
        // החזרת מפת כל הסניפים במערכת
        return branches;
    }

    public Map<String, Product> getProducts() {
        // החזרת מפת כל המוצרים במערכת
        return products;
    }

    public Map<String, User> getUsers() {
        // החזרת מפת כל המשתמשים במערכת
        return users;
    }
}