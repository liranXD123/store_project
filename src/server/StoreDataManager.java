package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import exceptions.AuthenticationException;
import exceptions.OutOfStockException;
import model.Branch;
import model.Product;
import model.SaleRecord;
import model.User;
import model.customers.Customer;

// Singleton class responsible for managing store data, including users, branches, customers,
// products and sales history. The employees, the customers and the products are kept in the
// JSON files of the db folder, so they survive a restart of the server: they are read once
// when this object is created, and written again after every change.
public class StoreDataManager {
    private static StoreDataManager instance;

    // The files that hold the data of the system
    private final StoreDatabase database = new StoreDatabase();

    // All the collections are reached only through the synchronized methods of this class,
    // and every method that hands data out returns a copy, so no caller can iterate over a
    // collection while another thread is changing it
    private final Map<String, User> users = new HashMap<String, User>();
    private final Map<String, Branch> branches = new HashMap<String, Branch>();
    private final Map<String, Customer> customers = new HashMap<String, Customer>();
    private final Map<String, Product> products = new HashMap<String, Product>();
    private final List<SaleRecord> salesHistory = new ArrayList<SaleRecord>();

    private StoreDataManager() {
        // Creating the database files when they are not there yet, and reading them into memory
        database.initializeIfMissing();
        loadFromDatabase();
    }

    public static synchronized StoreDataManager getInstance() {
        // Creating a single instance of StoreDataManager (Singleton Pattern) to ensure
        // that all parts of the application access the same data manager instance
        if (instance == null) {
            instance = new StoreDataManager();
        }
        return instance;
    }

    // Reading the saved data into memory. The branches are read first, because the stock
    // of every product is saved per branch and has to be put back into an existing branch
    private void loadFromDatabase() {
        branches.putAll(database.loadBranches());
        users.putAll(database.loadUsers());
        customers.putAll(database.loadCustomers());
        // The products carry the stock of every branch, which is put back into the branches
        products.putAll(database.loadProducts(branches));
        salesHistory.addAll(database.loadSales());

        System.out.println("Database loaded: " + branches.size() + " branches, " + users.size()
                + " employees, " + customers.size() + " customers, " + products.size() + " products, "
                + salesHistory.size() + " sales.");
    }

    public synchronized User authenticate(String employeeId, String password) throws AuthenticationException {
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
        database.saveUsers(users);
        return true;
    }

    public synchronized boolean addCustomer(Customer customer) {
        // Adding a new customer to the system, returning false if they already exist by
        // customer ID
        if (customers.containsKey(customer.getId())) {
            return false;
        }
        customers.put(customer.getId(), customer);
        database.saveCustomers(customers);
        return true;
    }

    // Adding stock of an existing product to a branch, which is the purchase side of the
    // inventory management (buying goods in, as opposed to selling them to a customer)
    public synchronized void restockProduct(String branchId, String prodId, int qty) {
        Branch branch = branches.get(branchId);
        Product prod = products.get(prodId);

        if (branch == null || prod == null) {
            throw new IllegalArgumentException("Invalid branch or product ID");
        }
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        branch.addStock(prod, qty);
        // The stock is part of the products file, so it is written after every change
        database.saveProducts(products, branches);
        LoggerService.getInstance().log(LoggerService.LogType.TRANSACTIONS,
                "Restock | Branch: " + branchId + " | Item: " + prod.getName() + " (x" + qty + ")");
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

        // Reducing stock in a thread-safe manner, and writing the new stock to the products file
        branch.reduceStock(prod, qty);
        database.saveProducts(products, branches);

        // Calculating final price based on customer type
        double baseTotal = prod.getBasePrice() * qty;
        double finalPrice = cust.calculateFinalPrice(baseTotal);

        SaleRecord record = new SaleRecord(UUID.randomUUID().toString().substring(0, 8),
                branchId, empId, custId, prodId, prod.getName(), prod.getCategory(), qty, finalPrice);

        salesHistory.add(record);
        // The sale joins the history that the reports are built from, so it is written to the file too
        database.saveSales(salesHistory);
        LoggerService.getInstance().log(LoggerService.LogType.TRANSACTIONS, record.toLogString());
        return record;
    }

    public synchronized List<SaleRecord> getSalesHistory() {
        // Returning a copy of all the sale records, so the caller may go over it safely
        return new ArrayList<SaleRecord>(salesHistory);
    }

    public synchronized Map<String, Customer> getCustomers() {
        // Returning a copy of the map of all customers in the system
        return new HashMap<String, Customer>(customers);
    }

    public synchronized Branch getBranch(String branchId) {
        // Returning a single branch by its ID, or null when no such branch exists
        return branches.get(branchId);
    }

    public synchronized boolean branchExists(String branchId) {
        // Checking whether a branch with the given ID is defined in the system
        return branches.containsKey(branchId);
    }

    public synchronized Map<String, Product> getProducts() {
        // Returning a copy of the map of all products in the system
        return new HashMap<String, Product>(products);
    }

    public synchronized Map<String, User> getUsers() {
        // Returning a copy of the map of all users in the system
        return new HashMap<String, User>(users);
    }
}