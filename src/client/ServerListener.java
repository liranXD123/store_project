package client;

import java.io.BufferedReader;
import java.io.IOException;

// Class that listens to the messages arriving from the server and prints them to the screen.
// It implements Runnable so that it can run in a thread of its own, next to the thread
// that reads the commands of the user from the console.
public class ServerListener implements Runnable {
    private BufferedReader in;
    private ClientSession session;
    private ClientMenu menu;

    public ServerListener(BufferedReader in, ClientSession session, ClientMenu menu) {
        this.in = in;
        this.session = session;
        this.menu = menu;
    }

    @Override
    public void run() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                handleServerMessage(msg);
                // Refresh the input prompt based on mode
                if (session.isInChatMode()) {
                    System.out.println("\n[Chat Mode] Type message (or '/exit'): ");
                } else {
                    System.out.println("\nSelect an action (type 'menu' to see options): ");
                }
            }
        } catch (IOException e) {
            System.out.println("\n[SYSTEM] Disconnected from server.");
        }
    }

    private void handleServerMessage(String message) {
        // Handling messages received from the server and displaying them to the user accordingly
        if (message.startsWith("LOGIN_SUCCESS::")) {
            String[] p = message.split("::");
            session.login(p[1], p[2], p[3]);
            System.out.println(
                    "\n[SYSTEM] Login Successful! Welcome " + p[1] + " (Role: " + p[2] + ", Branch: " + p[3] + ")");
            // Reprinting the menu because after the login the user is allowed to use more options
            menu.printMenuItems(session);
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
                    System.out.printf("ID: %-5s | Name: %-15s | T.Z: %-10s | Phone: %-12s | Type: %s\n",
                            cols[0], cols[1], cols[2], cols[3], cols[4]);
                }
            }
            System.out.println("-------------------------");
        } else if (message.startsWith("EMPLOYEES_DATA::")) {
            System.out.println("\n--- NETWORK EMPLOYEES ---");
            String data = message.substring(16);
            if (data.isEmpty()) {
                System.out.println("No employees found.");
                return;
            }
            for (String row : data.split(";")) {
                if (!row.isEmpty()) {
                    String[] cols = row.split(",");
                    System.out.printf("ID: %-5s | Name: %-15s | T.Z: %-10s | Phone: %-12s | Bank: %-12s | Branch: %-4s | Role: %s\n",
                            cols[0], cols[1], cols[2], cols[3], cols[4], cols[5], cols[6]);
                }
            }
            System.out.println("-------------------------");
        } else if (message.startsWith("ACTIVE_CHATS_DATA::")) {
            System.out.println("\n--- ACTIVE CHATS ---");
            String data = message.substring(19);
            if (data.isEmpty()) {
                System.out.println("No chat is open right now.");
                return;
            }
            for (String row : data.split(";")) {
                if (!row.isEmpty()) {
                    String[] cols = row.split(",");
                    System.out.printf("Employee: %-5s | Name: %-15s | Branch: %-4s | Talking with: %s\n",
                            cols[0], cols[1], cols[2], cols[3]);
                }
            }
            System.out.println("--------------------");
        } else if (message.startsWith("RESTOCK_SUCCESS::")) {
            String[] p = message.split("::");
            System.out.println("\n[STOCK] Added " + p[2] + " units of product " + p[1] + " to your branch.");
        } else if (message.equals("CUSTOMERS_UPDATED")) {
            System.out.println("\n[SERVER ALERT] Network customer list updated.");
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
                    + ". Use action " + menu.getSendChatNumber(session) + " to send messages.");
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
