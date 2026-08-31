package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that collects the details of a sale and sends the purchase request to the server.
// The final price is calculated on the server according to the type of the customer.
public class ProcessSaleCommand extends MenuCommand {
    public ProcessSaleCommand() {
        super("Process Sale", false, true, new String[] {});
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Requesting the details of the sale from the user and sending the request to the server
        System.out.print("Customer ID: ");
        String custId = scanner.nextLine();
        System.out.print("Product ID: ");
        String prodId = scanner.nextLine();
        System.out.print("Quantity: ");
        String qty = scanner.nextLine();
        session.send("BUY::" + custId + "::" + prodId + "::" + qty);
    }
}
