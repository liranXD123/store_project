package client.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class StoreGUI extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // נתוני המשתמש המחובר
    private String currentEmpId;
    private String currentEmpName;
    private String currentRole;
    private String currentBranchId;

    // רכיבי ממשק
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    // מסך ראשי
    private DefaultTableModel inventoryTableModel;
    private DefaultTableModel customerTableModel;
    private JTextArea chatArea;
    private JTextField chatInputField;
    private JLabel userInfoLabel;

    public StoreGUI() {
        super("מערכת ניהול רשת חנויות בגדים - HIT");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);

        initNetwork();
        buildLoginPanel();
        buildDashboardPanel();

        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");
    }

    private void initNetwork() {
        try {
            socket = new Socket("localhost", 7000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // תהליך קליטת הודעות אסינכרוני מהשרת
            Thread listener = new Thread(this::listenToServer);
            listener.setDaemon(true);
            listener.start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "לא ניתן להתחבר לשרת: " + e.getMessage(), "שגיאת תקשורת", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildLoginPanel() {
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(new Color(240, 245, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("התחברות למערכת רשת החנויות", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        loginPanel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        loginPanel.add(new JLabel("מספר עובד:"), gbc);
        JTextField empIdField = new JTextField(15);
        gbc.gridx = 1;
        loginPanel.add(empIdField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        loginPanel.add(new JLabel("סיסמה:"), gbc);
        JPasswordField passField = new JPasswordField(15);
        gbc.gridx = 1;
        loginPanel.add(passField, gbc);

        JButton loginBtn = new JButton("התחבר");
        loginBtn.setBackground(new Color(47, 85, 151));
        loginBtn.setForeground(Color.WHITE);
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        loginPanel.add(loginBtn, gbc);

        loginBtn.addActionListener(e -> {
            String emp = empIdField.getText().trim();
            String pass = new String(passField.getPassword()).trim();
            if (!emp.isEmpty() && !pass.isEmpty()) {
                currentEmpId = emp;
                out.println("LOGIN::" + emp + "::" + pass);
            }
        });

        mainPanel.add(loginPanel, "LOGIN");
    }

    private void buildDashboardPanel() {
        JPanel dashboard = new JPanel(new BorderLayout(5, 5));

        // סרגל עליון
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(47, 85, 151));
        userInfoLabel = new JLabel(" מחובר: ", JLabel.RIGHT);
        userInfoLabel.setForeground(Color.WHITE);
        userInfoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(userInfoLabel, BorderLayout.CENTER);

        JButton logoutBtn = new JButton("התנתק");
        logoutBtn.addActionListener(e -> {
            out.println("EXIT");
            System.exit(0);
        });
        topPanel.add(logoutBtn, BorderLayout.WEST);
        dashboard.add(topPanel, BorderLayout.NORTH);

        // טאבים
        JTabbedPane tabbedPane = new JTabbedPane();

        // טאב מלאי ומכירה
        JPanel inventoryPanel = new JPanel(new BorderLayout(5, 5));
        inventoryTableModel = new DefaultTableModel(new String[]{"קוד מוצר", "שם מוצר", "מחיר בסיס", "כמות במלאי"}, 0);
        JTable invTable = new JTable(inventoryTableModel);
        inventoryPanel.add(new JScrollPane(invTable), BorderLayout.CENTER);

        JPanel buyPanel = new JPanel(new FlowLayout());
        JTextField custIdInput = new JTextField(8);
        JTextField prodIdInput = new JTextField(8);
        JTextField qtyInput = new JTextField("1", 4);
        JButton buyBtn = new JButton("בצע מכירה");

        buyPanel.add(new JLabel("קוד לקוח:"));
        buyPanel.add(custIdInput);
        buyPanel.add(new JLabel("קוד מוצר:"));
        buyPanel.add(prodIdInput);
        buyPanel.add(new JLabel("כמות:"));
        buyPanel.add(qtyInput);
        buyPanel.add(buyBtn);

        buyBtn.addActionListener(e -> {
            String cId = custIdInput.getText().trim();
            String pId = prodIdInput.getText().trim();
            String q = qtyInput.getText().trim();
            if (!cId.isEmpty() && !pId.isEmpty() && !q.isEmpty()) {
                out.println("BUY::" + cId + "::" + pId + "::" + q);
            }
        });
        inventoryPanel.add(buyPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("ניהול מלאי ומכירות", inventoryPanel);

        // טאב לקוחות הרשת
        JPanel custPanel = new JPanel(new BorderLayout(5, 5));
        customerTableModel = new DefaultTableModel(new String[]{"קוד לקוח", "שם מלא", "טלפון", "סוג לקוח (מבצע)"}, 0);
        JTable custTable = new JTable(customerTableModel);
        custPanel.add(new JScrollPane(custTable), BorderLayout.CENTER);
        tabbedPane.addTab("לקוחות הרשת", custPanel);

        // טאב דוחות
        JPanel reportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 20));
        JButton exportJsonBtn = new JButton("הפק דוח מכירות (JSON)");
        JButton exportWordBtn = new JButton("יצא דוח מכירות ל-Word (.doc)");
        reportPanel.add(exportJsonBtn);
        reportPanel.add(exportWordBtn);

        exportJsonBtn.addActionListener(e -> out.println("REPORT_JSON::ALL"));
        exportWordBtn.addActionListener(e -> out.println("REPORT_WORD::ALL"));
        tabbedPane.addTab("דוחות והנהלה", reportPanel);

        // טאב צ'אט בין סניפים
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel chatBottom = new JPanel(new BorderLayout());
        chatInputField = new JTextField();
        JButton sendChatBtn = new JButton("שלח");
        JButton requestChatBtn = new JButton("בקש צ'אט מסניף אחר");

        chatBottom.add(requestChatBtn, BorderLayout.NORTH);
        chatBottom.add(chatInputField, BorderLayout.CENTER);
        chatBottom.add(sendChatBtn, BorderLayout.EAST);

        sendChatBtn.addActionListener(e -> {
            String msg = chatInputField.getText().trim();
            if (!msg.isEmpty()) {
                chatArea.append("אני: " + msg + "\n");
                out.println("CHAT_MSG::" + msg);
                chatInputField.setText("");
            }
        });

        requestChatBtn.addActionListener(e -> {
            String targetBranch = JOptionPane.showInputDialog(this, "הזן מזהה סניף מבוקש (למשל B1 או B2):");
            if (targetBranch != null && !targetBranch.trim().isEmpty()) {
                out.println("CHAT_REQUEST::" + targetBranch.trim());
            }
        });

        chatPanel.add(chatBottom, BorderLayout.SOUTH);
        tabbedPane.addTab("צ'אט סניפים", chatPanel);

        dashboard.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(dashboard, "DASHBOARD");
    }

    private void listenToServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                final String msg = line;
                SwingUtilities.invokeLater(() -> handleServerMessage(msg));
            }
        } catch (IOException ignored) {}
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("LOGIN_SUCCESS::")) {
            String[] parts = message.split("::");
            currentEmpName = parts[1];
            currentRole = parts[2];
            currentBranchId = parts[3];

            userInfoLabel.setText(" שלום, " + currentEmpName + " | תפקיד: " + currentRole + " | סניף: " + currentBranchId + " ");
            cardLayout.show(mainPanel, "DASHBOARD");

            // טעינת מלאי ולקוחות ראשונית
            out.println("GET_INVENTORY");
            out.println("GET_CUSTOMERS");
        } else if (message.startsWith("ERROR::")) {
            JOptionPane.showMessageDialog(this, message.substring(7), "שגיאה", JOptionPane.ERROR_MESSAGE);
        } else if (message.startsWith("INVENTORY_DATA::")) {
            updateInventoryTable(message.substring(16));
        } else if (message.startsWith("CUSTOMERS_DATA::")) {
            updateCustomersTable(message.substring(16));
        } else if (message.startsWith("BUY_SUCCESS::")) {
            String[] p = message.split("::");
            JOptionPane.showMessageDialog(this, "המכירה בוצעה בהצלחה!\nמזהה עסקה: " + p[1] + "\nסכום לתשלום: ₪" + p[2]);
            out.println("GET_INVENTORY");
        } else if (message.equals("INVENTORY_UPDATED")) {
            out.println("GET_INVENTORY");
        } else if (message.startsWith("CHAT_INCOMING::")) {
            String[] p = message.split("::");
            chatArea.append(p[1] + ": " + p[2] + "\n");
        } else if (message.startsWith("REPORT_JSON_DATA::")) {
            JTextArea jsonView = new JTextArea(message.substring(18), 15, 40);
            jsonView.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(jsonView), "דוח מכירות בפורמט JSON", JOptionPane.INFORMATION_MESSAGE);
        } else if (message.startsWith("CHAT_QUEUED::")) {
            JOptionPane.showMessageDialog(this, message.substring(13), "תור צ'אט", JOptionPane.WARNING_MESSAGE);
        } else if (message.startsWith("REPORT_WORD_SUCCESS::")) {
             String[] p = message.split("::");
                JOptionPane.showMessageDialog(this, "הדוח ייוצא בהצלחה לקובץ בתיקיית הפרויקט:\n" + p[1], "יצוא ל-Word", JOptionPane.INFORMATION_MESSAGE);
        }
        
    }

    private void updateInventoryTable(String data) {
        inventoryTableModel.setRowCount(0);
        if (data.isEmpty()) return;
        String[] items = data.split(";");
        for (String item : items) {
            if (!item.isEmpty()) {
                String[] f = item.split(",");
                inventoryTableModel.addRow(new Object[]{f[0], f[1], "₪" + f[2], f[3]});
            }
        }
    }

    private void updateCustomersTable(String data) {
        customerTableModel.setRowCount(0);
        if (data.isEmpty()) return;
        String[] custs = data.split(";");
        for (String c : custs) {
            if (!c.isEmpty()) {
                String[] f = c.split(",");
                customerTableModel.addRow(new Object[]{f[0], f[1], f[2], f[3]});
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StoreGUI().setVisible(true));
    }
}