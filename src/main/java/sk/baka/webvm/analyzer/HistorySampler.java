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
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Samples the VM history regularly. You need to invoke {@link #start()} to start the sampler, {@link #stop()} to stop it. Thread-safe.
 * @author Martin Vysny
 */
public final class HistorySampler {

	private static final Logger log = Logger.getLogger(HistorySampler.class.getName());

	/**
	 * Creates new sampler instance with default values.
	 */
	public HistorySampler() {
		this(HISTORY_VMSTAT, HISTORY_PROBLEMS);
	}

	/**
	 * Creates new sampler instance.
	 * @param vmstatConfig the vmstat sampler config
	 * @param problemConfig the problem sampler config
	 */
	public HistorySampler(final SamplerConfig vmstatConfig, final SamplerConfig problemConfig) {
		this.vmstatConfig = vmstatConfig;
		this.problemConfig = problemConfig;
		vmstatHistory = new SimpleFixedSizeFIFO<HistorySample>(vmstatConfig.getHistoryLength());
		problemHistory = new SimpleFixedSizeFIFO<List<ProblemReport>>(problemConfig.getHistoryLength());
	}
	private final SamplerConfig problemConfig;
	/**
	 * Default VMStat history.
	 */
	public static final SamplerConfig HISTORY_VMSTAT = new SamplerConfig(150, 1000, 0);
	/**
	 * Default Problems history.
	 */
	public static final SamplerConfig HISTORY_PROBLEMS = new SamplerConfig(20, 30 * 1000, 500);

	/**
	 * Starts the sampling process in a background thread.
	 */
	public void start() {
		if (executor != null) {
			throw new IllegalStateException("Already started.");
		}
		executor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {

			private final ThreadFactory def = Executors.defaultThreadFactory();

			public Thread newThread(Runnable r) {
				final Thread result = def.newThread(r);
				result.setDaemon(true);
				return result;
			}
		});
		executor.scheduleWithFixedDelay(new Sampler(), vmstatConfig.getInitialDelay(), vmstatConfig.getHistorySampleDelayMs(), TimeUnit.MILLISECONDS);
		executor.scheduleWithFixedDelay(new ProblemSampler(), problemConfig.getInitialDelay(), problemConfig.getHistorySampleDelayMs(), TimeUnit.MILLISECONDS);
	}
	private ScheduledThreadPoolExecutor executor = null;
	private final SimpleFixedSizeFIFO<HistorySample> vmstatHistory;

	/**
	 * Returns a snapshot of the history values.
	 * @return modifiable snapshot.
	 */
	public List<HistorySample> getVmstatHistory() {
		return vmstatHistory.toList();
	}

	/**
	 * Disposes of this sampler. This instance is no longer usable and cannot be started again.
	 */
	public void stop() {
		executor.shutdownNow();
		executor = null;
		vmstatHistory.clear();
		problemHistory.clear();
	}

	private final class Sampler implements Runnable {

		private long lastGcTimes = -1;
		private long lastGcSampleTaken = -1;

		public void run() {
			try {
				final int cpuUsageByGC = getGCCPUUsage();
				vmstatHistory.add(new HistorySample(cpuUsageByGC, (int) (MgmtUtils.getHeapFromRuntime().getUsed() / 1024 / 1024)));
			} catch (Throwable e) {
				log.log(Level.SEVERE, "The Sampler thread failed", e);
			}
		}

		private int getGCCPUUsage() {
			// get the GC CPU usage
			long collectTime = 0;
			final List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
			if (beans != null) {
				for (final GarbageCollectorMXBean bean : beans) {
					if (!bean.isValid()) {
						continue;
					}
					if (bean.getCollectionTime() > 0) {
						collectTime += bean.getCollectionTime();
					}
				}
			}
			final long currentTimeMillis = System.currentTimeMillis();
			if (lastGcTimes < 0) {
				lastGcTimes = collectTime;
				lastGcSampleTaken = currentTimeMillis;
			}
			final long gcTimeDelta = collectTime - lastGcTimes;
			final long gcSampleTakenDelta = currentTimeMillis - lastGcSampleTaken;
			lastGcTimes = collectTime;
			lastGcSampleTaken = currentTimeMillis;
			final int cpuUsageByGC;
			if (gcSampleTakenDelta > 0) {
				cpuUsageByGC = (int) (gcTimeDelta * 100 / gcSampleTakenDelta);
			} else {
				cpuUsageByGC = 0;
			}
			return cpuUsageByGC;
		}
	}
	private SamplerConfig vmstatConfig;
	private final SimpleFixedSizeFIFO<List<ProblemReport>> problemHistory;

	private final class ProblemSampler implements Runnable {

		public void run() {
			try {
				final List<ProblemReport> currentProblems = ProblemAnalyzer.getProblems(vmstatHistory.toList());
				final List<ProblemReport> last = problemHistory.getNewest();
				if (last == null) {
					if (!ProblemReport.isProblem(currentProblems)) {
						return;
					}
				} else {
					if (ProblemReport.equals(last, currentProblems)) {
						return;
					}
				}
				problemHistory.add(currentProblems);
			} catch (Throwable e) {
				log.log(Level.SEVERE, "The ProblemSampler timer failed", e);
			}
		}
	}

	public List<List<ProblemReport>> getProblemHistory() {
		return problemHistory.toList();
	}
}
