package client;

import java.util.Vector;

import client.commands.AddCustomerCommand;
import client.commands.AddEmployeeCommand;
import client.commands.AddProductCommand;
import client.commands.ExitCommand;
import client.commands.JoinChatCommand;
import client.commands.JsonReportCommand;
import client.commands.LoginCommand;
import client.commands.MenuCommand;
import client.commands.ProcessSaleCommand;
import client.commands.RequestChatCommand;
import client.commands.RestockCommand;
import client.commands.SendChatCommand;
import client.commands.ViewActiveChatsCommand;
import client.commands.ViewCustomersCommand;
import client.commands.ViewEmployeesCommand;
import client.commands.ViewInventoryCommand;
import client.commands.WordReportCommand;

// Class holding all the commands of the client and presenting them as a menu.
// The options that the current user is not allowed to use are not printed at all,
// and the remaining ones are numbered one after the other so no number is ever skipped.
public class ClientMenu {
    private Vector<MenuCommand> commands;

    public ClientMenu() {
        this.commands = buildCommands();
    }

    // Building the collection of all the commands, in the order in which they are displayed
    private Vector<MenuCommand> buildCommands() {
        Vector<MenuCommand> list = new Vector<MenuCommand>();
        list.add(new LoginCommand());
        list.add(new ViewInventoryCommand());
        list.add(new ProcessSaleCommand());
        list.add(new ViewCustomersCommand());
        list.add(new AddCustomerCommand());
        list.add(new JsonReportCommand());
        list.add(new WordReportCommand());
        list.add(new RequestChatCommand());
        list.add(new SendChatCommand());
        list.add(new AddProductCommand());
        list.add(new RestockCommand());
        list.add(new ViewActiveChatsCommand());
        list.add(new JoinChatCommand());
        list.add(new ViewEmployeesCommand());
        list.add(new AddEmployeeCommand());
        list.add(new ExitCommand());
        return list;
    }

    // Building a collection that holds only the commands the current session is allowed to see
    public Vector<MenuCommand> getVisibleCommands(ClientSession session) {
        Vector<MenuCommand> visible = new Vector<MenuCommand>();
        for (int i = 0; i < commands.size(); i++) {
            if (commands.get(i).isVisible(session))
                visible.add(commands.get(i));
        }
        return visible;
    }

    // Displaying the menu followed by the input prompt
    public void printMenu(ClientSession session) {
        printMenuItems(session);
        System.out.print("\nSelect an action: ");
    }

    // Displaying the visible commands, numbered one after the other
    public void printMenuItems(ClientSession session) {
        System.out.println("\n--- MAIN MENU ---");
        if (!session.isLoggedIn())
            System.out.println("(You are not logged in. Please log in to use the system.)");
        Vector<MenuCommand> visible = getVisibleCommands(session);
        for (int i = 0; i < visible.size(); i++) {
            System.out.println((i + 1) + ". " + visible.get(i).getLabel());
        }
    }

    // Finding the command that stands behind the number the user typed.
    // Returning null when the number does not belong to any visible command
    public MenuCommand findCommand(ClientSession session, String choice) {
        Vector<MenuCommand> visible = getVisibleCommands(session);
        for (int i = 0; i < visible.size(); i++) {
            if (choice.equals(String.valueOf(i + 1)))
                return visible.get(i);
        }
        return null;
    }

    // Finding the number under which the chat command is currently displayed, for hints printed to the user
    public String getSendChatNumber(ClientSession session) {
        Vector<MenuCommand> visible = getVisibleCommands(session);
        for (int i = 0; i < visible.size(); i++) {
            if (visible.get(i) instanceof SendChatCommand)
                return String.valueOf(i + 1);
        }
        return "?";
    }
}
