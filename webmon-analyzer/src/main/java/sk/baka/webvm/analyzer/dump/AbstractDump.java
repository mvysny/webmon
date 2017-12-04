package sk.baka.webvm.analyzer.dump;

import sk.baka.webvm.analyzer.HistorySample;
import sk.baka.webvm.analyzer.ProblemAnalyzer;
import sk.baka.webvm.analyzer.ProblemReport;
import sk.baka.webvm.analyzer.ThreadMap;
import sk.baka.webvm.analyzer.classloader.CLEnum;
import sk.baka.webvm.analyzer.classloader.ClassLoaderUtils;
import sk.baka.webvm.analyzer.config.Config;
import sk.baka.webvm.analyzer.hostos.Architecture;
import sk.baka.webvm.analyzer.hostos.Memory;
import sk.baka.webvm.analyzer.hostos.OS;
import sk.baka.webvm.analyzer.utils.MemoryUsage2;
import sk.baka.webvm.analyzer.utils.MemoryUsages;

import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author mavi
 */
public abstract class AbstractDump {

    protected abstract void newLine(StringBuilder sb);

    protected abstract void printProperties(StringBuilder sb, Map<?, ?> env);

    public String dump(List<HistorySample> history) {
        final StringBuilder sb = new StringBuilder();
        printHeader(sb, "VM Dump Report");
        sb.append("OS: ").append(OS.get());
        sb.append("  Architecture: ").append(Architecture.getFullName());
        newLine(sb);
        newLine(sb);
        printVMHistoryOverview(sb, history);
        newLine(sb);
        printMemoryUsageHistory(sb, history);
        newLine(sb);
        printThreadCPUUsage(sb, history);
        newLine(sb);
        printThreadStacktraceDump(sb);
        newLine(sb);
        printHeader(sb, "Environment dump");
        sb.append("Java System Properties");
        newLine(sb);
        printProperties(sb, System.getProperties());
        newLine(sb);
        sb.append("Environment Variables");
        newLine(sb);
        printProperties(sb, System.getenv());
        final Thread current = Thread.currentThread();
        newLine(sb);
        sb.append("Context Class Loader of thread " + "0x" + Long.toHexString(current.getId()) + " " + current.getName());
        newLine(sb);
        sb.append("WARNING: several CLs may be missing. Use Webmon web interface to obtain class loaders of the web server");
        final List<ClassLoader> cls = ClassLoaderUtils.getClassLoaderChain(Thread.currentThread().getContextClassLoader());
        int clNumber = 1;
        for (ClassLoader cl: cls) {
            final String name = "[" + (clNumber++) + "] " + CLEnum.getTypes(cl) + " " + cl.getClass().getName() + ": " + cl;
            sb.append(name);
            newLine(sb);
            if (cl instanceof URLClassLoader) {
                for (URL url: ((URLClassLoader) cl).getURLs()) {
                    sb.append("  ").append(url);
                    newLine(sb);
                }
            }
            newLine(sb);
        }
        newLine(sb);
        sb.append("Class loaders loading same jar");
        newLine(sb);
        final Map<URI, List<Integer>> clashes = ClassLoaderUtils.getClashes();
        sb.append(clashes);
        newLine(sb);
        printHeader(sb, "Problems report");
        final List<ProblemReport> problems = new ProblemAnalyzer(new Config(), Memory.getOSMemoryInfoProvider()).getProblems(history);
        for (ProblemReport problem : problems) {
            sb.append(problem);
            newLine(sb);
        }
        return sb.toString();
    }

    protected abstract void printHeader(StringBuilder sb, String header);

    protected abstract void printThreadStacktraceDump(StringBuilder sb);

    protected abstract Table newTable(int numberOfColumns);

    public interface Table {
        void setHorizontalHeaderSeparator(boolean horizontalHeaderSeparator);
        void setVerticalHeaderSeparator(boolean verticalHeaderSeparator);
        void setHorizontalContentsSeparator(boolean horizontalContentsSeparator);
        void setVerticalContentsSeparator(boolean verticalContentsSeparator);
        void add(List<String> row, List<Boolean> rightAlign);
    }

    private void printVMHistoryOverview(StringBuilder sb, List<HistorySample> history) {
        printHeader(sb, "History of VM overview");
        final List<String> header = new ArrayList<String>(Collections.nCopies(history.size() + 1, ""));
        header.set(1, "old");
        header.set(header.size() - 1, "new");
        final Table table = newTable(history.size() + 1);
        table.setVerticalContentsSeparator(false);
        final List<String> gccpuusage = new ArrayList<String>();
        gccpuusage.add("GC CPU Usage %");
        final List<String> threadcount = new ArrayList<String>();
        threadcount.add("Thread Count");
        final List<String> daemonthreadcount = new ArrayList<String>();
        daemonthreadcount.add("Daemon Thread Count");
        final List<String> hostcpuusage = new ArrayList<String>();
        hostcpuusage.add("Host CPU Usage %");
        final List<String> cpucoreusage = new ArrayList<String>();
        cpucoreusage.add("CPU Core Max Usage %");
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
            hostcpuusage.add("" + hs.cpuUsage.cpuAvgUsage);
            javacpuusage.add("" + hs.cpuJavaUsage);
            hostiousage.add("" + hs.cpuIOUsage);
            classcount.add("" + hs.classesLoaded);
            cpucoreusage.add("" + hs.cpuUsage.cpuMaxCoreUsage);
        }
        final List<Boolean> rightAlign = new ArrayList<Boolean>(Collections.nCopies(history.size() + 1, Boolean.TRUE));
        table.add(header, rightAlign);
        table.add(hostcpuusage, rightAlign);
        table.add(cpucoreusage, rightAlign);
        table.add(hostiousage, rightAlign);
        table.add(javacpuusage, rightAlign);
        table.add(gccpuusage, rightAlign);
        table.add(threadcount, rightAlign);
        table.add(daemonthreadcount, rightAlign);
        table.add(classcount, rightAlign);
        sb.append(table.toString());
    }

    private void printThreadCPUUsage(StringBuilder sb, List<HistorySample> history) {
        printHeader(sb, "Per-Thread CPU Usage history");
        final List<String> header = new ArrayList<String>(Collections.nCopies(history.size() + 1, ""));
        header.set(1, "old");
        header.set(header.size() - 1, "new");
        final Table table = newTable(history.size() + 1);
        table.setVerticalContentsSeparator(false);
        final List<Boolean> rightAlign = new ArrayList<Boolean>(Collections.nCopies(history.size() + 1, Boolean.TRUE));
        table.add(header, rightAlign);
        final SortedMap<Long, List<ThreadMap.Item>> threadTable = ThreadMap.historyToTable(history);
        for (List<ThreadMap.Item> row : threadTable.values()) {
            final List<String> contentRow = new ArrayList<String>();
            final String threadName = getThreadName(row);
            if (threadName == null) {
                continue;
            }
            for (ThreadMap.Item item : row) {
                if (contentRow.isEmpty()) {
                    contentRow.add(threadName);
                }
                contentRow.add(item == null ? "" : (item.lastCpuUsagePerc == null ? "?" : item.lastCpuUsagePerc.toString()));
            }
            table.add(contentRow, rightAlign);
        }
        sb.append(table.toString());
    }

    private String getThreadName(Collection<ThreadMap.Item> items) {
        for (ThreadMap.Item item : items) {
            if (item != null) {
                return truncate("0x" + item.info.getThreadId() + " " + item.info.getThreadName(), MAX_THREAD_NAME_LENGTH);
            }
        }
        return null;
    }

    private static final int MAX_THREAD_NAME_LENGTH = 48;

    private static String truncate(String str, int maxlen) {
        return str.length() > maxlen ? str.substring(0, maxlen) : str;
    }

    private void printMemoryUsageHistory(StringBuilder sb, List<HistorySample> history) {
        final List<String> header = new ArrayList<String>(Collections.nCopies(history.size() + 1, ""));
        header.set(0, "Memory Used %");
        header.set(1, "old");
        header.set(header.size() - 1, "new");
        final Table table = newTable(history.size() + 1);
        table.setVerticalContentsSeparator(false);
        final List<Boolean> rightAlign = new ArrayList<Boolean>(Collections.nCopies(history.size() + 1, Boolean.TRUE));
        table.add(header, rightAlign);
        final List<List<String>> content = new ArrayList<List<String>>();
        final HistorySample last = history.isEmpty() ? null : history.get(history.size() - 1);
        for (HistorySample.MemoryPools pool: HistorySample.MemoryPools.values()) {
            content.add(new ArrayList<String>(Collections.singletonList(pool.displayable)));
            if (last != null) {
                sb.append(content.get(content.size() - 1).get(0)).append(": ");
                sb.append(MemoryUsages.toString(last.memPoolUsage.get(pool), true));
                newLine(sb);
            }
        }
        newLine(sb);
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
}
