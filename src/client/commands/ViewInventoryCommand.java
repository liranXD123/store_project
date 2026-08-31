package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Command that asks the server for the inventory of the branch the connected user belongs to
public class ViewInventoryCommand extends MenuCommand {
    public ViewInventoryCommand() {
        super("View Branch Inventory", false, true, new String[] {});
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Requesting to view branch inventory from the server
        session.send("GET_INVENTORY");
    }
}
