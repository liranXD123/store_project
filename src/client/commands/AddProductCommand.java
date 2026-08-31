package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that adds a product the network did not sell until now.
// The product joins the catalogue of the whole network, while the quantity that is entered
// here is the amount that arrived into the branch of the employee adding it.
public class AddProductCommand extends MenuCommand {
    public AddProductCommand() {
        super("[MANAGER] Add New Product", false, true,
                new String[] { ClientSession.ROLE_SHIFT_MANAGER, ClientSession.ROLE_ADMIN });
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Requesting the details of the new product from the user
        System.out.print("Enter Product ID (e.g. P04): ");
        String prodId = scanner.nextLine();
        System.out.print("Enter Product Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter Category (e.g. Shirts): ");
        String category = scanner.nextLine();
        System.out.print("Enter Base Price (e.g. 149.90): ");
        String price = scanner.nextLine();
        System.out.print("Enter starting quantity for your branch (0 for none): ");
        String qty = scanner.nextLine();
        session.send("ADD_PRODUCT::" + prodId + "::" + name + "::" + category + "::" + price + "::" + qty);
    }
}
