package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Abstract command for the options that put the client into chat mode.
// The loop that reads the messages of the user and sends them to the server is
// identical for all of them, so it is written here once.
public abstract class ChatModeCommand extends MenuCommand {
    public ChatModeCommand(String label, String[] allowedRoles) {
        super(label, false, true, allowedRoles);
    }

    // Reading messages from the user and sending them to the server until the user asks to leave
    protected void runChatMode(ClientSession session, Scanner scanner) {
        session.setInChatMode(true);
        System.out.println(
                "\n--- Entered Chat Mode. Type your messages directly. Type '/exit' to return to main menu. ---");
        System.out.print("[Chat Mode] Type message (or '/exit'): ");

        while (true) {
            String msg = scanner.nextLine();

            // Checking if the user wants to exit chat mode and updating the server accordingly
            if (msg.equalsIgnoreCase("/exit")) {
                session.send("LEAVE_CHAT");
                session.setInChatMode(false);
                System.out.println("\n--- Exited Chat Mode ---");
                System.out.print("\nSelect an action (type 'menu' to see options): ");
                break;
            }

            // Sending the message to the server
            session.send("CHAT_MSG::" + msg);
        }
    }
}
