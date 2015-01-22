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

import sk.baka.webvm.analyzer.hostos.CPUUsage;
import sk.baka.webvm.analyzer.hostos.ICpuUsageMeasureStrategy;
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
import sk.baka.webvm.analyzer.hostos.Memory;
import sk.baka.webvm.analyzer.utils.BackgroundService;
import sk.baka.webvm.analyzer.utils.Constants;
import sk.baka.webvm.analyzer.utils.INotificationDelivery;
import sk.baka.webvm.analyzer.utils.MemoryUsages;
import sk.baka.webvm.analyzer.utils.SimpleFixedSizeFIFO;

/**
 * Samples the VM history regularly. You need to invoke {@link #start()} to start the sampler, {@link #stop()} to stop it. Thread-safe.
 * @author Martin Vysny
 */
public class HistorySampler extends BackgroundService implements IHistorySampler {

    private static final Logger LOG = Logger.getLogger(HistorySampler.class.getName());
    public final IProblemAnalyzer analyzer;

    /**
     * Creates new sampler instance with default values.
     * @param analyzer a configured instance of the analyzer. May be null if this functionality is not required.
     * @param notificator the notificator, may be null if not needed. 
     */
    public HistorySampler(IProblemAnalyzer analyzer, INotificationDelivery notificator) {
        this(HISTORY_VMSTAT, HISTORY_PROBLEMS, analyzer, notificator);
    }

    /**
     * Creates new sampler instance.
     * @param vmstatConfig the vmstat sampler config
     * @param problemConfig the problem sampler config
     * @param analyzer a configured instance of the analyzer. May be null if this functionality is not required.
     * @param notificator the notificator, may be null if not needed. 
     */
    public HistorySampler(final SamplerConfig vmstatConfig, final SamplerConfig problemConfig, final IProblemAnalyzer analyzer, INotificationDelivery notificator) {
        super("Sampler", 1);
        this.vmstatConfig = vmstatConfig;
        this.problemConfig = problemConfig;
        vmstatHistory = new SimpleFixedSizeFIFO<HistorySample>(vmstatConfig.getHistoryLength());
        problemHistory = new SimpleFixedSizeFIFO<List<ProblemReport>>(problemConfig.getHistoryLength());
        this.analyzer = analyzer;
        this.notificator = notificator;
    }
    private final SamplerConfig problemConfig;
    public final INotificationDelivery notificator;

    /**
     * Sets the new configuration file.
     * @param cfg the new config file.
     */
    @Override
    public void configChanged(Config cfg) {
        if (notificator != null) {
            notificator.configChanged(cfg);
        }
        if (analyzer != null) {
            analyzer.configChanged(cfg);
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
    @Override
    public List<HistorySample> getVmstatHistory() {
        return vmstatHistory.toList();
    }
    /**
     * Serves for Host OS CPU usage measurement.
     */
    private final CPUUsageMeasurer cpuOS = Cpu.newHostCpu();
    /**
     * Serves for Java CPU usage measurement.
     */
    private final CPUUsageMeasurer cpuJava = Cpu.newJavaCpu();
    /**
     * Serves for Host OS CPU IO usage measurement.
     */
    private final CPUUsageMeasurer cpuOSIO = Cpu.newHostIOCpu();
    private final IMemoryInfoProvider meminfo = Memory.getOSMemoryInfoProvider();
    
    /**
     * Invoked when the sample is taken. The default implementation does nothing.
     * @param hs the sample taken, never null.
     */
    protected void onSample(HistorySample hs) {}

    private final class Sampler implements Runnable {

        @Override
        public void run() {
            try {
                final CPUUsage cpuUsageByGC = gcCpuUsage.getCpuUsage();
                assert cpuUsageByGC != null; // this info is always available.
                CPUUsage usage = cpuOS.getCpuUsage();
                usage = usage == null ? CPUUsage.ZERO : usage;
                CPUUsage javaUsage = cpuJava.getCpuUsage();
                javaUsage = javaUsage == null ? CPUUsage.ZERO : javaUsage;
                CPUUsage ioUsage = cpuOSIO.getCpuUsage();
                ioUsage = ioUsage == null ? CPUUsage.ZERO : ioUsage;
                final HistorySample hs = new HistorySample.Builder().
                        setGcCpuUsage(cpuUsageByGC.cpuAvgUsage)
                        .setCpuUsage(usage)
                        .setCpuIOUsage(ioUsage.cpuAvgUsage)
                        .setCpuJavaUsage(javaUsage.cpuAvgUsage)
                        .autodetectMemClassesThreads(meminfo)
                        .build();
                vmstatHistory.add(hs);
                onSample(hs);
            } catch (Throwable e) {
                // catch all throwables as the thread is going to terminate anyway
                LOG.log(Level.SEVERE, "The Sampler thread failed", e);
            }
        }
    }
    private SamplerConfig vmstatConfig;
    private final SimpleFixedSizeFIFO<List<ProblemReport>> problemHistory;
    private final CPUUsageMeasurer gcCpuUsage = new CPUUsageMeasurer(new GCCpuUsageMeasureStrategy());

    /**
     * Measures the GC CPU usage.
     */
    private static final class GCCpuUsageMeasureStrategy implements ICpuUsageMeasureStrategy {

        public boolean supported() {
            return true;
        }

        @Override
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

        @Override
        public CPUUsage getAvgCpuUsage(Object o1, Object o2) {
            final long[] m1 = (long[]) o1;
            final long[] m2 = (long[]) o2;
            final long gcTimeDelta = m2[0] - m1[0];
            final long gcSampleTakenDelta = m2[1] - m1[1];
            if (gcSampleTakenDelta == 0) {
                return CPUUsage.ZERO;
            }
            final int cpuusage = (int) (gcTimeDelta * Constants.HUNDRED_PERCENT / gcSampleTakenDelta);
            return new CPUUsage(cpuusage, cpuusage);
        }
    }

    private final class ProblemSampler implements Runnable {

        @Override
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
    @Override
    public List<List<ProblemReport>> getProblemHistory() {
        return problemHistory.toList();
    }
}
