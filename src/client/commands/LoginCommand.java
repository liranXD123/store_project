package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that asks the user for credentials and sends a login request to the server.
// It is shown only while nobody is connected through this client.
public class LoginCommand extends MenuCommand {
    public LoginCommand() {
        super("Login (LOGIN)", true, false, new String[] {});
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Requesting login credentials from the user and sending the login request to the server
        System.out.print("Enter Employee ID: ");
        String empId = scanner.nextLine();
        System.out.print("Enter Password: ");
        String pass = scanner.nextLine();
        session.send("LOGIN::" + empId + "::" + pass);
    }
}
