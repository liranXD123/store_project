package client.commands;

// Command that asks the server for a sales report in JSON format
public class JsonReportCommand extends ReportCommand {
    public JsonReportCommand() {
        super("Generate JSON Report (Branch/ALL)");
    }

    @Override
    protected String getServerCommand() {
        return "REPORT_JSON";
    }
}
