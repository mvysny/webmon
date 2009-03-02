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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.baka.webvm.config.Config;

/**
 * Samples the VM history regularly. You need to invoke {@link #start()} to start the sampler, {@link #stop()} to stop it. Thread-safe.
 * @author Martin Vysny
 */
public final class HistorySampler extends BackgroundService {

	private static final Logger log = Logger.getLogger(HistorySampler.class.getName());
	private final ProblemAnalyzer analyzer;

	/**
	 * Creates new sampler instance with default values.
	 * @param analyzer a configured instance of the analyzer
	 */
	public HistorySampler(final ProblemAnalyzer analyzer) {
		this(HISTORY_VMSTAT, HISTORY_PROBLEMS, analyzer);
	}

	/**
	 * Creates new sampler instance.
	 * @param vmstatConfig the vmstat sampler config
	 * @param problemConfig the problem sampler config
	 * @param analyzer a configured instance of the analyzer
	 */
	public HistorySampler(final SamplerConfig vmstatConfig, final SamplerConfig problemConfig, final ProblemAnalyzer analyzer) {
        super(1);
		this.vmstatConfig = vmstatConfig;
		this.problemConfig = problemConfig;
		vmstatHistory = new SimpleFixedSizeFIFO<HistorySample>(vmstatConfig.getHistoryLength());
		problemHistory = new SimpleFixedSizeFIFO<List<ProblemReport>>(problemConfig.getHistoryLength());
		this.analyzer = analyzer;
	}
	private volatile Config config = new Config();
	private final SamplerConfig problemConfig;
	/**
	 * Default VMStat history.
	 */
	public static final SamplerConfig HISTORY_VMSTAT = new SamplerConfig(150, 1000, 0);
	/**
	 * Default Problems history.
	 */
	public static final SamplerConfig HISTORY_PROBLEMS = new SamplerConfig(20, 10 * 1000, 500);

    private final NotificationDelivery notificator = new NotificationDelivery();

    /**
     * Sets the new configuration file.
     * @param config the new config file.
     */
    public void configure(final Config config) {
        this.config = new Config(config);
        notificator.configure(config);
    }

    @Override
	protected void started(final ScheduledExecutorService executor) {
        notificator.start();
		executor.scheduleWithFixedDelay(new Sampler(), vmstatConfig.getInitialDelay(), vmstatConfig.getHistorySampleDelayMs(), TimeUnit.MILLISECONDS);
		executor.scheduleWithFixedDelay(new ProblemSampler(), problemConfig.getInitialDelay(), problemConfig.getHistorySampleDelayMs(), TimeUnit.MILLISECONDS);
	}

    @Override
    protected void stopped() {
        notificator.stop();
    }

	private final SimpleFixedSizeFIFO<HistorySample> vmstatHistory;

	/**
	 * Returns a snapshot of the history values.
	 * @return modifiable snapshot.
	 */
	public List<HistorySample> getVmstatHistory() {
		return vmstatHistory.toList();
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
				final List<ProblemReport> currentProblems = analyzer.getProblems(vmstatHistory.toList());
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
				notificator.deliverAsync(currentProblems);
			} catch (Throwable e) {
				log.log(Level.SEVERE, "The ProblemSampler timer failed", e);
			}
		}
	}

	public List<List<ProblemReport>> getProblemHistory() {
		return problemHistory.toList();
	}
}
