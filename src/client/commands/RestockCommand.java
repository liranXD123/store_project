package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that buys goods into the branch, which is the purchase side of the inventory
// management. Only a shift manager or an administrator may see this option.
public class RestockCommand extends MenuCommand {
    public RestockCommand() {
        super("[MANAGER] Restock Product (buy into branch)", false, true,
                new String[] { ClientSession.ROLE_SHIFT_MANAGER, ClientSession.ROLE_ADMIN });
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Requesting the product and the amount that arrived into the branch
        System.out.print("Product ID (e.g. P01): ");
        String prodId = scanner.nextLine();
        System.out.print("Quantity to add: ");
        String qty = scanner.nextLine();
        session.send("RESTOCK::" + prodId + "::" + qty);
    }
}
