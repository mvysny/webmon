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

import java.io.File;
import sk.baka.webvm.misc.*;
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
import sk.baka.webvm.config.Config;

/**
 * Analyzes VM problems.
 * @author Martin Vysny
 */
public final class ProblemAnalyzer {

	private final static Logger log = Logger.getLogger(ProblemAnalyzer.class.getName());

	/**
	 * Parses init values from given application.
	 * @param app the application to parse.
	 */
	public void configure(final Config config) {
		this.config = new Config(config);
	}
	private Config config;

	public static int parse(final Properties app, final String argName, final int defaultValue) {
		String arg = app.getProperty(argName);
		if (arg == null) {
			return defaultValue;
		}
		arg = arg.trim();
		try {
			return Integer.parseInt(arg);
		} catch (final Exception ex) {
			log.log(Level.SEVERE, argName + ": failed to parse '" + arg + "'", ex);
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
		return result;
	}

	/**
	 * Prepares the {@link #CLASS_GC_CPU_USAGE} report.
	 * @param history the history
	 * @return report
	 */
	public ProblemReport getGCCPUUsageReport(final List<HistorySample> history) {
		int startFrom = history.size() - config.gcCpuTresholdSamples;
		if (startFrom < 0) {
			startFrom = 0;
		}
		final int samples = history.size() - startFrom;
		int tresholdCount = 0;
		int avgTreshold = 0;
		for (final HistorySample h : history.subList(startFrom, history.size())) {
			avgTreshold += h.getGcCpuUsage();
			if (h.getGcCpuUsage() >= config.gcCpuTreshold) {
				tresholdCount++;
			}
		}
		avgTreshold = samples == 0 ? 0 : avgTreshold / samples;
		if (tresholdCount >= config.gcCpuTresholdSamples) {
			return new ProblemReport(true, CLASS_GC_CPU_USAGE, "GC spent more than " + config.gcCpuTreshold + "% (avg. " +
					avgTreshold + "%) of CPU last " + (config.gcCpuTresholdSamples * HistorySampler.HISTORY_VMSTAT.getHistorySampleDelayMs() / 1000) + " seconds",
					getGcCpuUsageDesc());
		}
		return new ProblemReport(false, CLASS_GC_CPU_USAGE, "Avg. GC CPU usage last " +
				(samples * HistorySampler.HISTORY_VMSTAT.getHistorySampleDelayMs() / 1000) + " seconds: " + avgTreshold + "%",
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
		long[] dt;
		try {
			// unsupported on Java 1.5
			dt = bean.findDeadlockedThreads();
		} catch (Throwable ex) {
			// an ugly way to retain support for Java 1.5
			dt = bean.findMonitorDeadlockedThreads();
		}
		if ((dt == null) || (dt.length == 0)) {
			return new ProblemReport(false, CLASS_DEADLOCKED_THREADS, "None", CLASS_DEADLOCKED_THREADS_DESC);
		}
		for (final long thread : dt) {
			final ThreadInfo info = bean.getThreadInfo(thread, Integer.MAX_VALUE);
			sb.append("Locked thread: ");
			sb.append(info.getThreadId());
			sb.append(": ");
			sb.append(info.getThreadName());
			sb.append(" ");
			sb.append(info.getThreadState());
			if (info.isInNative()) {
				sb.append(" InNative");
			}
			if (info.isSuspended()) {
				sb.append(" Suspended");
			}
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
				final long freeSpaceMb = FileSystemUtils.freeSpaceKb(root.getAbsolutePath()) / 1024;
				if (freeSpaceMb < config.minFreeDiskSpaceMb) {
					problem = true;
					sb.append("Low disk space on ");
					sb.append(root.getAbsolutePath());
					sb.append(": ");
					sb.append(freeSpaceMb);
					sb.append("Mb\n");
				}
			} catch (Exception ex) {
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
