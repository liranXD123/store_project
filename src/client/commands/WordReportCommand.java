package client.commands;

// Command that asks the server to export a sales report into a Word document
public class WordReportCommand extends ReportCommand {
    public WordReportCommand() {
        super("Export Word Report (Branch/ALL)");
    }

    @Override
    protected String getServerCommand() {
        return "REPORT_WORD";
    }
}
