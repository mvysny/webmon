/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebMon is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebMon.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.analyzer;

import org.jetbrains.annotations.NotNull;
import sk.baka.webvm.analyzer.config.Config;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;
import sk.baka.webvm.analyzer.hostos.Memory;
import sk.baka.webvm.analyzer.utils.Constants;
import sk.baka.webvm.analyzer.utils.MemoryUsage2;
import sk.baka.webvm.analyzer.utils.MemoryUsages;
import sk.baka.webvm.analyzer.utils.MiscUtils;
import sk.baka.webvm.analyzer.utils.Threads;

import java.io.File;
import java.lang.management.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Analyzes VM problems.
 * @author Martin Vysny
 */
public class ProblemAnalyzer implements IProblemAnalyzer {

    public ProblemAnalyzer(Config cfg, IMemoryInfoProvider meminfo) {
        this.config = cfg;
        this.meminfo = meminfo;
    }
    
    private static final Logger LOG = Logger.getLogger(ProblemAnalyzer.class.getName());

    /**
     * Finds deadlocked threads. Uses {@link ThreadMXBean#findDeadlockedThreads} on 1.6, falls back to {@link ThreadMXBean#findMonitorDeadlockedThreads()} on 1.5.
     * @param bean the thread mx bean
     * @return ids of deadlocked threads, null if no threads are deadlocked.
     */
    public static long[] findDeadlockedThreads(final ThreadMXBean bean) {
        long[] dt;
        // try to get the "findDeadlockedThreads()" 1.6 method.
        Method m = null;
        try {
            m = ThreadMXBean.class.getMethod("findDeadlockedThreads");
        } catch (NoSuchMethodException ex) {
            // nope. We are on 1.5. We'll use findMonitorDeadlockedThreads()
        }
        if (m != null) {
            try {
                dt = (long[]) m.invoke(bean);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            dt = bean.findMonitorDeadlockedThreads();
        }
        return dt;
    }

    /**
     * Notifies the component that the config file has been changed.
     */
    @Override
    public synchronized void configChanged(Config cfg) {
        this.config = cfg;
    }

    private volatile Config config;
    
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
    private static final String CLASS_DEADLOCKED_THREADS_DESC = "Triggered when there are some deadlocked threads. Finds cycles of threads that are in "
            + "deadlock waiting to acquire object monitors (also "
            + "<a href=\"http://java.sun.com/javase/6/docs/api/java/lang/management/LockInfo.html#OwnableSynchronizer\">ownable synchronizers"
            + "</a> when running on Java 6).";
    /**
     * The "GC CPU Usage" problem class.
     */
    public static final String CLASS_GC_CPU_USAGE = "GC CPU Usage";

    private String getGcCpuUsageDesc() {
        return "Triggered when GC uses " + config.gcCpuTreshold + "% or more of CPU continuously for "
                + (config.gcCpuTresholdSamples * HistorySampler.HISTORY_VMSTAT.getHistorySampleDelayMs() / Constants.MILLIS_IN_SECOND) + " seconds";
    }

    /**
     * The "CPU Usage" problem class.
     */
    public static final String CLASS_CPU_USAGE = "CPU Usage";

    private String getCpuUsageDesc() {
        return "Triggered when a CPU core is used for " + config.cpuTreshold + "% or more, continuously for "
                + (config.cpuTresholdSamples * HistorySampler.HISTORY_VMSTAT.getHistorySampleDelayMs() / Constants.MILLIS_IN_SECOND) + " seconds";
    }
    /**
     * The "GC Memory cleanup" problem class.
     */
    public static final String CLASS_GC_MEMORY_CLEANUP = "GC Memory cleanup";

    private String getGcMemoryCleanupDesc() {
        return "Triggered when GC cannot make available more than "
                + (Constants.HUNDRED_PERCENT - config.memAfterGcUsageTreshold) + "% of memory";
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
    @Override
    public List<ProblemReport> getProblems(final List<HistorySample> history) {
        final List<ProblemReport> result = new ArrayList<ProblemReport>();
        result.add(getDeadlockReport());
        result.add(getGCCPUUsageReport(history));
        result.add(getCPUUsageReport(history));
        result.add(getMemStatReport());
        result.add(getGCMemUsageReport());
        result.add(getFreeDiskspaceReport());
        result.add(getHostVirtMemReport());
        return result;
    }
    private static final Logger log = Logger.getLogger(ProblemAnalyzer.class.getName());
    private final IMemoryInfoProvider meminfo;
    /**
     * Prepares the {@link #CLASS_HOST_MEMORY_USAGE} report.
     * @return report
     */
    public ProblemReport getHostVirtMemReport() {
        final StringBuilder sb = new StringBuilder();
        boolean isProblem = false;
        // buffer/cache information is available?
        boolean cbAvailable = false;
        MemoryUsage2 phys = MemoryUsage2.ZERO;
        try {
            phys = meminfo.getPhysicalMemory();
            if (phys == null) {
                return new ProblemReport(false, CLASS_HOST_MEMORY_USAGE, "Host memory reporting unsupported on this platform", getHostMemoryUsageDesc());
            }
            cbAvailable = !(phys.getCommitted() == phys.getUsed());
            if (!cbAvailable) {
                sb.append("buffers/cache detection not supported, disabled\n");
            }
            sb.append("Physical memory used: ");
            sb.append(phys.getCommitted() * Constants.HUNDRED_PERCENT / phys.getMax());
            sb.append("%, minus buffers/cache: ");
            sb.append(phys.getUsed() * Constants.HUNDRED_PERCENT / phys.getMax());
            sb.append("%\n");
        } catch (Throwable t) {
            sb.append("Failed to obtain physical memory usage: ").append(t).append("\n");
            log.log(Level.CONFIG, "Failed to obtain physical memory usage", t);
            isProblem = true;
        }
        sb.append("Swap used: ");
        MemoryUsage2 swap = MemoryUsage2.ZERO;
        try {
            swap = meminfo.getSwap();
            sb.append(MemoryUsages.getUsagePerc(swap));
        } catch (Throwable t) {
            sb.append("Failed to obtain swap usage: ").append(t);
            log.log(Level.CONFIG, "Failed to obtain swap usage", t);
            isProblem = true;
        }
        sb.append('\n');
        final long total = phys.getMax() + (swap == null ? 0 : swap.getMax());
        final long used = phys.getUsed() + (swap == null ? 0 : swap.getUsed());
        final long usedPerc = used * Constants.HUNDRED_PERCENT / total;
        sb.append("Total virtual memory usage: ");
        sb.append(usedPerc);
        sb.append('%');
        isProblem |= cbAvailable && (usedPerc >= config.hostVirtMem);
        return new ProblemReport(isProblem, CLASS_HOST_MEMORY_USAGE, sb.toString(), getHostMemoryUsageDesc());
    }

    /**
     * Prepares the {@link #CLASS_GC_CPU_USAGE} report.
     * @param history the history
     * @return report
     */
    public ProblemReport getGCCPUUsageReport(final List<HistorySample> history) {
        final Stats stats = getStats(history, new IntFunction() {
            @Override
            public int get(@NotNull HistorySample sample) {
                return sample.gcCpuUsage;
            }
        }, config.gcCpuTreshold, config.gcCpuTresholdSamples);
        if (stats.isThresholdTriggered()) {
            return new ProblemReport(true, CLASS_GC_CPU_USAGE, "GC spent more than " + config.gcCpuTreshold + "% (avg. "
                    + stats.thresholdValueAvg + "%) of CPU for " + stats.thresholdViolationSeconds + " seconds",
                    getGcCpuUsageDesc());
        }
        return new ProblemReport(false, CLASS_GC_CPU_USAGE, "Avg. GC CPU usage last "
                + stats.historyLengthSeconds + " seconds: " + stats.avg + "%",
                getGcCpuUsageDesc());
    }

    /**
     * Prepares the {@link #CLASS_CPU_USAGE} report.
     * @param history the history
     * @return report
     */
    public ProblemReport getCPUUsageReport(final List<HistorySample> history) {
        final Stats stats = getStats(history, new IntFunction() {
            @Override
            public int get(@NotNull HistorySample sample) {
                return sample.cpuUsage.cpuMaxCoreUsage;
            }
        }, config.cpuTreshold, config.cpuTresholdSamples);
        if (stats.isThresholdTriggered()) {
            return new ProblemReport(true, CLASS_CPU_USAGE, "A CPU core spent more than " + config.cpuTreshold + "% (avg. "
                    + stats.thresholdValueAvg + "%) of CPU for " + stats.thresholdViolationSeconds + " seconds",
                    getCpuUsageDesc());
        }
        return new ProblemReport(false, CLASS_CPU_USAGE, "Avg. max CPU core usage last "
                + stats.historyLengthSeconds + " seconds: " + stats.avg + "%",
                getCpuUsageDesc());
    }

    private static class Stats {
        public final int historyLengthSeconds;
        /**
         * Average value.
         */
        public final int avg;
        /**
         * If the value threshold has been triggered, this is greater than zero.
         */
        public final int thresholdViolationSeconds;
        /**
         * If the value threshold has been triggered, this is greater than zero.
         */
        public final int thresholdValueAvg;

        public Stats(int historyLengthSeconds, int avg, int thresholdViolationSeconds, int thresholdValueAvg) {
            this.historyLengthSeconds = historyLengthSeconds;
            this.avg = avg;
            this.thresholdValueAvg = thresholdValueAvg;
            this.thresholdViolationSeconds = thresholdViolationSeconds;
        }
        public static final Stats ZERO = new Stats(0, 0, 0, 0);

        public boolean isThresholdTriggered() {
            return thresholdViolationSeconds > 0;
        }
    }
    private static interface IntFunction {
        int get(@NotNull HistorySample sample);
    }

    /**
     * Spocita statistiku hodnoty
     * @param history historia, not null
     * @param f funkcia ktora vracia z history sample hodnotu, ktoru analyzujeme.
     * @param thresholdValue ak je thresholdSamples poslednych hodnot viac alebo rovnych thresholdValue, problem.
     * @param thresholdSamples ak je thresholdSamples poslednych hodnot viac alebo rovnych thresholdValue, problem.
     * @return stats, not null.
     */
    @NotNull
    private static Stats getStats(final List<HistorySample> history, @NotNull final IntFunction f, int thresholdValue, int thresholdSamples) {
        if (history.isEmpty()) {
            return Stats.ZERO;
        }
        int total = 0;
        int currentTresholdViolationCount = 0;
        int currentTresholdViolationSum = 0;
        for (int i = 0; i < history.size(); i++) {
            final int val = f.get(history.get(i));
            total += val;
            if (val >= thresholdValue) {
                currentTresholdViolationCount++;
                currentTresholdViolationSum += val;
            } else {
                currentTresholdViolationCount = 0;
                currentTresholdViolationSum = 0;
            }
        }
        if (currentTresholdViolationCount < thresholdSamples) {
            currentTresholdViolationCount = 0;
            currentTresholdViolationSum = 0;
        }
        final int historyLengthInSeconds = history.size() * HistorySampler.HISTORY_VMSTAT.getHistorySampleDelayMs() / Constants.MILLIS_IN_SECOND;
        final int thresholdViolationSeconds = currentTresholdViolationCount * HistorySampler.HISTORY_VMSTAT.getHistorySampleDelayMs() / Constants.MILLIS_IN_SECOND;
        return new Stats(historyLengthInSeconds,
                total / history.size(),
                thresholdViolationSeconds,
                currentTresholdViolationCount > 0 ? currentTresholdViolationSum / currentTresholdViolationCount : 0);
    }

    /**
     * Prepares the {@link #CLASS_MEMORY_USAGE} report.
     * @return report
     */
    public ProblemReport getMemStatReport() {
        final List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
        if (MiscUtils.isBlank(beans)) {
            return new ProblemReport(false, CLASS_MEMORY_USAGE, "INFO: No memory pool information", getMemUsageDesc());
        }
        final StringBuilder sb = new StringBuilder();
        for (final MemoryPoolMXBean bean : beans) {
            final MemoryUsage usage = bean.getUsage();
            if (usage == null || !bean.isCollectionUsageThresholdSupported() || !bean.isUsageThresholdSupported()) {
                continue;
            }
            final long used = usage.getUsed() * Constants.HUNDRED_PERCENT / usage.getMax();
            if (used >= config.memUsageTreshold) {
                sb.append("INFO: Pool [");
                sb.append(bean.getName());
                sb.append("] is now ");
                sb.append(used);
                sb.append("% full\n");
            }
        }
        if (sb.length() == 0) {
            return new ProblemReport(false, CLASS_MEMORY_USAGE, "Heap usage: " + MemoryUsages.getUsagePerc(Memory.getHeapFromRuntime()), getMemUsageDesc());
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
        if (MiscUtils.isBlank(beans)) {
            return new ProblemReport(false, CLASS_GC_MEMORY_CLEANUP, "INFO: No memory pool information", getGcMemoryCleanupDesc());
        }
        final StringBuilder sb = new StringBuilder();
        for (final MemoryPoolMXBean bean : beans) {
            final MemoryUsage usage = bean.getCollectionUsage();
            if (usage == null || !bean.isCollectionUsageThresholdSupported() || !bean.isUsageThresholdSupported()) {
                continue;
            }
            if (usage.getMax() <= 0) {
                continue;
            }
            final long used = usage.getUsed() * Constants.HUNDRED_PERCENT / usage.getMax();
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
        if (dt == null || dt.length == 0) {
            return new ProblemReport(false, CLASS_DEADLOCKED_THREADS, "None", CLASS_DEADLOCKED_THREADS_DESC);
        }
        for (final long thread : dt) {
            final ThreadInfo info = bean.getThreadInfo(thread, Integer.MAX_VALUE);
            sb.append("Locked thread: ");
            sb.append(Threads.getThreadMetadata(info));
            sb.append('\n');
            sb.append("Stacktrace:");
            sb.append(Threads.getThreadStacktrace(info));
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
        for (final File root : MiscUtils.getLocalHarddrives()) {
            try {
                final long freeSpace = root.getFreeSpace();  // breaks java 1.5 compatibility
                final long freeSpaceMb = freeSpace / Constants.KIBIBYTES /  Constants.KIBIBYTES;
                if (freeSpaceMb < config.minFreeDiskSpaceMb) {
                    problem = true;
                    sb.append("Low disk space: ");
                }
                sb.append(root.getAbsolutePath());
                sb.append("  ");
                sb.append(freeSpaceMb);
                sb.append("mB free\n");
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
        return new ProblemReport(problem, CLASS_FREE_DISK_SPACE, sb.toString().trim(), getFreeDiskSpaceDesc());
    }
}
