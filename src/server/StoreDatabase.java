package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Branch;
import model.Product;
import model.Role;
import model.SaleRecord;
import model.User;
import model.customers.Customer;
import model.customers.NewCustomer;
import model.customers.ReturningCustomer;
import model.customers.VipCustomer;
import util.JsonParser;
import util.JsonWriter;

// The database of the system, kept as JSON files inside the db folder: one for the branches,
// one for the employees, one for the customers, one for the products and one for the sales.
// When the folder or one of the files does not exist yet, it is created with the
// starting data of the network, so the system can be run on a clean machine.
public class StoreDatabase {
    private static final String DB_DIR = "db";
    private static final String BRANCHES_FILE = "branches.json";
    private static final String USERS_FILE = "users.json";
    private static final String CUSTOMERS_FILE = "customers.json";
    private static final String PRODUCTS_FILE = "products.json";
    private static final String SALES_FILE = "sales.json";

    // ------------------------------------------------------------------
    // Creating the database when it is not there yet
    // ------------------------------------------------------------------

    // Making sure that the db folder exists and that each one of the three files is present.
    // A file that already exists is never touched, so the data of previous runs is kept
    public void initializeIfMissing() {
        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Created the database folder: " + dir.getPath());
        }

        // The branches are written first, because the stock inside the products file
        // is written per branch and is read back into them
        if (!fileOf(BRANCHES_FILE).exists()) {
            saveBranches(seedBranches());
            System.out.println("Created " + BRANCHES_FILE + " with the starting branches.");
        }
        if (!fileOf(USERS_FILE).exists()) {
            saveUsers(seedUsers());
            System.out.println("Created " + USERS_FILE + " with the starting employees.");
        }
        if (!fileOf(CUSTOMERS_FILE).exists()) {
            saveCustomers(seedCustomers());
            System.out.println("Created " + CUSTOMERS_FILE + " with the starting customers.");
        }
        if (!fileOf(PRODUCTS_FILE).exists()) {
            saveProductsWithStock(seedProducts(), seedStock());
            System.out.println("Created " + PRODUCTS_FILE + " with the starting products and stock.");
        }
        // A new network has not sold anything yet, so the sales file starts empty
        if (!fileOf(SALES_FILE).exists()) {
            saveSales(new ArrayList<SaleRecord>());
            System.out.println("Created " + SALES_FILE + " with an empty sales history.");
        }
    }

    // The branches the network starts with
    private Map<String, Branch> seedBranches() {
        Map<String, Branch> branches = new HashMap<String, Branch>();
        Branch b1 = new Branch("B1", "Tel Aviv");
        Branch b2 = new Branch("B2", "Haifa");
        branches.put(b1.getBranchId(), b1);
        branches.put(b2.getBranchId(), b2);
        return branches;
    }

    // The employees the network starts with
    private Map<String, User> seedUsers() {
        Map<String, User> users = new HashMap<String, User>();
        addUser(users, new User("E101", "Avi Cohen", "012345678", "050-1111111", "12-345-678", "B1",
                Role.ADMIN, "admin123"));
        addUser(users, new User("E102", "Dana Levi", "023456789", "052-2222222", "12-345-679", "B1",
                Role.SHIFT_MANAGER, "mgr123"));
        addUser(users, new User("E103", "Yossi Sharon", "034567890", "054-3333333", "12-345-680", "B2",
                Role.CASHIER, "cash123"));
        return users;
    }

    private void addUser(Map<String, User> users, User user) {
        users.put(user.getEmployeeId(), user);
    }

    // The customers the network starts with, one of every type
    private Map<String, Customer> seedCustomers() {
        Map<String, Customer> customers = new HashMap<String, Customer>();
        Customer c1 = new NewCustomer("C01", "Ronnie Kline", "301234567", "050-9999991");
        Customer c2 = new ReturningCustomer("C02", "Michal Ziv", "302345678", "050-9999992");
        Customer c3 = new VipCustomer("C03", "Alon Doron", "303456789", "050-9999993");
        customers.put(c1.getId(), c1);
        customers.put(c2.getId(), c2);
        customers.put(c3.getId(), c3);
        return customers;
    }

    // The products the network starts with
    private Map<String, Product> seedProducts() {
        Map<String, Product> products = new HashMap<String, Product>();
        Product p1 = new Product("P01", "Polo Shirt", "Shirts", 120.0);
        Product p2 = new Product("P02", "Jeans", "Pants", 250.0);
        Product p3 = new Product("P03", "Leather Jacket", "Jackets", 450.0);
        products.put(p1.getId(), p1);
        products.put(p2.getId(), p2);
        products.put(p3.getId(), p3);
        return products;
    }

    // The stock every branch starts with, as product ID -> (branch ID -> quantity)
    private Map<String, Map<String, Integer>> seedStock() {
        Map<String, Map<String, Integer>> stock = new HashMap<String, Map<String, Integer>>();

        Map<String, Integer> p1 = new HashMap<String, Integer>();
        p1.put("B1", 20);
        p1.put("B2", 10);
        stock.put("P01", p1);

        Map<String, Integer> p2 = new HashMap<String, Integer>();
        p2.put("B1", 15);
        stock.put("P02", p2);

        Map<String, Integer> p3 = new HashMap<String, Integer>();
        p3.put("B2", 8);
        stock.put("P03", p3);

        return stock;
    }

    // ------------------------------------------------------------------
    // Branches
    // ------------------------------------------------------------------

    // Reading the branches of the network. The branches come back empty of stock,
    // which is filled in afterwards by loadProducts
    public Map<String, Branch> loadBranches() {
        Map<String, Object> root = readFile(BRANCHES_FILE);
        List<Object> rows = JsonWriter.getArray(root, "branches");
        Map<String, Branch> branches = new HashMap<String, Branch>();

        if (rows == null) {
            return branches;
        }
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = asObject(rows.get(i), BRANCHES_FILE);
            Branch branch = new Branch(
                    JsonWriter.getString(row, "id"),
                    JsonWriter.getString(row, "name"));
            branches.put(branch.getBranchId(), branch);
        }
        return branches;
    }

    public void saveBranches(Map<String, Branch> branches) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"branches\": [\n");
        int written = 0;
        for (Branch b : branches.values()) {
            sb.append("    {\n");
            sb.append("      \"id\": ").append(JsonWriter.quote(b.getBranchId())).append(",\n");
            sb.append("      \"name\": ").append(JsonWriter.quote(b.getBranchName())).append("\n");
            sb.append("    }");
            written++;
            sb.append(written < branches.size() ? "," : "").append("\n");
        }
        sb.append("  ]\n}\n");
        writeFile(BRANCHES_FILE, sb.toString());
    }

    // ------------------------------------------------------------------
    // Employees
    // ------------------------------------------------------------------

    public Map<String, User> loadUsers() {
        Map<String, Object> root = readFile(USERS_FILE);
        List<Object> rows = JsonWriter.getArray(root, "users");
        Map<String, User> users = new HashMap<String, User>();

        if (rows == null) {
            return users;
        }
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = asObject(rows.get(i), USERS_FILE);
            User user = new User(
                    JsonWriter.getString(row, "employeeId"),
                    JsonWriter.getString(row, "fullName"),
                    JsonWriter.getString(row, "idNumber"),
                    JsonWriter.getString(row, "phone"),
                    JsonWriter.getString(row, "bankAccountNumber"),
                    JsonWriter.getString(row, "branchId"),
                    Role.valueOf(JsonWriter.getString(row, "role")),
                    JsonWriter.getString(row, "password"));
            users.put(user.getEmployeeId(), user);
        }
        return users;
    }

    public void saveUsers(Map<String, User> users) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"users\": [\n");
        int written = 0;
        for (User u : users.values()) {
            sb.append("    {\n");
            sb.append("      \"employeeId\": ").append(JsonWriter.quote(u.getEmployeeId())).append(",\n");
            sb.append("      \"fullName\": ").append(JsonWriter.quote(u.getFullName())).append(",\n");
            sb.append("      \"idNumber\": ").append(JsonWriter.quote(u.getIdNumber())).append(",\n");
            sb.append("      \"phone\": ").append(JsonWriter.quote(u.getPhone())).append(",\n");
            sb.append("      \"bankAccountNumber\": ").append(JsonWriter.quote(u.getBankAccountNumber())).append(",\n");
            sb.append("      \"branchId\": ").append(JsonWriter.quote(u.getBranchId())).append(",\n");
            sb.append("      \"role\": ").append(JsonWriter.quote(u.getRole().name())).append(",\n");
            sb.append("      \"password\": ").append(JsonWriter.quote(u.getPassword())).append("\n");
            sb.append("    }");
            written++;
            sb.append(written < users.size() ? "," : "").append("\n");
        }
        sb.append("  ]\n}\n");
        writeFile(USERS_FILE, sb.toString());
    }

    // ------------------------------------------------------------------
    // Customers
    // ------------------------------------------------------------------

    public Map<String, Customer> loadCustomers() {
        Map<String, Object> root = readFile(CUSTOMERS_FILE);
        List<Object> rows = JsonWriter.getArray(root, "customers");
        Map<String, Customer> customers = new HashMap<String, Customer>();

        if (rows == null) {
            return customers;
        }
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = asObject(rows.get(i), CUSTOMERS_FILE);
            String id = JsonWriter.getString(row, "id");
            String fullName = JsonWriter.getString(row, "fullName");
            String idNumber = JsonWriter.getString(row, "idNumber");
            String phone = JsonWriter.getString(row, "phone");
            String type = JsonWriter.getString(row, "type").toUpperCase();

            // The type written in the file decides which of the customer classes is created,
            // and with it the purchase track of that customer
            Customer customer;
            if (type.equals("NEW")) {
                customer = new NewCustomer(id, fullName, idNumber, phone);
            } else if (type.equals("RETURNING")) {
                customer = new ReturningCustomer(id, fullName, idNumber, phone);
            } else if (type.equals("VIP")) {
                customer = new VipCustomer(id, fullName, idNumber, phone);
            } else {
                throw new IllegalArgumentException("Unknown customer type '" + type + "' in " + CUSTOMERS_FILE);
            }
            customers.put(customer.getId(), customer);
        }
        return customers;
    }

    public void saveCustomers(Map<String, Customer> customers) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"customers\": [\n");
        int written = 0;
        for (Customer c : customers.values()) {
            sb.append("    {\n");
            sb.append("      \"id\": ").append(JsonWriter.quote(c.getId())).append(",\n");
            sb.append("      \"fullName\": ").append(JsonWriter.quote(c.getFullName())).append(",\n");
            sb.append("      \"idNumber\": ").append(JsonWriter.quote(c.getIdNumber())).append(",\n");
            sb.append("      \"phone\": ").append(JsonWriter.quote(c.getPhone())).append(",\n");
            sb.append("      \"type\": ").append(JsonWriter.quote(c.getCustomerType())).append("\n");
            sb.append("    }");
            written++;
            sb.append(written < customers.size() ? "," : "").append("\n");
        }
        sb.append("  ]\n}\n");
        writeFile(CUSTOMERS_FILE, sb.toString());
    }

    // ------------------------------------------------------------------
    // Products and the stock of every branch
    // ------------------------------------------------------------------

    // Reading the products and putting the stock that was saved for each branch back into the branches
    public Map<String, Product> loadProducts(Map<String, Branch> branches) {
        Map<String, Object> root = readFile(PRODUCTS_FILE);
        List<Object> rows = JsonWriter.getArray(root, "products");
        Map<String, Product> products = new HashMap<String, Product>();

        if (rows == null) {
            return products;
        }
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = asObject(rows.get(i), PRODUCTS_FILE);
            Product product = new Product(
                    JsonWriter.getString(row, "id"),
                    JsonWriter.getString(row, "name"),
                    JsonWriter.getString(row, "category"),
                    JsonWriter.getDouble(row, "basePrice"));
            products.put(product.getId(), product);

            // The quantity of this product in every branch
            Map<String, Object> stock = JsonWriter.getObject(row, "stock");
            if (stock == null) {
                continue;
            }
            for (Map.Entry<String, Object> entry : stock.entrySet()) {
                Branch branch = branches.get(entry.getKey());
                if (branch == null) {
                    // Stock of a branch that is no longer part of the network is simply skipped
                    continue;
                }
                branch.addStock(product, JsonWriter.getInt(stock, entry.getKey()));
            }
        }
        return products;
    }

    // Saving the products together with the current stock of every branch
    public void saveProducts(Map<String, Product> products, Map<String, Branch> branches) {
        saveProductsWithStock(products, collectStock(products, branches));
    }

    // Building the map of product ID -> (branch ID -> quantity) out of the branches
    private Map<String, Map<String, Integer>> collectStock(Map<String, Product> products,
            Map<String, Branch> branches) {
        Map<String, Map<String, Integer>> stock = new HashMap<String, Map<String, Integer>>();
        for (String productId : products.keySet()) {
            stock.put(productId, new HashMap<String, Integer>());
        }

        for (Branch branch : branches.values()) {
            Map<Product, Integer> inventory = branch.getInventorySnapshot();
            for (Map.Entry<Product, Integer> entry : inventory.entrySet()) {
                Map<String, Integer> perBranch = stock.get(entry.getKey().getId());
                if (perBranch != null) {
                    perBranch.put(branch.getBranchId(), entry.getValue());
                }
            }
        }
        return stock;
    }

    private void saveProductsWithStock(Map<String, Product> products, Map<String, Map<String, Integer>> stock) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"products\": [\n");
        int written = 0;
        for (Product p : products.values()) {
            sb.append("    {\n");
            sb.append("      \"id\": ").append(JsonWriter.quote(p.getId())).append(",\n");
            sb.append("      \"name\": ").append(JsonWriter.quote(p.getName())).append(",\n");
            sb.append("      \"category\": ").append(JsonWriter.quote(p.getCategory())).append(",\n");
            sb.append("      \"basePrice\": ").append(p.getBasePrice()).append(",\n");
            sb.append("      \"stock\": {");

            Map<String, Integer> perBranch = stock.get(p.getId());
            if (perBranch != null && !perBranch.isEmpty()) {
                sb.append("\n");
                int stockWritten = 0;
                for (Map.Entry<String, Integer> entry : perBranch.entrySet()) {
                    sb.append("        ").append(JsonWriter.quote(entry.getKey())).append(": ")
                            .append(entry.getValue());
                    stockWritten++;
                    sb.append(stockWritten < perBranch.size() ? "," : "").append("\n");
                }
                sb.append("      ");
            }
            sb.append("}\n");
            sb.append("    }");
            written++;
            sb.append(written < products.size() ? "," : "").append("\n");
        }
        sb.append("  ]\n}\n");
        writeFile(PRODUCTS_FILE, sb.toString());
    }

    // ------------------------------------------------------------------
    // Sales history
    // ------------------------------------------------------------------

    // Reading every sale that was ever made, keeping the time each one happened,
    // so the reports still cover the days before the server was restarted
    public List<SaleRecord> loadSales() {
        Map<String, Object> root = readFile(SALES_FILE);
        List<Object> rows = JsonWriter.getArray(root, "sales");
        List<SaleRecord> sales = new ArrayList<SaleRecord>();

        if (rows == null) {
            return sales;
        }
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = asObject(rows.get(i), SALES_FILE);
            sales.add(new SaleRecord(
                    JsonWriter.getString(row, "transactionId"),
                    JsonWriter.getString(row, "branchId"),
                    JsonWriter.getString(row, "employeeId"),
                    JsonWriter.getString(row, "customerId"),
                    JsonWriter.getString(row, "productId"),
                    JsonWriter.getString(row, "productName"),
                    JsonWriter.getString(row, "category"),
                    JsonWriter.getInt(row, "quantity"),
                    JsonWriter.getDouble(row, "finalPrice"),
                    LocalDateTime.parse(JsonWriter.getString(row, "timestamp"))));
        }
        return sales;
    }

    public void saveSales(List<SaleRecord> sales) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"sales\": [\n");
        for (int i = 0; i < sales.size(); i++) {
            SaleRecord s = sales.get(i);
            sb.append("    {\n");
            sb.append("      \"transactionId\": ").append(JsonWriter.quote(s.getTransactionId())).append(",\n");
            sb.append("      \"branchId\": ").append(JsonWriter.quote(s.getBranchId())).append(",\n");
            sb.append("      \"employeeId\": ").append(JsonWriter.quote(s.getEmployeeId())).append(",\n");
            sb.append("      \"customerId\": ").append(JsonWriter.quote(s.getCustomerId())).append(",\n");
            sb.append("      \"productId\": ").append(JsonWriter.quote(s.getProductId())).append(",\n");
            sb.append("      \"productName\": ").append(JsonWriter.quote(s.getProductName())).append(",\n");
            sb.append("      \"category\": ").append(JsonWriter.quote(s.getCategory())).append(",\n");
            sb.append("      \"quantity\": ").append(s.getQuantity()).append(",\n");
            sb.append("      \"finalPrice\": ").append(s.getFinalPrice()).append(",\n");
            sb.append("      \"timestamp\": ").append(JsonWriter.quote(s.getTimestamp().toString())).append("\n");
            sb.append("    }");
            sb.append(i < sales.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n}\n");
        writeFile(SALES_FILE, sb.toString());
    }

    // ------------------------------------------------------------------
    // Reading and writing the files themselves
    // ------------------------------------------------------------------

    private File fileOf(String fileName) {
        return new File(DB_DIR + File.separator + fileName);
    }

    // Reading a whole file and returning the object that was written in it
    private Map<String, Object> readFile(String fileName) {
        File file = fileOf(fileName);
        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Object value = JsonParser.parse(text);
            if (!(value instanceof Map)) {
                throw new IllegalArgumentException("The file does not hold a JSON object");
            }
            return asObject(value, fileName);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the database file " + file.getPath()
                    + ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("The database file " + file.getPath() + " is damaged: "
                    + e.getMessage());
        }
    }

    private void writeFile(String fileName, String content) {
        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = fileOf(fileName);
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            pw.print(content);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write the database file " + file.getPath()
                    + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asObject(Object value, String fileName) {
        if (!(value instanceof Map)) {
            throw new IllegalStateException("The database file " + fileName + " holds a record that is not an object");
        }
        return (Map<String, Object>) value;
    }
}
