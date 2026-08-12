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
        if (instance == null) {
            instance = new StoreDataManager();
        }
        return instance;
    }

    private void initDefaultData() {
        // סניפים
        Branch b1 = new Branch("B1", "תל אביב");
        Branch b2 = new Branch("B2", "חיפה");
        branches.put("B1", b1);
        branches.put("B2", b2);

        // משתמשי מערכת
        User admin = new User("E101", "אבי כהן", "012345678", "050-1111111", "12-345-678", "B1", Role.ADMIN, "admin123");
        User shiftMgr = new User("E102", "דנה לוי", "023456789", "052-2222222", "12-345-679", "B1", Role.SHIFT_MANAGER, "mgr123");
        User cashier = new User("E103", "יוסי שרון", "034567890", "054-3333333", "12-345-680", "B2", Role.CASHIER, "cash123");
        users.put(admin.getEmployeeId(), admin);
        users.put(shiftMgr.getEmployeeId(), shiftMgr);
        users.put(cashier.getEmployeeId(), cashier);

        // מוצרים ומלאי
        Product p1 = new Product("P01", "חולצת פולו", "חולצות", 120.0);
        Product p2 = new Product("P02", "מכנסי ג'ינס", "מכנסיים", 250.0);
        Product p3 = new Product("P03", "ז'קט עור", "עליוניות", 450.0);
        products.put(p1.getId(), p1);
        products.put(p2.getId(), p2);
        products.put(p3.getId(), p3);

        b1.addStock(p1, 20);
        b1.addStock(p2, 15);
        b2.addStock(p1, 10);
        b2.addStock(p3, 8);

        // לקוחות ראשוניים
        Customer c1 = new NewCustomer("C01", "רוני קליין", "050-9999991");
        Customer c2 = new ReturningCustomer("C02", "מיכל זיו", "050-9999992");
        Customer c3 = new VipCustomer("C03", "אלון דורון", "050-9999993");
        customers.put(c1.getId(), c1);
        customers.put(c2.getId(), c2);
        customers.put(c3.getId(), c3);
    }

    public User authenticate(String employeeId, String password) throws AuthenticationException {
        User u = users.get(employeeId);
        if (u == null || !u.validatePassword(password)) {
            throw new AuthenticationException("שם משתמש או סיסמה שגויים!");
        }
        return u;
    }

    public synchronized void registerCustomer(Customer customer) {
        customers.put(customer.getId(), customer);
        LoggerService.getInstance().log(LoggerService.LogType.CUSTOMERS, "Registered customer: " + customer);
    }

    public synchronized void registerEmployee(User user) {
        users.put(user.getEmployeeId(), user);
        LoggerService.getInstance().log(LoggerService.LogType.EMPLOYEES, "Registered employee: " + user);
    }

    public synchronized SaleRecord processPurchase(String branchId, String empId, String custId, 
                                                   String prodId, int qty) throws OutOfStockException {
        Branch branch = branches.get(branchId);
        Product prod = products.get(prodId);
        Customer cust = customers.get(custId);

        if (branch == null || prod == null || cust == null) {
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
        return Collections.unmodifiableList(salesHistory);
    }

    public Map<String, Customer> getCustomers() { return customers; }
    public Map<String, Branch> getBranches() { return branches; }
    public Map<String, Product> getProducts() { return products; }
    public Map<String, User> getUsers() { return users; }
}