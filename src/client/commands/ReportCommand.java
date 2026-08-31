package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Abstract command for producing a sales report.
// Every report is built in exactly the same way and only the command that is finally
// sent to the server changes, so the shared steps are written here once and the
// changing step is left to the inheriting classes.
public abstract class ReportCommand extends MenuCommand {
    public ReportCommand(String label) {
        super(label, false, true, new String[] {});
    }

    @Override
    public void execute(ClientSession session, Scanner scanner) {
        // Requesting the filter of the report from the user
        System.out.println("Select Filter Type:");
        System.out.println("1. ALL (Entire Network)");
        System.out.println("2. BRANCH (Filter by Branch ID)");
        System.out.println("3. PRODUCT (Filter by Product ID)");
        System.out.println("4. CATEGORY (Filter by Category Name)");
        System.out.println("5. DATE (Daily report for a single day)");
        System.out.print("Your choice (1-5): ");
        String filterChoice = scanner.nextLine();

        String filterType = "ALL";
        String filterValue = "ALL";

        if (filterChoice.equals("2")) {
            // Requesting the branch ID from the user
            filterType = "BRANCH";
            System.out.print("Enter Branch ID (e.g., B1): ");
            filterValue = scanner.nextLine();
        } else if (filterChoice.equals("3")) {
            // Requesting the product ID from the user
            filterType = "PRODUCT";
            System.out.print("Enter Product ID (e.g., P01): ");
            filterValue = scanner.nextLine();
        } else if (filterChoice.equals("4")) {
            // Requesting the category name from the user
            filterType = "CATEGORY";
            System.out.print("Enter Category (e.g., Shirts): ");
            filterValue = scanner.nextLine();
        } else if (filterChoice.equals("5")) {
            // Requesting the day of the report from the user
            filterType = "DATE";
            System.out.print("Enter Date (yyyy-MM-dd, e.g. 2026-08-31): ");
            filterValue = scanner.nextLine();
        }

        // Sending the report command of the inheriting class together with the chosen filter
        session.send(getServerCommand() + "::" + filterType + "::" + filterValue);
    }

    // Returning the command the server expects for this kind of report
    protected abstract String getServerCommand();
}
