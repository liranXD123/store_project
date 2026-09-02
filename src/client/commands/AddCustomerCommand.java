package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that collects the details of a new customer and registers them in the network
public class AddCustomerCommand extends MenuCommand {
    public AddCustomerCommand() {
        super("Add New Customer", false, true, new String[] {});
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Requesting the user to enter new customer details and adding them to the system
        System.out.print("Enter Customer ID (e.g. C04): ");
        String cId = scanner.nextLine();
        System.out.print("Enter Full Name: ");
        String cName = scanner.nextLine();
        System.out.print("Enter ID Number (T.Z): ");
        String cTz = scanner.nextLine();
        System.out.print("Enter Phone: ");
        String cPhone = scanner.nextLine();
        System.out.print("Enter Customer Type (NEW / RETURNING / VIP): ");
        String cType = scanner.nextLine();
        session.send("ADD_CUSTOMER::" + cId + "::" + cName + "::" + cTz + "::" + cPhone + "::" + cType);
    }
}
