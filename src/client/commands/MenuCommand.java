package client.commands;

import java.util.Scanner;

import client.ClientSession;

// Abstract base class of every option that can appear in the client's main menu (Command pattern).
// Each command knows how it is presented, who is allowed to see it, and what it does when chosen.
// The number the option is displayed under is not stored here, because it depends on
// which options are visible at that moment and may change after every login.
public abstract class MenuCommand {
    // Command fields
    private String label;
    private boolean guestOnly;
    private boolean requiresLogin;
    private String[] allowedRoles;

    // Constructor for the MenuCommand class.
    // guestOnly - the command is shown only while nobody is logged in.
    // requiresLogin - the command is shown only after a successful login.
    // allowedRoles - an empty array means that every role may see the command.
    public MenuCommand(String label, boolean guestOnly, boolean requiresLogin, String[] allowedRoles) {
        this.label = label;
        this.guestOnly = guestOnly;
        this.requiresLogin = requiresLogin;
        this.allowedRoles = allowedRoles;
    }

    // Getter for the text of the command
    public String getLabel() {
        return label;
    }

    // Deciding whether this command should appear in the menu of the given session
    public boolean isVisible(ClientSession session) {
        boolean loggedIn = session.isLoggedIn();
        // An option such as the login itself disappears once a user is connected
        if (guestOnly)
            return !loggedIn;
        // An option that works on the data of the connected user is hidden until the user logs in
        if (requiresLogin && !loggedIn)
            return false;
        // An option without a role restriction is shown to every user
        if (allowedRoles.length == 0)
            return true;
        // Going over the allowed roles and looking for the role of the connected user
        for (int i = 0; i < allowedRoles.length; i++) {
            if (allowedRoles[i].equals(session.getRole()))
                return true;
        }
        return false;
    }

    // Performing the action of the command. Every concrete command implements this differently
    public abstract void execute(ClientSession session, Scanner scanner);

    @Override
    public String toString() {
        return label;
    }
}
