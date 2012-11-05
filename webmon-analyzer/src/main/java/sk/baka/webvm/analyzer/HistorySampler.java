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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.baka.webvm.analyzer.config.Config;
import sk.baka.webvm.analyzer.hostos.Cpu;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;
import sk.baka.webvm.analyzer.utils.BackgroundService;
import sk.baka.webvm.analyzer.utils.Constants;
import sk.baka.webvm.analyzer.utils.INotificationDelivery;
import sk.baka.webvm.analyzer.utils.MgmtUtils;
import sk.baka.webvm.analyzer.utils.SimpleFixedSizeFIFO;

/**
 * Samples the VM history regularly. You need to invoke {@link #start()} to start the sampler, {@link #stop()} to stop it. Thread-safe.
 * @author Martin Vysny
 */
public class HistorySampler extends BackgroundService implements IHistorySampler {

    private static final Logger LOG = Logger.getLogger(HistorySampler.class.getName());
    private final IProblemAnalyzer analyzer;

    /**
     * Creates new sampler instance with default values.
     * @param meminfo memory info provider, not null.
     * @param analyzer a configured instance of the analyzer. May be null if this functionality is not required.
     * @param notificator the notificator, may be null if not needed. 
     */
    public HistorySampler(IMemoryInfoProvider meminfo, IProblemAnalyzer analyzer, INotificationDelivery notificator) {
        this(HISTORY_VMSTAT, HISTORY_PROBLEMS, meminfo, analyzer, notificator);
    }

    /**
     * Creates new sampler instance.
     * @param vmstatConfig the vmstat sampler config
     * @param problemConfig the problem sampler config
     * @param meminfo memory info provider, not null.
     * @param analyzer a configured instance of the analyzer. May be null if this functionality is not required.
     * @param notificator the notificator, may be null if not needed. 
     */
    public HistorySampler(final SamplerConfig vmstatConfig, final SamplerConfig problemConfig, final IMemoryInfoProvider meminfo, final IProblemAnalyzer analyzer, INotificationDelivery notificator) {
        super("Sampler", 1);
        this.vmstatConfig = vmstatConfig;
        this.problemConfig = problemConfig;
        vmstatHistory = new SimpleFixedSizeFIFO<HistorySample>(vmstatConfig.getHistoryLength());
        problemHistory = new SimpleFixedSizeFIFO<List<ProblemReport>>(problemConfig.getHistoryLength());
        this.analyzer = analyzer;
        this.notificator = notificator;
    }
    private final SamplerConfig problemConfig;
    private final INotificationDelivery notificator;

    /**
     * Sets the new configuration file.
     * @param config the new config file.
     */
    public void configChanged(Config cfg) {
        if (notificator != null) {
            notificator.configChanged(cfg);
        }
    }

    @Override
    protected void started(final ScheduledExecutorService executor) {
        if (notificator != null) {
            notificator.start();
        }
        executor.scheduleWithFixedDelay(new Sampler(), vmstatConfig.getInitialDelay(), vmstatConfig.getHistorySampleDelayMs(), TimeUnit.MILLISECONDS);
        if (analyzer != null) {
        executor.scheduleWithFixedDelay(new ProblemSampler(), problemConfig.getInitialDelay(), problemConfig.getHistorySampleDelayMs(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    protected void stopped() {
        if (notificator != null) {
            notificator.stop();
        }
    }
    private final SimpleFixedSizeFIFO<HistorySample> vmstatHistory;

    /**
     * Returns a snapshot of the history values.
     * @return modifiable snapshot.
     */
    public List<HistorySample> getVmstatHistory() {
        return vmstatHistory.toList();
    }
    /**
     * Serves for Host OS CPU usage measurement.
     */
    private final CpuUsage cpuOS = Cpu.newHostCpu();
    /**
     * Serves for Java CPU usage measurement.
     */
    private final CpuUsage cpuJava = Cpu.newJavaCpu();
    /**
     * Serves for Host OS CPU IO usage measurement.
     */
    private final CpuUsage cpuOSIO = Cpu.newHostIOCpu();
    private final IMemoryInfoProvider meminfo = MgmtUtils.getMemoryInfoProvider();

    private final class Sampler implements Runnable {

        public void run() {
            try {
                final int cpuUsageByGC = gcCpuUsage.getCpuUsage();
                int usage = cpuOS.getCpuUsage();
                usage = usage < 0 ? 0 : usage;
                int javaUsage = cpuJava.getCpuUsage();
                javaUsage = javaUsage < 0 ? 0 : javaUsage;
                int ioUsage = cpuOSIO.getCpuUsage();
                ioUsage = ioUsage < 0 ? 0 : ioUsage;
                vmstatHistory.add(new HistorySample(cpuUsageByGC, usage, javaUsage, ioUsage, meminfo));
            } catch (Throwable e) {
                // catch all throwables as the thread is going to terminate anyway
                LOG.log(Level.SEVERE, "The Sampler thread failed", e);
            }
        }
    }
    private SamplerConfig vmstatConfig;
    private final SimpleFixedSizeFIFO<List<ProblemReport>> problemHistory;
    private final CpuUsage gcCpuUsage = new CpuUsage(new GCCpuUsageMeasure());

    /**
     * Measures the GC CPU usage.
     */
    private static final class GCCpuUsageMeasure implements ICpuUsageMeasure {

        public boolean supported() {
            return true;
        }

        public Object measure() throws Exception {
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
            return new long[]{collectTime, currentTimeMillis};
        }

        public int getAvgCpuUsage(Object o1, Object o2) {
            final long[] m1 = (long[]) o1;
            final long[] m2 = (long[]) o2;
            final long gcTimeDelta = m2[0] - m1[0];
            final long gcSampleTakenDelta = m2[1] - m1[1];
            if (gcSampleTakenDelta == 0) {
                return 0;
            }
            return (int) (gcTimeDelta * Constants.HUNDRED_PERCENT / gcSampleTakenDelta);
        }
    }

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
                if (notificator != null) {
                    notificator.deliverAsync(currentProblems);
                }
            } catch (Throwable e) {
                // catch all throwables as the thread is going to terminate anyway
                LOG.log(Level.SEVERE, "The ProblemSampler timer failed", e);
            }
        }
    }

    /**
     * Returns a snapshot view over the problem history.
     * @return the history.
     */
    public List<List<ProblemReport>> getProblemHistory() {
        return problemHistory.toList();
    }
}