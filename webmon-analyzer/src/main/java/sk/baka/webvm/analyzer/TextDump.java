package sk.baka.webvm.analyzer;

import java.lang.management.MemoryUsage;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import sk.baka.webvm.analyzer.ThreadMap.Item;
import sk.baka.webvm.analyzer.classloader.CLEnum;
import sk.baka.webvm.analyzer.classloader.ClassLoaderUtils;
import sk.baka.webvm.analyzer.config.Config;
import sk.baka.webvm.analyzer.hostos.Architecture;
import sk.baka.webvm.analyzer.hostos.Memory;
import sk.baka.webvm.analyzer.hostos.OS;
import sk.baka.webvm.analyzer.utils.Constants;
import sk.baka.webvm.analyzer.utils.MemoryUsage2;
import sk.baka.webvm.analyzer.utils.MemoryUsages;
import sk.baka.webvm.analyzer.utils.Threads;

/**
 * Dumps a VM state.
 *
 * @author Martin Vysny
 */
public class TextDump {

    public static final class TextTable {

        public boolean horizontalHeaderSeparator = true;
        public boolean verticalHeaderSeparator = true;
        public boolean horizontalContentsSeparator = false;
        public boolean verticalContentsSeparator = true;

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
        printHeader(sb, "VM Dump Report");
        sb.append("OS: ").append(OS.get());
        sb.append("  Architecture: ").append(Architecture.getFullName()).append("\n\n");
        printVMHistoryOverview(sb, history);
        sb.append('\n');
        printMemoryUsageHistory(sb, history);
        sb.append('\n');
        printThreadCPUUsage(sb, history);
        sb.append('\n');
        printThreadStacktraceDump(sb);
        sb.append('\n');
        printHeader(sb, "Environment dump");
        sb.append("Java System Properties\n");
        printProperties(sb, System.getProperties());
        sb.append("\nEnvironment Variables\n");
        printProperties(sb, System.getenv());
        final Thread current = Thread.currentThread();
        sb.append("\nContext Class Loader of thread " + "0x" + Long.toHexString(current.getId()) + " " + current.getName() + "\n");
        sb.append("WARNING: several CLs may be missing. Use Webmon web interface to obtain class loaders of the web server");
        final List<ClassLoader> cls = ClassLoaderUtils.getClassLoaderChain(Thread.currentThread().getContextClassLoader());
        int clNumber = 1;
        for (ClassLoader cl: cls) {
            final String name = "[" + (clNumber++) + "] " + CLEnum.getTypes(cl) + " " + cl.getClass().getName() + ": " + cl;
            sb.append(name).append("\n");
            if (cl instanceof URLClassLoader) {
                for (URL url: ((URLClassLoader) cl).getURLs()) {
                    sb.append("  ").append(url).append("\n");
                }
            }
            sb.append("\n");
        }
        sb.append("\nClass loaders loading same jar\n");
        final Map<URI, List<Integer>> clashes = ClassLoaderUtils.getClashes();
        sb.append(clashes).append("\n");
        printHeader(sb, "Problems report");
        final List<ProblemReport> problems = new ProblemAnalyzer(new Config(), Memory.getOSMemoryInfoProvider()).getProblems(history);
        for (ProblemReport problem : problems) {
            sb.append(problem).append('\n');
        }
        return sb.toString();
    }

    private static void printProperties(StringBuilder sb, Map<?, ?> env) {
        final List<String> names = new ArrayList<String>(env.size());
        for (Object name : env.keySet()) {
            names.add(name.toString());
        }
        Collections.sort(names, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        for (String name : names) {
            sb.append(name).append('=').append(env.get(name)).append('\n');
        }
    }

    private static void printThreadStacktraceDump(StringBuilder sb) {
        printHeader(sb, "Thread Stacktrace Dump");
        sb.append(new Threads.Dump());
        sb.append("\nThead Deadlock Analysis Result:\n");
        sb.append(ProblemAnalyzer.getDeadlockReport().toString());
        sb.append("\n\n");
    }

    private static void printMemoryUsageHistory(StringBuilder sb, List<HistorySample> history) {
        final List<String> header = new ArrayList<String>(Collections.nCopies(history.size() + 1, ""));
        header.set(0, "Memory Used %");
        header.set(1, "old");
        header.set(header.size() - 1, "new");
        final TextTable table = new TextTable(history.size() + 1);
        table.verticalContentsSeparator = false;
        final List<Boolean> rightAlign = new ArrayList<Boolean>(Collections.nCopies(history.size() + 1, Boolean.TRUE));
        table.add(header, rightAlign);
        final List<List<String>> content = new ArrayList<List<String>>();
        final HistorySample last = history.isEmpty() ? null : history.get(history.size() - 1);
        for (HistorySample.MemoryPools pool: HistorySample.MemoryPools.values()) {
            content.add(new ArrayList<String>(Collections.singletonList(pool.displayable)));
            if (last != null) {
                if (content.size() > 1) {
                    sb.append(" / ");
                }
                sb.append(content.get(content.size() - 1).get(0)).append(": ");
                sb.append(MemoryUsages.toString(last.memPoolUsage.get(pool), true));
            }
        }
        sb.append("\n");
        for (int i = 0; i < history.size(); i++) {
            for (int j = 0; j < HistorySample.MemoryPools.values().length; j++) {
                content.get(j).add(MemoryUsage2.getUsagePerc(history.get(i).memPoolUsage.get(HistorySample.MemoryPools.values()[j])));
            }
        }
        for (int i = 0; i < HistorySample.MemoryPools.values().length; i++) {
            table.add(content.get(i), rightAlign);
        }
        sb.append(table.toString());
    }

    private static final int MAX_THREAD_NAME_LENGTH = 48;
    
    private static String getThreadName(Collection<Item> items) {
        for (Item item : items) {
            if (item != null) {
                return truncate("0x" + item.info.getThreadId() + " " + item.info.getThreadName(), MAX_THREAD_NAME_LENGTH);
            }
        }
        return null;
    }

    private static String truncate(String str, int maxlen) {
        return str.length() > maxlen ? str.substring(0, maxlen) : str;
    }

    private static void printThreadCPUUsage(StringBuilder sb, List<HistorySample> history) {
        printHeader(sb, "Per-Thread CPU Usage history");
        final List<String> header = new ArrayList<String>(Collections.nCopies(history.size() + 1, ""));
        header.set(1, "old");
        header.set(header.size() - 1, "new");
        final TextTable table = new TextTable(history.size() + 1);
        table.verticalContentsSeparator = false;
        final List<Boolean> rightAlign = new ArrayList<Boolean>(Collections.nCopies(history.size() + 1, Boolean.TRUE));
        table.add(header, rightAlign);
        final SortedMap<Long, List<Item>> threadTable = ThreadMap.historyToTable(history);
        for (List<Item> row : threadTable.values()) {
            final List<String> contentRow = new ArrayList<String>();
            final String threadName = getThreadName(row);
            if (threadName == null) {
                continue;
            }
            for (Item item : row) {
                if (contentRow.isEmpty()) {
                    contentRow.add(threadName);
                }
                contentRow.add(item == null ? "" : (item.lastCpuUsagePerc == null ? "?" : item.lastCpuUsagePerc.toString()));
            }
            table.add(contentRow, rightAlign);
        }
        sb.append(table.toString());
    }

    private static void printHeader(StringBuilder sb, String header) {
        sb.append("======================== ");
        sb.append(header);
        sb.append(" ========================\n\n");
    }

    private static void printVMHistoryOverview(StringBuilder sb, List<HistorySample> history) {
        printHeader(sb, "History of VM overview");
        final List<String> header = new ArrayList<String>(Collections.nCopies(history.size() + 1, ""));
        header.set(1, "old");
        header.set(header.size() - 1, "new");
        final TextTable table = new TextTable(history.size() + 1);
        table.verticalContentsSeparator = false;
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
