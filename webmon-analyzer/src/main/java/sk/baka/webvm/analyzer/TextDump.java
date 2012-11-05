package sk.baka.webvm.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dumps a VM state.
 *
 * @author Martin Vysny
 */
public class TextDump {

    public static final class TextTable {

        public boolean horizontalHeaderSeparator = true;
        public boolean verticalHeaderSeparator = true;
        public boolean horizontalContentsSeparator = true;
        public boolean verticalContentsSeparator = false;

        public TextTable(int columnCount) {
            columnSizes = new int[columnCount];
        }
        private final int[] columnSizes;
        private final List<List<String>> contents = new ArrayList<List<String>>();
        private final List<List<Boolean>> rightAlign = new ArrayList<List<Boolean>>();

        public TextTable add(List<String> values, List<Boolean> rightAlign) {
            if (values.size() < columnSizes.length) {
                throw new IllegalArgumentException("Parameter values: invalid value " + values + ": must contain at least " + columnSizes.length + " items");
            }
            if (rightAlign.size() < columnSizes.length) {
                throw new IllegalArgumentException("Parameter rightAlign: invalid value " + rightAlign + ": must contain at least " + columnSizes.length + " items");
            }
            contents.add(values);
            this.rightAlign.add(rightAlign);
            for (int i = 0; i < columnSizes.length; i++) {
                if (columnSizes[i] < values.get(i).length()) {
                    columnSizes[i] = values.get(i).length();
                }
            }
            return this;
        }

        private void printContent(StringBuilder target, List<String> contents, List<Boolean> rightAlign) {
            for (int i = 0; i < contents.size(); i++) {
                final boolean ra = rightAlign == null ? false : rightAlign.get(i);
                if (!ra) {
                    target.append(contents.get(i));
                }
                for (int spacesToAdd = columnSizes[i] - contents.get(i).length(); spacesToAdd > 0; spacesToAdd--) {
                    target.append(' ');
                }
                if (ra) {
                    target.append(contents.get(i));
                }
                if (i < contents.size() - 1) {
                    target.append(printVerticalSeparator(i) ? " | " : " ");
                }
            }
            target.append('\n');
        }

        private boolean printVerticalSeparator(int index) {
            return (verticalHeaderSeparator && index == 0) || (verticalContentsSeparator && index > 0);
        }

        private boolean printHorizontalSeparator(int index) {
            return (horizontalHeaderSeparator && index == 0) || (horizontalContentsSeparator && index > 0);
        }

        private void printHorizontalSeparator(StringBuilder target) {
            for (int i = 0; i < columnSizes.length; i++) {
                for (int spacesToAdd = columnSizes[i]; spacesToAdd > 0; spacesToAdd--) {
                    target.append('-');
                }
                if (i < columnSizes.length - 1) {
                    target.append(printVerticalSeparator(i) ? "-+-" : "-");
                }
            }
            target.append('\n');
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < contents.size(); i++) {
                printContent(sb, contents.get(i), rightAlign.get(i));
                if (printHorizontalSeparator(i) && i < contents.size() - 1) {
                    printHorizontalSeparator(sb);
                }
            }
            return sb.toString();
        }
    }

    public static String dump(List<HistorySample> history) {
        final StringBuilder sb = new StringBuilder();
        printVMHistoryOverview(sb, history);
        return sb.toString();
    }

    private static void printVMHistoryOverview(StringBuilder sb, List<HistorySample> history) {
        final List<String> header = new ArrayList<String>(Collections.nCopies(history.size() + 1, ""));
        header.set(1, "old");
        header.set(header.size() - 1, "new");
        final TextTable table = new TextTable(history.size() + 1);
        table.horizontalContentsSeparator = false;
        final List<String> gccpuusage = new ArrayList<String>();
        gccpuusage.add("GC CPU Usage %");
        final List<String> threadcount = new ArrayList<String>();
        threadcount.add("Thread Count");
        final List<String> daemonthreadcount = new ArrayList<String>();
        daemonthreadcount.add("Daemon Thread Count");
        final List<String> hostcpuusage = new ArrayList<String>();
        hostcpuusage.add("Host CPU Usage %");
        final List<String> javacpuusage = new ArrayList<String>();
        javacpuusage.add("Java CPU Usage %");
        final List<String> hostiousage = new ArrayList<String>();
        hostiousage.add("Host IO Usage %");
        final List<String> classcount = new ArrayList<String>();
        classcount.add("Loaded Classes");
        for (HistorySample hs : history) {
            gccpuusage.add("" + hs.gcCpuUsage);
            threadcount.add("" + hs.threads.threadCount);
            daemonthreadcount.add("" + hs.threads.daemonThreadCount);
            hostcpuusage.add("" + hs.cpuUsage);
            javacpuusage.add("" + hs.cpuJavaUsage);
            hostiousage.add("" + hs.cpuIOUsage);
            classcount.add("" + hs.classesLoaded);
        }
        final List<Boolean> rightAlign = new ArrayList<Boolean>(Collections.nCopies(history.size() + 1, Boolean.TRUE));
        table.add(header, rightAlign);
        table.add(hostcpuusage, rightAlign);
        table.add(hostiousage, rightAlign);
        table.add(javacpuusage, rightAlign);
        table.add(gccpuusage, rightAlign);
        table.add(threadcount, rightAlign);
        table.add(daemonthreadcount, rightAlign);
        table.add(classcount, rightAlign);
        sb.append(table.toString());
    }
}
