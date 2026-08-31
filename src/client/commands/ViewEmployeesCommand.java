package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that asks the server for the list of the employees of the network.
// Employee management belongs to the administrator screen, so only an administrator sees it.
public class ViewEmployeesCommand extends MenuCommand {
    public ViewEmployeesCommand() {
        super("[ADMIN] View All Employees", false, true, new String[] { ClientSession.ROLE_ADMIN });
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Requesting to view all employees from the server
        session.send("GET_EMPLOYEES");
    }
}
