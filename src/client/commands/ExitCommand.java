package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that closes the session. It is available both before and after a login,
// so a user that did not manage to connect can still leave the program from the menu.
public class ExitCommand extends MenuCommand {
    public ExitCommand() {
        super("Logout & Exit", false, false, new String[] {});
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Telling the server that the session is over and asking the main loop to stop
        session.send("EXIT");
        session.logout();
        session.stop();
    }
}
