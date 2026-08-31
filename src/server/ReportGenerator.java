package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.SaleRecord;

// Class responsible for generating reports in various formats, such as JSON and Word documents, based on sales data.
public class ReportGenerator {

    // Counting how many sales each branch made and how much money it brought in.
    // The keys of both maps are branch IDs
    private static Map<String, Integer> countSalesByBranch(List<SaleRecord> sales) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (int i = 0; i < sales.size(); i++) {
            String branchId = sales.get(i).getBranchId();
            Integer current = counts.get(branchId);
            counts.put(branchId, current == null ? 1 : current + 1);
        }
        return counts;
    }

    private static Map<String, Double> sumRevenueByBranch(List<SaleRecord> sales) {
        Map<String, Double> totals = new HashMap<String, Double>();
        for (int i = 0; i < sales.size(); i++) {
            SaleRecord s = sales.get(i);
            Double current = totals.get(s.getBranchId());
            totals.put(s.getBranchId(), current == null ? s.getFinalPrice() : current + s.getFinalPrice());
        }
        return totals;
    }

    // Summing the money of all the sales in the report
    private static double sumRevenue(List<SaleRecord> sales) {
        double total = 0;
        for (int i = 0; i < sales.size(); i++) {
            total += sales.get(i).getFinalPrice();
        }
        return total;
    }

    // Producing the report in JSON format, written by hand without any external library
    public static String generateSalesJson(List<SaleRecord> sales) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"totalSalesCount\": ").append(sales.size()).append(",\n");
        double totalRevenue = sumRevenue(sales);
        json.append("  \"totalRevenue\": ").append(String.format("%.2f", totalRevenue)).append(",\n");

        // The amount of sales of every branch, so one report shows the whole network side by side
        Map<String, Integer> countsByBranch = countSalesByBranch(sales);
        Map<String, Double> revenueByBranch = sumRevenueByBranch(sales);
        json.append("  \"salesByBranch\": {\n");
        int written = 0;
        for (Map.Entry<String, Integer> entry : countsByBranch.entrySet()) {
            json.append("    \"").append(entry.getKey()).append("\": { \"salesCount\": ")
                    .append(entry.getValue()).append(", \"revenue\": ")
                    .append(String.format("%.2f", revenueByBranch.get(entry.getKey()))).append(" }");
            written++;
            json.append(written < countsByBranch.size() ? "," : "").append("\n");
        }
        json.append("  },\n");

        json.append("  \"sales\": [\n");

        for (int i = 0; i < sales.size(); i++) {
            // יצירת רשומת מכירה בפורמט JSON
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

    // Exporting the report into a Word document (an HTML format that Word opens as a document)
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

            // A summary table showing the amount of sales of every branch
            pw.println("<h2>כמות מכירות לפי סניף</h2>");
            pw.println("<table>");
            pw.println("<tr><th>סניף</th><th>כמות מכירות</th><th>הכנסות</th></tr>");
            Map<String, Integer> countsByBranch = countSalesByBranch(sales);
            Map<String, Double> revenueByBranch = sumRevenueByBranch(sales);
            for (Map.Entry<String, Integer> entry : countsByBranch.entrySet()) {
                pw.println("<tr>");
                pw.println("<td>" + entry.getKey() + "</td>");
                pw.println("<td>" + entry.getValue() + "</td>");
                pw.println("<td>₪" + String.format("%.2f", revenueByBranch.get(entry.getKey())) + "</td>");
                pw.println("</tr>");
            }
            pw.println("</table>");

            pw.println("<h2>פירוט המכירות</h2>");
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