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

// Singleton class responsible for managing store data, including users, branches, customers, products, and sales history. It provides methods for authentication, adding users/customers, processing purchases, and retrieving various data snapshots.
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
        // Creating a single instance of StoreDataManager (Singleton Pattern) to ensure
        // that all parts of the application access the same data manager instance
        if (instance == null) {
            instance = new StoreDataManager();
        }
        return instance;
    }

    private void initDefaultData() {
        // Branches
        Branch b1 = new Branch("B1", "Tel Aviv");
        Branch b2 = new Branch("B2", "Haifa");
        branches.put("B1", b1);
        branches.put("B2", b2);

        // Users
        User admin = new User("E101", "Avi Cohen", "012345678", "050-1111111", "12-345-678", "B1", Role.ADMIN,
                "admin123");
        User shiftMgr = new User("E102", "Dana Levi", "023456789", "052-2222222", "12-345-679", "B1",
                Role.SHIFT_MANAGER, "mgr123");
        User cashier = new User("E103", "Yossi Sharon", "034567890", "054-3333333", "12-345-680", "B2", Role.CASHIER,
                "cash123");
        users.put(admin.getEmployeeId(), admin);
        users.put(shiftMgr.getEmployeeId(), shiftMgr);
        users.put(cashier.getEmployeeId(), cashier);

        // Products and initial stock for branches
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

        // initializing customers
        Customer c1 = new NewCustomer("C01", "Ronnie Kline", "050-9999991");
        Customer c2 = new ReturningCustomer("C02", "Michal Ziv", "050-9999992");
        Customer c3 = new VipCustomer("C03", "Alon Doron", "050-9999993");
        customers.put(c1.getId(), c1);
        customers.put(c2.getId(), c2);
        customers.put(c3.getId(), c3);
    }

    public User authenticate(String employeeId, String password) throws AuthenticationException {
        // Authenticating a user by their ID and password
        User u = users.get(employeeId);
        if (u == null || !u.validatePassword(password)) {
            throw new AuthenticationException("Invalid user ID or password.");
        }
        return u;
    }

    public synchronized boolean addUser(User user) {
        // Adding a new user to the system, returning false if they already exist by
        // employee ID
        if (users.containsKey(user.getEmployeeId())) {
            return false;
        }
        users.put(user.getEmployeeId(), user);
        return true;
    }

    public synchronized boolean addCustomer(Customer customer) {
        // Adding a new customer to the system, returning false if they already exist by
        // customer ID
        if (customers.containsKey(customer.getId())) {
            return false;
        }
        customers.put(customer.getId(), customer);
        return true;
    }

    public synchronized void registerCustomer(Customer customer) {
        // Registering a new customer in the system and adding them to the customers
        // database
        customers.put(customer.getId(), customer);
        LoggerService.getInstance().log(LoggerService.LogType.CUSTOMERS, "Registered customer: " + customer);
    }

    public synchronized void registerEmployee(User user) {
        // Registering a new employee in the system and adding them to the users
        // database
        users.put(user.getEmployeeId(), user);
        LoggerService.getInstance().log(LoggerService.LogType.EMPLOYEES, "Registered employee: " + user);
    }

    public synchronized SaleRecord processPurchase(String branchId, String empId, String custId,
            String prodId, int qty) throws OutOfStockException {
        // Processing a purchase transaction, reducing stock, calculating final price
        // based on customer type, and logging the sale record. Throws an exception if
        // the product is out of stock.
        Branch branch = branches.get(branchId);
        Product prod = products.get(prodId);
        Customer cust = customers.get(custId);

        if (branch == null || prod == null || cust == null) {
            // Throwing an exception if the branch, product, or customer ID is invalid
            throw new IllegalArgumentException("Invalid branch, product, or customer ID");
        }

        // Reducing stock in a thread-safe manner
        branch.reduceStock(prod, qty);

        // Calculating final price based on customer type
        double baseTotal = prod.getBasePrice() * qty;
        double finalPrice = cust.calculateFinalPrice(baseTotal);

        SaleRecord record = new SaleRecord(UUID.randomUUID().toString().substring(0, 8),
                branchId, empId, custId, prodId, prod.getName(), prod.getCategory(), qty, finalPrice);

        salesHistory.add(record);
        LoggerService.getInstance().log(LoggerService.LogType.TRANSACTIONS, record.toLogString());
        return record;
    }

    public List<SaleRecord> getSalesHistory() {
        // Returning a list of all sale records in the system
        return Collections.unmodifiableList(salesHistory);
    }

    public Map<String, Customer> getCustomers() {
        // Returning a map of all customers in the system
        return customers;
    }

    public Map<String, Branch> getBranches() {
        // Returning a map of all branches in the system
        return branches;
    }

    public Map<String, Product> getProducts() {
        // Returning a map of all products in the system
        return products;
    }

    public Map<String, User> getUsers() {
        // Returning a map of all users in the system
        return users;
    }
}