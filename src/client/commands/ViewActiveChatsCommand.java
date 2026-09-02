package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that shows a manager which conversations are open right now, so they can pick
// one to join. A shift manager sees their own branch, an administrator sees the whole network.
public class ViewActiveChatsCommand extends MenuCommand {
    public ViewActiveChatsCommand() {
        super("[MANAGER] View Active Chats in Branch", false, true,
                new String[] { ClientSession.ROLE_SHIFT_MANAGER, ClientSession.ROLE_ADMIN });
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Requesting the list of the open conversations from the server
        session.send("GET_ACTIVE_CHATS");
    }
}
