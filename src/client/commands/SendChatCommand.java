package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that moves the client into chat mode so the user can type messages directly
public class SendChatCommand extends ChatModeCommand {
    public SendChatCommand() {
        super("Send Chat Message", new String[] {});
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        runChatMode(session, scanner);
    }
}
