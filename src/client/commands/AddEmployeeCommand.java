package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that collects the details of a new employee and registers them in the system.
// Only an administrator may see this option, and the server verifies the role again.
public class AddEmployeeCommand extends MenuCommand {
    public AddEmployeeCommand() {
        super("[ADMIN] Add New Employee", false, true, new String[] { ClientSession.ROLE_ADMIN });
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Requesting the user to enter the details of the new employee and sending the command to the server to add them
        System.out.print("Enter New Employee ID (e.g. E104): ");
        String newEmpId = scanner.nextLine();
        System.out.print("Enter Full Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter ID Number (T.Z): ");
        String tz = scanner.nextLine();
        System.out.print("Enter Phone: ");
        String phone = scanner.nextLine();
        System.out.print("Enter Bank Account: ");
        String bank = scanner.nextLine();
        System.out.print("Enter Branch (e.g. B1): ");
        String branch = scanner.nextLine();
        System.out.print("Enter Role (ADMIN/SHIFT_MANAGER/CASHIER/SELLER): ");
        String role = scanner.nextLine();
        System.out.print("Enter Password (at least 6 chars, with an upper case letter, a lower case letter and a digit): ");
        String pwd = scanner.nextLine();
        session.send("ADD_EMP::" + newEmpId + "::" + name + "::" + tz + "::" + phone + "::" + bank + "::"
                + branch + "::" + role + "::" + pwd);
    }
}
