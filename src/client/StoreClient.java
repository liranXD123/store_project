package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class StoreClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 7000;
    private static PrintWriter out;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            out = new PrintWriter(socket.getOutputStream(), true);

            // Async background thread to listen to server messages
            Thread listener = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        handleServerMessage(msg);
                        // Refresh the input prompt
                        System.out.print("\nSelect an action (type 'menu' to see options): ");
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
            System.err.println("Server connection error: " + e.getMessage());
        }
    }

    private static void printMenu() {
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
        System.out.print("\nSelect an action: ");
    }

    private static void processUserChoice(String choice, Scanner scanner) {
        switch (choice) {
            case "1":
                System.out.print("Enter Employee ID: ");
                String empId = scanner.nextLine();
                System.out.print("Enter Password: ");
                String pass = scanner.nextLine();
                out.println("LOGIN::" + empId + "::" + pass);
                break;
            case "2":
                out.println("GET_INVENTORY");
                break;
            case "3":
                System.out.print("Customer ID: ");
                String custId = scanner.nextLine();
                System.out.print("Product ID: ");
                String prodId = scanner.nextLine();
                System.out.print("Quantity: ");
                String qty = scanner.nextLine();
                out.println("BUY::" + custId + "::" + prodId + "::" + qty);
                break;
            case "4":
                out.println("GET_CUSTOMERS");
                break;
            case "5":
                System.out.print("Enter Branch ID (e.g., B1) or ALL for network: ");
                String branchJson = scanner.nextLine();
                out.println("REPORT_JSON::" + branchJson);
                break;
            case "6":
                System.out.print("Enter Branch ID (e.g., B1) or ALL for network: ");
                String branchWord = scanner.nextLine();
                out.println("REPORT_WORD::" + branchWord);
                break;
            case "7":
                System.out.print("Enter target Branch ID for chat (e.g., B2): ");
                String target = scanner.nextLine();
                out.println("CHAT_REQUEST::" + target);
                break;
            case "8":
                System.out.print("Type message: ");
                String msg = scanner.nextLine();
                out.println("CHAT_MSG::" + msg);
                break;
            default:
                System.out.println("Invalid choice. Type 'menu' to see options.");
        }
    }

    private static void handleServerMessage(String message) {
        if (message.startsWith("LOGIN_SUCCESS::")) {
            String[] p = message.split("::");
            System.out.println("\n[SYSTEM] Login Successful! Welcome " + p[1] + " (Role: " + p[2] + ", Branch: " + p[3] + ")");
        } else if (message.startsWith("ERROR::")) {
            System.out.println("\n[ERROR] " + message.substring(7));
        } else if (message.startsWith("INVENTORY_DATA::")) {
            System.out.println("\n--- BRANCH INVENTORY ---");
            String data = message.substring(16);
            if (data.isEmpty()) { System.out.println("Inventory is empty."); return; }
            for (String row : data.split(";")) {
                if (!row.isEmpty()) {
                    String[] cols = row.split(",");
                    System.out.printf("ID: %-5s | Name: %-15s | Price: NIS %-6s | Qty: %s\n", cols[0], cols[1], cols[2], cols[3]);
                }
            }
            System.out.println("------------------------");
        } else if (message.startsWith("CUSTOMERS_DATA::")) {
            System.out.println("\n--- NETWORK CUSTOMERS ---");
            String data = message.substring(16);
            if (data.isEmpty()) { System.out.println("No customers found."); return; }
            for (String row : data.split(";")) {
                if (!row.isEmpty()) {
                    String[] cols = row.split(",");
                    System.out.printf("ID: %-5s | Name: %-15s | Phone: %-12s | Type: %s\n", cols[0], cols[1], cols[2], cols[3]);
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
            System.out.println("\n[CHAT] Conversation started with " + message.split("::")[1] + ". Use action 8 to send messages.");
        } else if (message.startsWith("CHAT_QUEUED::")) {
            System.out.println("\n[CHAT] " + message.substring(13));
        } else if (message.startsWith("REPORT_JSON_DATA::")) {
            System.out.println("\n--- JSON SALES REPORT ---\n" + message.substring(18));
            System.out.println("-------------------------");
        } else if (message.startsWith("REPORT_WORD_SUCCESS::")) {
            System.out.println("\n[SYSTEM] Report successfully exported to: " + message.split("::")[1]);
        } else {
            System.out.println("\n[SERVER] " + message);
        }
    }
}