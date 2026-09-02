package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that asks the server for the list of the customers of the whole network
public class ViewCustomersCommand extends MenuCommand {
    public ViewCustomersCommand() {
        super("View All Customers", false, true, new String[] {});
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Requesting to view all customers from the server
        session.send("GET_CUSTOMERS");
    }
}
