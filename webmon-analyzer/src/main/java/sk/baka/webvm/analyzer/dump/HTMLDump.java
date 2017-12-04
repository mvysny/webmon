package sk.baka.webvm.analyzer.dump;

import sk.baka.webvm.analyzer.HistorySample;
import sk.baka.webvm.analyzer.ProblemAnalyzer;
import sk.baka.webvm.analyzer.ProblemReport;
import sk.baka.webvm.analyzer.utils.Threads;

import java.util.*;

/**
 * @author mavi
 */
public class HTMLDump extends AbstractDump {
    @Override protected void newLine(StringBuilder sb) {
        sb.append("<br/>\n");
    }

    @Override protected void printHeader(StringBuilder sb, String header) {
        sb.append("<h2>").append(header).append("</h2>");
    }

    @Override
    protected void printProperties(StringBuilder sb, Map<?, ?> env) {
        final List<String> names = new ArrayList<String>(env.size());
        for (Object name : env.keySet()) {
            names.add(name.toString());
        }
        Collections.sort(names, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        sb.append("<table>");
        for (String name : names) {
            sb.append("<tr><td>").append(name).append("</td><td>").append(env.get(name)).append("</td></tr>");
        }
        sb.append("</table>");
    }

    @Override
    protected void printThreadStacktraceDump(StringBuilder sb) {
        printHeader(sb, "Thread Stacktrace Dump");
        sb.append("<code><pre>");
        sb.append(new Threads.Dump());
        sb.append("</pre></code>");
        sb.append("<br/>Thead Deadlock Analysis Result:<br/>");
        sb.append(ProblemReport.toHtml(Collections.singleton(ProblemAnalyzer.getDeadlockReport())));
        sb.append("<br/><br/>");
    }

    @Override protected Table newTable(int numberOfColumns) {
        return new HtmlTable();
    }

    private static class HtmlTable implements Table {
        private final StringBuilder sb = new StringBuilder("<table>");

        @Override public void setHorizontalHeaderSeparator(boolean horizontalHeaderSeparator) {
        }

        @Override public void setVerticalHeaderSeparator(boolean verticalHeaderSeparator) {
        }

        @Override public void setHorizontalContentsSeparator(boolean horizontalContentsSeparator) {
        }

        @Override public void setVerticalContentsSeparator(boolean verticalContentsSeparator) {
        }

        @Override public void add(List<String> row, List<Boolean> rightAlign) {
            sb.append("<tr>");
            for (int i = 0; i < row.size(); i++) {
                sb.append("<td");
                if (rightAlign.get(i)) {
                    sb.append(" align='right'");
                }
                sb.append(">").append(row.get(i)).append("</td>");
            }
            sb.append("</tr>\n");
        }

        @Override public String toString() {
            return sb.toString() + "</table>";
        }
    }

    @Override public String dump(List<HistorySample> history) {
        return "<html><head><style type='text/css'>table {\n"
                + "    border-collapse: collapse;\n"
                + "}\n"
                + "\n"
                + "table, th, td {\n"
                + "    border: 1px solid black;\n"
                + "}</style></head><body>" + super.dump(history) + "</body></html>";
    }
}
