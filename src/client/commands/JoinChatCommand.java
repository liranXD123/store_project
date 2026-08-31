package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that lets a shift manager join a chat of one of the employees.
// Only a shift manager or an administrator may see this option, and the server verifies the role again.
public class JoinChatCommand extends ChatModeCommand {
    public JoinChatCommand() {
        super("[MANAGER] Join Active Chat in Branch",
                new String[] { ClientSession.ROLE_SHIFT_MANAGER, ClientSession.ROLE_ADMIN });
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Requesting the employee whose chat the manager wants to join
        System.out.print("Enter Employee ID to monitor/join (e.g. E103): ");
        String targetUser = scanner.nextLine();
        session.send("JOIN_CHAT::" + targetUser);

        runChatMode(session, scanner);
    }
}
