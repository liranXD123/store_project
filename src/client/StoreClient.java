package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class StoreClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 7000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("=== מערכת ניהול רשת חנויות ===");
            
            // תהליך קריאה אסינכרוני מהשרת
            Thread listenerThread = new Thread(() -> {
                try {
                    String sMsg;
                    while ((sMsg = serverIn.readLine()) != null) {
                        System.out.println("\n[שרת]: " + sMsg);
                        System.out.print("> ");
                    }
                } catch (Exception ignored) {}
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

            // לולאת תפריט פקודות למשתמש
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("exit")) {
                    serverOut.println("EXIT");
                    break;
                }
                serverOut.println(input);
            }

        } catch (Exception e) {
            System.err.println("שגיאת חיבור לשרת: " + e.getMessage());
        }
    }
}