package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import client.commands.MenuCommand;

// Entry point of the client side. The class only takes care of the connection to the
// server and of the loop that reads the choices of the user, while the menu itself and
// the actions behind it are handled by the command classes.
public class StoreClient {
    // Defining server connection parameters
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 7000;

    public static void main(String[] args) {
        // Establishing a connection to the server and setting up input/output streams for communication
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Scanner scanner = new Scanner(System.in)) {

            ClientSession session = new ClientSession(new PrintWriter(socket.getOutputStream(), true));
            ClientMenu menu = new ClientMenu();

            // Creating a Thread that listens for messages from the server and prints them to the screen
            ServerListener listener = new ServerListener(in, session, menu);
            Thread listenerThread = new Thread(listener);
            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("=== Welcome to the Store Management System (Console Mode) ===");
            menu.printMenu(session);

            // Main user loop
            while (session.isRunning()) {
                String choice = scanner.nextLine().trim();
                if (choice.equalsIgnoreCase("exit")) {
                    // Allowing the user to leave the system by typing the word itself
                    session.send("EXIT");
                    session.stop();
                } else if (choice.equalsIgnoreCase("menu")) {
                    menu.printMenu(session);
                } else {
                    // Looking for the command that stands behind the number the user typed
                    MenuCommand command = menu.findCommand(session, choice);
                    if (command == null) {
                        System.out.println("Invalid choice. Type 'menu' to see options.");
                    } else {
                        command.execute(session, scanner);
                    }
                }
            }

        } catch (IOException e) {
            // Handling exceptions that may occur during server connection or communication
            System.err.println("Server connection error: " + e.getMessage());
        }
    }
}
