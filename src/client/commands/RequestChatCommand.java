package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that asks the server to open a chat with an employee of another branch
public class RequestChatCommand extends MenuCommand {
    public RequestChatCommand() {
        super("Request Chat with another branch", false, true, new String[] {});
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Requesting the branch with which the user wants to open a chat
        System.out.print("Enter target Branch ID for chat (e.g., B2): ");
        String target = scanner.nextLine();
        session.send("CHAT_REQUEST::" + target);
    }
}
