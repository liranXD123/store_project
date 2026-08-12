package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import model.SaleRecord;

public class ReportGenerator {

    // הפקת דוח בפורמט JSON (נקי ללא ספריות חיצוניות)
    public static String generateSalesJson(List<SaleRecord> sales) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"totalSalesCount\": ").append(sales.size()).append(",\n");
        double totalRevenue = sales.stream().mapToDouble(SaleRecord::getFinalPrice).sum();
        json.append("  \"totalRevenue\": ").append(String.format("%.2f", totalRevenue)).append(",\n");
        json.append("  \"sales\": [\n");

        for (int i = 0; i < sales.size(); i++) {
            SaleRecord s = sales.get(i);
            json.append("    {\n");
            json.append("      \"transactionId\": \"").append(s.getTransactionId()).append("\",\n");
            json.append("      \"branchId\": \"").append(s.getBranchId()).append("\",\n");
            json.append("      \"employeeId\": \"").append(s.getEmployeeId()).append("\",\n");
            json.append("      \"customerId\": \"").append(s.getCustomerId()).append("\",\n");
            json.append("      \"productName\": \"").append(s.getProductName()).append("\",\n");
            json.append("      \"category\": \"").append(s.getCategory()).append("\",\n");
            json.append("      \"quantity\": ").append(s.getQuantity()).append(",\n");
            json.append("      \"finalPrice\": ").append(s.getFinalPrice()).append(",\n");
            json.append("      \"timestamp\": \"").append(s.getTimestamp().toString()).append("\"\n");
            json.append("    }").append(i < sales.size() - 1 ? "," : "").append("\n");
        }
        json.append("  ]\n");
        json.append("}");
        return json.toString();
    }

    // יצוא דוח למסמך Word (בפורמט XML/HTML תואם Word באופן מלא)
    public static void exportToWordDoc(String filePath, String title, List<SaleRecord> sales) throws IOException {
        File file = new File(filePath);
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>");
            pw.println("<head><meta charset='utf-8'><title>" + title + "</title>");
            pw.println("<style>");
            pw.println("body { font-family: Arial, sans-serif; direction: rtl; }");
            pw.println("table { border-collapse: collapse; width: 100%; margin-top: 20px; }");
            pw.println("th, td { border: 1px solid #dddddd; text-align: right; padding: 8px; }");
            pw.println("th { background-color: #2F5597; color: white; }");
            pw.println("tr:nth-child(even) { background-color: #f2f2f2; }");
            pw.println("</style></head>");
            pw.println("<body>");
            pw.println("<h1>" + title + "</h1>");
            pw.println("<p>תאריך הפקה: " + java.time.LocalDateTime.now() + "</p>");
            pw.println("<table>");
            pw.println("<tr><th>מזהה עסקה</th><th>סניף</th><th>מוצר</th><th>קטגוריה</th><th>כמות</th><th>מחיר סופי</th></tr>");

            double total = 0;
            for (SaleRecord s : sales) {
                pw.println("<tr>");
                pw.println("<td>" + s.getTransactionId() + "</td>");
                pw.println("<td>" + s.getBranchId() + "</td>");
                pw.println("<td>" + s.getProductName() + "</td>");
                pw.println("<td>" + s.getCategory() + "</td>");
                pw.println("<td>" + s.getQuantity() + "</td>");
                pw.println("<td>₪" + String.format("%.2f", s.getFinalPrice()) + "</td>");
                pw.println("</tr>");
                total += s.getFinalPrice();
            }
            pw.println("</table>");
            pw.println("<h3>סה\"כ הכנסות: ₪" + String.format("%.2f", total) + "</h3>");
            pw.println("</body></html>");
        }
    }
}