package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class StoreClient {
      //  Defining server connection parameters and initializing variables for communication and user state
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 7000;
    private static PrintWriter out;
    private static boolean inChatMode = false;
    private static String currentUserRole = "";

    public static void main(String[] args) {
        // Establishing a connection to the server and setting up input/output streams for communication
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Scanner scanner = new Scanner(System.in)) {

            out = new PrintWriter(socket.getOutputStream(), true);

            // Creating a Thread to listen for messages from the server and print them to the screen
            Thread listener = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        handleServerMessage(msg);
                        // Refresh the input prompt based on mode
                        if (inChatMode) {
                            System.out.print("\n[Chat Mode] Type message (or '/exit'): ");
                        } else {
                            System.out.print("\nSelect an action (type 'menu' to see options): ");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("\n[SYSTEM] Disconnected from server.");
                }
            });
            listener.setDaemon(true);
            listener.start();

            System.out.println("=== Welcome to the Store Management System (Console Mode) ===");
            printMenu();

            // Main user loop
            while (true) {
                String choice = scanner.nextLine().trim();
                // Checking if the user wants to exit the system or view the menu
                if (choice.equals("9") || choice.equalsIgnoreCase("exit")) {
                    out.println("EXIT");
                    break;
                } else if (choice.equalsIgnoreCase("menu")) {
                    printMenu();
                } else {
                    processUserChoice(choice, scanner);
                }
            }

        } catch (Exception e) {
            // Handling exceptions that may occur during server connection or communication
            System.err.println("Server connection error: " + e.getMessage());
        }
    }

    private static void printMenu() {
        // Displaying the main menu for the user
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("1. Login (LOGIN)");
        System.out.println("2. View Branch Inventory");
        System.out.println("3. Process Sale");
        System.out.println("4. View All Customers");
        System.out.println("5. Generate JSON Report (Branch/ALL)");
        System.out.println("6. Export Word Report (Branch/ALL)");
        System.out.println("7. Request Chat with another branch");
        System.out.println("8. Send Chat Message");
        System.out.println("9. Logout & Exit");
        if ("ADMIN".equals(currentUserRole)) {
            System.out.println("10. [ADMIN] Add New Employee");
        }
        System.out.println("11. Add New Customer");
        if ("SHIFT_MANAGER".equals(currentUserRole) || "ADMIN".equals(currentUserRole)) {
            System.out.println("12. [MANAGER] Join Active Chat in Branch");
        }
        System.out.print("\nSelect an action: ");
    }

    private static void processUserChoice(String choice, Scanner scanner) {
        switch (choice) {
            // Handling user choice based on the selected action number
            case "1":
                // Requesting login credentials from the user and sending the login request to the server
                System.out.print("Enter Employee ID: ");
                String empId = scanner.nextLine();
                System.out.print("Enter Password: ");
                String pass = scanner.nextLine();
                out.println("LOGIN::" + empId + "::" + pass);
                break;
            case "2":
                // Requesting to view branch inventory from the server
                out.println("GET_INVENTORY");
                break;
            case "3":
                // Requesting to process a sale from the user and sending the request to the server
                System.out.print("Customer ID: ");
                String custId = scanner.nextLine();
                System.out.print("Product ID: ");
                String prodId = scanner.nextLine();
                System.out.print("Quantity: ");
                String qty = scanner.nextLine();
                out.println("BUY::" + custId + "::" + prodId + "::" + qty);
                break;
            case "4":
                // Requesting to view all customers from the server
                out.println("GET_CUSTOMERS");
                break;
            case "5":
            case "6":
                // Requesting to generate a JSON or Word report from the user and sending the request to the server
                System.out.println("Select Filter Type:");
                System.out.println("1. ALL (Entire Network)");
                System.out.println("2. BRANCH (Filter by Branch ID)");
                System.out.println("3. PRODUCT (Filter by Product ID)");
                System.out.println("4. CATEGORY (Filter by Category Name)");
                System.out.print("Your choice (1-4): ");
                String filterChoice = scanner.nextLine();
                
                String filterType = "ALL";
                String filterValue = "ALL";
                
                if (filterChoice.equals("2")) {
                    // Requesting the branch ID from the user
                    filterType = "BRANCH";
                    System.out.print("Enter Branch ID (e.g., B1): ");
                    filterValue = scanner.nextLine();
                } else if (filterChoice.equals("3")) {
                    // Requesting the product ID from the user
                    filterType = "PRODUCT";
                    System.out.print("Enter Product ID (e.g., P01): ");
                    filterValue = scanner.nextLine();
                } else if (filterChoice.equals("4")) {
                    // Requesting the category name from the user
                    filterType = "CATEGORY";
                    System.out.print("Enter Category (e.g., Shirts): ");
                    filterValue = scanner.nextLine();
                }
                // Sending the command to the server based on the user's choice (JSON or Word report) with type and filter
                String command = choice.equals("5") ? "REPORT_JSON" : "REPORT_WORD";
                out.println(command + "::" + filterType + "::" + filterValue);
                break;
            case "7":
                // Requesting the branch ID to which the user wants to send a chat message
                System.out.print("Enter target Branch ID for chat (e.g., B2): ");
                String target = scanner.nextLine();
                out.println("CHAT_REQUEST::" + target);
                break;
            case "8":
                // Allowing the user to enter chat mode to send messages directly, with an option to exit back to the main menu
                inChatMode = true;
                System.out.println(
                        "\n--- Entered Chat Mode. Type your messages directly. Type '/exit' to return to main menu. ---");
                System.out.print("[Chat Mode] Type message (or '/exit'): ");
                
                    // Loop to allow the user to send chat messages until they choose to exit chat mode
                while (true) {
                    String msg = scanner.nextLine();

                    // Checking if the user wants to exit chat mode and updating the server accordingly
                    if (msg.equalsIgnoreCase("/exit")) {
                        /// Updating the server that the user has left chat mode
                        out.println("LEAVE_CHAT");
                        inChatMode = false;
                        System.out.println("\n--- Exited Chat Mode ---");
                        System.out.print("\nSelect an action (type 'menu' to see options): ");
                        break;
                    }

                    // Sending the message to the server
                    out.println("CHAT_MSG::" + msg);
                }
                break;
            case "10":
                // Requesting the user to enter new employee details and adding them to the system, but only if the current user is an admin (ADMIN)
                if (!"ADMIN".equals(currentUserRole)) {
                    System.out.println("Permission denied. Admins only.");
                    break;
                }
                // Requesting the user to enter the details of the new employee and sending the command to the server to add them
                System.out.print("Enter New Employee ID (e.g. E104): ");
                String newEmpId = scanner.nextLine();
                System.out.print("Enter Full Name: ");
                String name = scanner.nextLine();
                System.out.print("Enter ID Number (T.Z): ");
                String tz = scanner.nextLine();
                System.out.print("Enter Phone: ");
                String phone = scanner.nextLine();
                System.out.print("Enter Bank Account: ");
                String bank = scanner.nextLine();
                System.out.print("Enter Branch (e.g. B1): ");
                String branch = scanner.nextLine();
                System.out.print("Enter Role (ADMIN/SHIFT_MANAGER/CASHIER): ");
                String role = scanner.nextLine();
                System.out.print("Enter Password (must be at least 6 chars): ");
                String pwd = scanner.nextLine();
                out.println("ADD_EMP::" + newEmpId + "::" + name + "::" + tz + "::" + phone + "::" + bank + "::"
                        + branch + "::" + role + "::" + pwd);
                break;
            case "11":
                // Requesting the user to enter new customer details and adding them to the system
                System.out.print("Enter Customer ID (e.g. C04): ");
                String cId = scanner.nextLine();
                System.out.print("Enter Full Name: ");
                String cName = scanner.nextLine();
                System.out.print("Enter Phone: ");
                String cPhone = scanner.nextLine();
                System.out.print("Enter Customer Type (NEW / RETURNING / VIP): ");
                String cType = scanner.nextLine();
                out.println("ADD_CUSTOMER::" + cId + "::" + cName + "::" + cPhone + "::" + cType);
                break;
            case "12":
                // Requesting the user to join an active chat session in a different branch, but only if the current user is a manager (SHIFT_MANAGER or ADMIN)
                if (!"SHIFT_MANAGER".equals(currentUserRole) && !"ADMIN".equals(currentUserRole)) {
                    System.out.println("Permission denied. Managers only.");
                    break;
                }
                // Correction: Requesting the employee ID to which they want to listen
                System.out.print("Enter Employee ID to monitor/join (e.g. E103): ");
                String targetUser = scanner.nextLine();
                out.println("JOIN_CHAT::" + targetUser);
                
                inChatMode = true;
                System.out.println("\n--- Entered Chat Mode. Type your messages directly. Type '/exit' to return to main menu. ---");
                System.out.print("[Chat Mode] Type message (or '/exit'): ");
                
                while (true) {
                    // Loop to allow the user to send chat messages until they choose to exit chat mode
                    String msg = scanner.nextLine();
                    if (msg.equalsIgnoreCase("/exit")) {
                        inChatMode = false;
                        out.println("LEAVE_CHAT"); // Updating the server that the user has left
                        System.out.println("\n--- Exited Chat Mode ---");
                        System.out.print("\nSelect an action (type 'menu' to see options): ");
                        break;
                    }
                    out.println("CHAT_MSG::" + msg);
                }
                break;
            default:
                System.out.println("Invalid choice. Type 'menu' to see options.");
        }
    }

    private static void handleServerMessage(String message) {
        // Handling messages received from the server and displaying them to the user accordingly
        if (message.startsWith("LOGIN_SUCCESS::")) {
            String[] p = message.split("::");
            currentUserRole = p[2];
            System.out.println(
                    "\n[SYSTEM] Login Successful! Welcome " + p[1] + " (Role: " + p[2] + ", Branch: " + p[3] + ")");
        } else if (message.startsWith("ERROR::")) {
            System.out.println("\n[ERROR] " + message.substring(7));
        } else if (message.startsWith("INVENTORY_DATA::")) {
            System.out.println("\n--- BRANCH INVENTORY ---");
            String data = message.substring(16);
            if (data.isEmpty()) {
                System.out.println("Inventory is empty.");
                return;
            }
            for (String row : data.split(";")) {
                if (!row.isEmpty()) {
                    String[] cols = row.split(",");
                    System.out.printf("ID: %-5s | Name: %-15s | Price: NIS %-6s | Qty: %s\n", cols[0], cols[1], cols[2],
                            cols[3]);
                }
            }
            System.out.println("------------------------");
        } else if (message.startsWith("CUSTOMERS_DATA::")) {
            System.out.println("\n--- NETWORK CUSTOMERS ---");
            String data = message.substring(16);
            if (data.isEmpty()) {
                System.out.println("No customers found.");
                return;
            }
            for (String row : data.split(";")) {
                if (!row.isEmpty()) {
                    String[] cols = row.split(",");
                    System.out.printf("ID: %-5s | Name: %-15s | Phone: %-12s | Type: %s\n", cols[0], cols[1], cols[2],
                            cols[3]);
                }
            }
            System.out.println("-------------------------");
        } else if (message.startsWith("BUY_SUCCESS::")) {
            String[] p = message.split("::");
            System.out.println("\n[POS] Sale completed successfully! Trans ID: " + p[1] + ", Final Price: NIS " + p[2]);
        } else if (message.equals("INVENTORY_UPDATED")) {
            System.out.println("\n[SERVER ALERT] Branch inventory updated remotely.");
        } else if (message.startsWith("CHAT_INCOMING::")) {
            String[] p = message.split("::");
            System.out.println("\n[CHAT from " + p[1] + "]: " + p[2]);
        } else if (message.startsWith("CHAT_STARTED::")) {
            System.out.println("\n[CHAT] Conversation started with " + message.split("::")[1]
                    + ". Use action 8 to send messages.");
        } else if (message.startsWith("CHAT_QUEUED::")) {
            System.out.println("\n[CHAT] " + message.substring(13));
        } else if (message.startsWith("REPORT_JSON_DATA::")) {
            System.out.println("\n--- JSON SALES REPORT ---\n" + message.substring(18));
            System.out.println("-------------------------");
        } else if (message.startsWith("ADD_EMP_SUCCESS::")) {
            System.out.println("\n[ADMIN] Employee added successfully: " + message.split("::")[1]);
        } else if (message.startsWith("REPORT_WORD_SUCCESS::")) {
            System.out.println("\n[SYSTEM] Report successfully exported to: " + message.split("::")[1]);
        } else if (message.startsWith("ADD_CUSTOMER_SUCCESS::")) {
            System.out.println("\n[SYSTEM] Customer added successfully: " + message.split("::")[1]);
        } else {
            System.out.println("\n[SERVER] " + message);
        }
    }
}