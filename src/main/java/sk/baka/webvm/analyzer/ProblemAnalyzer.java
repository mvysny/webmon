/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebVM.
 *
 * WebVM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebVM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebVM.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.analyzer;

import sk.baka.webvm.analyzer.hostos.Memory;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FileUtils;
import sk.baka.webvm.ThreadDump;
import sk.baka.webvm.config.Config;
import sk.baka.webvm.misc.MgmtUtils;

/**
 * Analyzes VM problems.
 * @author Martin Vysny
 */
public final class ProblemAnalyzer {

    private static final Logger LOG = Logger.getLogger(ProblemAnalyzer.class.getName());

    private static long[] findDeadlockedThreads(final ThreadMXBean bean) {
        long[] dt;
        try {
            // unsupported on Java 1.5
            dt = bean.findDeadlockedThreads();
        } catch (Throwable ex) {
            // an ugly way to retain support for Java 1.5
            dt = bean.findMonitorDeadlockedThreads();
        }
        return dt;
    }

    /**
     * Parses init values from given application.
     * @param config
     */
    public void configure(final Config config) {
        this.config = new Config(config);
    }
    private Config config = null;

    /**
     * Parses given property and returns it as an integer. Allows default value to be returned in case of null.
     * @param props the properties
     * @param propName the property name
     * @param defaultValue returned when given property is not specified.
     * @return parsed property value
     */
    public static int parse(final Properties props, final String propName, final int defaultValue) {
        String arg = props.getProperty(propName);
        if (arg == null) {
            return defaultValue;
        }
        arg = arg.trim();
        try {
            return Integer.parseInt(arg);
        } catch (final Exception ex) {
            LOG.log(Level.SEVERE, propName + ": failed to parse '" + arg + "'", ex);
        }
        return defaultValue;
    }
    /**
     * The "Free disk space" problem class.
     */
    public static final String CLASS_FREE_DISK_SPACE = "Free disk space";

    private String getFreeDiskSpaceDesc() {
        return "Triggered when there is less than " + config.minFreeDiskSpaceMb + "Mb of free space on some drive";
    }
    /**
     * The "Deadlocked threads" problem class.
     */
    public static final String CLASS_DEADLOCKED_THREADS = "Deadlocked threads";
    private static final String CLASS_DEADLOCKED_THREADS_DESC = "Triggered when there are some deadlocked threads. Finds cycles of threads that are in " +
            "deadlock waiting to acquire object monitors (also " +
            "<a href=\"http://java.sun.com/javase/6/docs/api/java/lang/management/LockInfo.html#OwnableSynchronizer\">ownable synchronizers" +
            "</a> when running on Java 6).";
    /**
     * The "GC CPU Usage" problem class.
     */
    public static final String CLASS_GC_CPU_USAGE = "GC CPU Usage";

    private String getGcCpuUsageDesc() {
        return "Triggered when GC uses " + config.gcCpuTreshold + "% or more of CPU continuously for " +
                (config.gcCpuTresholdSamples * HistorySampler.HISTORY_VMSTAT.getHistorySampleDelayMs() / 1000) + " seconds";
    }
    /**
     * The "GC Memory cleanup" problem class.
     */
    public static final String CLASS_GC_MEMORY_CLEANUP = "GC Memory cleanup";

    private String getGcMemoryCleanupDesc() {
        return "Triggered when GC cannot make available more than " +
                (100 - config.memAfterGcUsageTreshold) + "% of memory";
    }
    /**
     * The "Memory status" problem class.
     */
    public static final String CLASS_MEMORY_USAGE = "Memory usage";

    private String getMemUsageDesc() {
        return "Triggered: never. Reports memory pools which are at least " + config.memUsageTreshold + "% full";
    }
    /**
     * The "Host Memory" problem class.
     */
    public static final String CLASS_HOST_MEMORY_USAGE = "Host Virtual Mem";

    private String getHostMemoryUsageDesc() {
        return "Triggered when host uses " + config.hostVirtMem + "% or more virtual memory";
    }

    /**
     * Diagnose the VM and returns a list of problem reports.
     * @param history current history.
     * @return the list of reports.
     */
    public List<ProblemReport> getProblems(final List<HistorySample> history) {
        final List<ProblemReport> result = new ArrayList<ProblemReport>();
        result.add(getDeadlockReport());
        result.add(getGCCPUUsageReport(history));
        result.add(getMemStatReport());
        result.add(getGCMemUsageReport());
        result.add(getFreeDiskspaceReport());
        result.add(getHostVirtMemReport());
        return result;
    }

    /**
     * Prepares the {@link #CLASS_HOST_MEMORY_USAGE} report.
     * @return report
     */
    public ProblemReport getHostVirtMemReport() {
        final MemoryUsage phys = Memory.getPhysicalMemory();
        if (phys == null) {
            return new ProblemReport(false, CLASS_HOST_MEMORY_USAGE, "Host memory reporting unsupported on this platform", getHostMemoryUsageDesc());
        }
        final boolean cbUnsupported = (phys.getCommitted() == phys.getUsed());
        final StringBuilder sb = new StringBuilder();
        if (cbUnsupported) {
            sb.append("buffers/cache detection not supported, disabled\n");
        }
        final MemoryUsage swap = Memory.getSwap();
        sb.append("Physical memory used: ");
        sb.append(phys.getCommitted() * 100 / phys.getMax());
        sb.append("%, minus buffers/cache: ");
        sb.append(phys.getUsed() * 100 / phys.getMax());
        sb.append("%\nSwap used: ");
        if (swap == null) {
            sb.append("-");
        } else {
            sb.append(swap.getUsed() * 100 / swap.getMax());
        }
        sb.append("%\n");
        final long total = phys.getMax() + (swap == null ? 0 : swap.getMax());
        final long used = phys.getUsed() + (swap == null ? 0 : swap.getUsed());
        final long usedPerc = used * 100 / total;
        sb.append("Total virtual memory usage: ");
        sb.append(usedPerc);
        sb.append('%');
        final boolean isProblem = !cbUnsupported && (usedPerc >= config.hostVirtMem);
        return new ProblemReport(isProblem, CLASS_HOST_MEMORY_USAGE, sb.toString(), getHostMemoryUsageDesc());
    }

    /**
     * Prepares the {@link #CLASS_GC_CPU_USAGE} report.
     * @param history the history
     * @return report
     */
    public ProblemReport getGCCPUUsageReport(final List<HistorySample> history) {
        int tresholdViolationCount = 0;
        int maxTresholdViolationCount = 0;
        int totalAvgTreshold = 0;
        int avgTresholdViolation = 0;
        int maxAvgTresholdViolation = 0;
        for (final HistorySample h : history) {
            totalAvgTreshold += h.gcCpuUsage;
            if (h.gcCpuUsage >= config.gcCpuTreshold) {
                tresholdViolationCount++;
                avgTresholdViolation += h.gcCpuUsage;
                if (maxTresholdViolationCount < tresholdViolationCount) {
                    maxTresholdViolationCount = tresholdViolationCount;
                    maxAvgTresholdViolation = avgTresholdViolation;
                }
            } else {
                tresholdViolationCount = 0;
                avgTresholdViolation = 0;
            }
        }
        maxAvgTresholdViolation = maxTresholdViolationCount == 0 ? 0 : maxAvgTresholdViolation / maxTresholdViolationCount;
        totalAvgTreshold = history.size() == 0 ? 0 : totalAvgTreshold / history.size();
        if (maxTresholdViolationCount >= config.gcCpuTresholdSamples) {
            return new ProblemReport(true, CLASS_GC_CPU_USAGE, "GC spent more than " + config.gcCpuTreshold + "% (avg. " +
                    maxAvgTresholdViolation + "%) of CPU for " + (maxTresholdViolationCount * HistorySampler.HISTORY_VMSTAT.getHistorySampleDelayMs() / 1000) + " seconds",
                    getGcCpuUsageDesc());
        }
        return new ProblemReport(false, CLASS_GC_CPU_USAGE, "Avg. GC CPU usage last " +
                (history.size() * HistorySampler.HISTORY_VMSTAT.getHistorySampleDelayMs() / 1000) + " seconds: " + totalAvgTreshold + "%",
                getGcCpuUsageDesc());
    }

    /**
     * Prepares the {@link #CLASS_MEMORY_STATUS} report.
     * @return report
     */
    public ProblemReport getMemStatReport() {
        final List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
        if (beans == null || beans.isEmpty()) {
            return new ProblemReport(false, CLASS_MEMORY_USAGE, "INFO: No memory pool information", getMemUsageDesc());
        }
        final StringBuilder sb = new StringBuilder();
        for (final MemoryPoolMXBean bean : beans) {
            final MemoryUsage usage = bean.getUsage();
            if (usage == null || !bean.isCollectionUsageThresholdSupported() || !bean.isUsageThresholdSupported()) {
                continue;
            }
            final long used = usage.getUsed() * 100 / usage.getMax();
            if (used >= config.memUsageTreshold) {
                sb.append("INFO: Pool [");
                sb.append(bean.getName());
                sb.append("] is now ");
                sb.append(used);
                sb.append("% full\n");
            }
        }
        if (sb.length() == 0) {
            return new ProblemReport(false, CLASS_MEMORY_USAGE, "Heap usage: " + MgmtUtils.getUsagePerc(MgmtUtils.getHeapFromRuntime()), getMemUsageDesc());
        }
        sb.append("\nTry performing a GC: this should decrease the memory usage. If not, you may need to increase the memory or check for memory leaks");
        return new ProblemReport(false, CLASS_MEMORY_USAGE, sb.toString(), getMemUsageDesc());
    }

    /**
     * Prepares the {@link #CLASS_GC_MEMORY_CLEANUP} report.
     * @return report
     */
    public ProblemReport getGCMemUsageReport() {
        final List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
        if (beans == null || beans.isEmpty()) {
            return new ProblemReport(false, CLASS_GC_MEMORY_CLEANUP, "INFO: No memory pool information", getGcMemoryCleanupDesc());
        }
        final StringBuilder sb = new StringBuilder();
        for (final MemoryPoolMXBean bean : beans) {
            final MemoryUsage usage = bean.getCollectionUsage();
            if (usage == null || !bean.isCollectionUsageThresholdSupported() || !bean.isUsageThresholdSupported()) {
                continue;
            }
            if (usage.getMax() < 1) {
                continue;
            }
            final long used = usage.getUsed() * 100 / usage.getMax();
            if (used >= config.memAfterGcUsageTreshold) {
                sb.append("Pool [");
                sb.append(bean.getName());
                sb.append("] is ");
                sb.append(used);
                sb.append("% full after GC\n");
            }
        }
        if (sb.length() == 0) {
            return new ProblemReport(false, CLASS_GC_MEMORY_CLEANUP, "OK", getGcMemoryCleanupDesc());
        }
        sb.append("\nYou may need to increase the memory or check for memory leaks");
        return new ProblemReport(true, CLASS_GC_MEMORY_CLEANUP, sb.toString(), getGcMemoryCleanupDesc());
    }

    /**
     * Prepares the {@link #CLASS_DEADLOCKED_THREADS} report.
     * @return report
     */
    public static ProblemReport getDeadlockReport() {
        final StringBuilder sb = new StringBuilder();
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (bean == null) {
            return new ProblemReport(false, CLASS_DEADLOCKED_THREADS, "INFO: Report Unavailable - ThreadMXBean null", CLASS_DEADLOCKED_THREADS_DESC);
        }
        final long[] dt = findDeadlockedThreads(bean);
        if ((dt == null) || (dt.length == 0)) {
            return new ProblemReport(false, CLASS_DEADLOCKED_THREADS, "None", CLASS_DEADLOCKED_THREADS_DESC);
        }
        for (final long thread : dt) {
            final ThreadInfo info = bean.getThreadInfo(thread, Integer.MAX_VALUE);
            sb.append("Locked thread: ");
            sb.append(ThreadDump.getThreadMetadata(info));
            sb.append('\n');
            sb.append("Stacktrace:");
            final StackTraceElement[] trace = info.getStackTrace();
            if (trace == null || trace.length == 0) {
                sb.append("unknown\n");
            } else {
                sb.append('\n');
                for (int i = 0; i < trace.length; i++) {
                    sb.append("\tat ");
                    sb.append(trace[i]);
                    sb.append('\n');
                }
            }
        }
        return new ProblemReport(true, CLASS_DEADLOCKED_THREADS, sb.toString(), CLASS_DEADLOCKED_THREADS_DESC);
    }

    /**
     * Analyzes free disk space.
     * @return free disk space report.
     */
    public ProblemReport getFreeDiskspaceReport() {
        final StringBuilder sb = new StringBuilder();
        boolean problem = false;
        for (final File root : File.listRoots()) {
            try {
                final long freeSpaceKb = FileSystemUtils.freeSpaceKb(root.getAbsolutePath());
                final long freeSpaceMb = freeSpaceKb / 1024;
                if (freeSpaceMb < config.minFreeDiskSpaceMb) {
                    problem = true;
                    sb.append("Low disk space: ");
                }
                sb.append(root.getAbsolutePath());
                sb.append("  ");
                sb.append(FileUtils.byteCountToDisplaySize(freeSpaceKb * 1024));
                sb.append(" free\n");
            } catch (Exception ex) {
                LOG.log(Level.INFO, "Failed to get free space on " + root.getAbsolutePath(), ex);
                sb.append("Failed to get free space on ");
                sb.append(root.getAbsolutePath());
                sb.append(": ");
                sb.append(ex.toString());
                sb.append("\n");
            }
        }
        if (sb.length() == 0) {
            sb.append("OK");
        }
        return new ProblemReport(problem, CLASS_FREE_DISK_SPACE, sb.toString(), getFreeDiskSpaceDesc());
    }
}
