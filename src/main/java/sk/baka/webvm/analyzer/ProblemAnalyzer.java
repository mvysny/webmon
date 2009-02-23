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

import sk.baka.webvm.misc.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes VM problems.
 * @author Martin Vysny
 */
public final class ProblemAnalyzer {

    private ProblemAnalyzer() {
        throw new AssertionError();
    }
    /**
     * The "Deadlocked threads" problem class.
     */
    public static final String CLASS_DEADLOCKED_THREADS = "Deadlocked threads";
    /**
     * The "GC CPU Usage" problem class.
     */
    public static final String CLASS_GC_CPU_USAGE = "GC CPU Usage";
    /**
     * The "GC Memory cleanup" problem class.
     */
    public static final String CLASS_GC_MEMORY_CLEANUP = "GC Memory cleanup";
    /**
     * The "Memory status" problem class.
     */
    public static final String CLASS_MEMORY_STATUS = "Memory status";
    /**
     * If the memory usage goes above this value the {@link #CLASS_MEMORY_STATUS} problem is reported.
     */
    public static final int MEM_USAGE_TRESHOLD = 90;
    /**
     * If the memory usage after GC goes above this value the {@link #CLASS_GC_MEMORY_CLEANUP} problem is reported.
     */
    public static final int MEM_AFTER_GC_USAGE_TRESHOLD = 85;
    private static final int GC_CPU_TRESHOLD = 50;
    private static final int GC_CPU_TRESHOLD_SAMPLES = 3;

    /**
     * Diagnose the VM and returns a list of problem reports.
     * @param history current history.
     * @return the list of reports.
     */
    public static List<ProblemReport> getProblems(final List<HistorySample> history) {
        final List<ProblemReport> result = new ArrayList<ProblemReport>();
        result.add(getDeadlockReport());
        result.add(getGCCPUUsageReport(history));
        result.add(getMemStatReport());
        result.add(getGCMemUsageReport());
        return result;
    }

    private static ProblemReport getGCCPUUsageReport(final List<HistorySample> history) {
        int startFrom = history.size() - GC_CPU_TRESHOLD_SAMPLES;
        if (startFrom < 0) {
            startFrom = 0;
        }
        final int samples = history.size() - startFrom;
        int tresholdCount = 0;
        int avgTreshold = 0;
        for (final HistorySample h : history.subList(startFrom, history.size())) {
            avgTreshold += h.getGcCpuUsage();
            if (h.getGcCpuUsage() >= GC_CPU_TRESHOLD) {
                tresholdCount++;
            }
        }
        avgTreshold = avgTreshold / samples;
        if (tresholdCount >= GC_CPU_TRESHOLD_SAMPLES) {
            return new ProblemReport(true, CLASS_GC_CPU_USAGE, "GC spent more than " + GC_CPU_TRESHOLD + "% (avg. " +
                    avgTreshold + "%) of CPU last " + (GC_CPU_TRESHOLD_SAMPLES * HistorySampler.HISTORY_SAMPLE_RATE_MS / 1000) + " seconds");
        }
        return new ProblemReport(false, CLASS_GC_CPU_USAGE, "Avg. GC CPU usage last " +
                (samples * HistorySampler.HISTORY_SAMPLE_RATE_MS / 1000) + " seconds: " + avgTreshold + "%");
    }

    private static ProblemReport getMemStatReport() {
        final List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
        if (beans == null || beans.isEmpty()) {
            return new ProblemReport(false, CLASS_MEMORY_STATUS, "INFO: No memory pool information");
        }
        final StringBuilder sb = new StringBuilder();
        for (final MemoryPoolMXBean bean : beans) {
            final MemoryUsage usage = bean.getUsage();
            if (usage == null || !bean.isCollectionUsageThresholdSupported() || !bean.isUsageThresholdSupported()) {
                continue;
            }
            final long used = usage.getUsed() * 100 / usage.getMax();
            if (used >= MEM_USAGE_TRESHOLD) {
                sb.append("INFO: Pool [");
                sb.append(bean.getName());
                sb.append("] is now ");
                sb.append(used);
                sb.append("% full\n");
            }
        }
        if (sb.length() == 0) {
            return new ProblemReport(false, CLASS_MEMORY_STATUS, "Heap usage: " + MgmtUtils.getUsagePerc(MgmtUtils.getHeapFromRuntime()));
        }
        sb.append("\nTry performing a GC: this should decrease the memory usage. If not, you may need to increase the memory or check for memory leaks");
        return new ProblemReport(false, CLASS_MEMORY_STATUS, sb.toString());
    }

    private static ProblemReport getGCMemUsageReport() {
        final List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
        if (beans == null || beans.isEmpty()) {
            return new ProblemReport(false, CLASS_GC_MEMORY_CLEANUP, "INFO: No memory pool information");
        }
        final StringBuilder sb = new StringBuilder();
        for (final MemoryPoolMXBean bean : beans) {
            final MemoryUsage usage = bean.getCollectionUsage();
            if (usage == null || !bean.isCollectionUsageThresholdSupported() || !bean.isUsageThresholdSupported()) {
                continue;
            }
            final long used = usage.getUsed() * 100 / usage.getMax();
            if (used >= MEM_AFTER_GC_USAGE_TRESHOLD) {
                sb.append("Pool [");
                sb.append(bean.getName());
                sb.append("] is ");
                sb.append(used);
                sb.append("% full after GC\n");
            }
        }
        if (sb.length() == 0) {
            return new ProblemReport(false, CLASS_GC_MEMORY_CLEANUP, "OK");
        }
        sb.append("\nYou may need to increase the memory or check for memory leaks");
        return new ProblemReport(true, CLASS_GC_MEMORY_CLEANUP, sb.toString());
    }

    private static ProblemReport getDeadlockReport() {
        final StringBuilder sb = new StringBuilder();
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (bean == null) {
            return new ProblemReport(false, CLASS_DEADLOCKED_THREADS, "Unavailable - ThreadMXBean null");
        }
        long[] dt;
        try {
            // unsupported on Java 1.5
            dt = bean.findDeadlockedThreads();
        } catch (Throwable ex) {
            // an ugly way to retain support for Java 1.5 as
            dt = bean.findMonitorDeadlockedThreads();
        }
        if ((dt == null) || (dt.length == 0)) {
            return new ProblemReport(false, CLASS_DEADLOCKED_THREADS, "None");
        }
        for (final long thread : dt) {
            final ThreadInfo info = bean.getThreadInfo(thread);
            sb.append("Locked thread: ");
            sb.append(info.getThreadId());
            sb.append(": ");
            sb.append(info.getThreadName());
            sb.append(" ");
            sb.append(info.getThreadState());
            sb.append('\n');
            final String lockName = info.getLockName();
            if (lockName != null) {
                sb.append("Locked on lock ");
                sb.append(lockName);
                sb.append(" owned by thread ");
                sb.append(info.getLockOwnerId());
                sb.append(": ");
                sb.append(info.getLockOwnerName());
                sb.append('\n');
            }
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
        return new ProblemReport(true, CLASS_DEADLOCKED_THREADS, sb.toString());
    }
}
