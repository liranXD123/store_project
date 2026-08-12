package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerService {
    private static LoggerService instance;
    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LoggerService() {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static synchronized LoggerService getInstance() {
        if (instance == null) {
            instance = new LoggerService();
        }
        return instance;
    }

    public enum LogType {
        EMPLOYEES("employees.log"),
        CUSTOMERS("customers.log"),
        TRANSACTIONS("sales_transactions.log"),
        CHAT("chat_history.log"),
        SYSTEM("system.log");

        private final String fileName;
        LogType(String fileName) { this.fileName = fileName; }
        public String getFileName() { return fileName; }
    }

    public synchronized void log(LogType type, String message) {
        File file = new File(LOG_DIR + File.separator + type.getFileName());
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
            String timestamp = LocalDateTime.now().format(DTF);
            pw.println(String.format("[%s] [%s] %s", timestamp, type.name(), message));
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
}