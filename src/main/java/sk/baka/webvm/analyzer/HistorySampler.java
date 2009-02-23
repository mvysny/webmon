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
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Samples the VM history regularly. You need to invoke {@link #start()} to start the sampler, {@link #stop()} to stop it. Thread-safe.
 * @author Martin Vysny
 */
public final class HistorySampler {

    /**
     * Number of samples to keep.
     */
    public static final int HISTORY_LENGTH = 150;
    /**
     * History sample is taken each {@value #HISTORY_SAMPLE_RATE_MS} millis.
     */
    public static final int HISTORY_SAMPLE_RATE_MS = 1000;

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
        executor.scheduleWithFixedDelay(new Sampler(), 0, HISTORY_SAMPLE_RATE_MS, TimeUnit.MILLISECONDS);
    }
    private ScheduledThreadPoolExecutor executor = null;
    private final Queue<HistorySample> history = new ConcurrentLinkedQueue<HistorySample>();

    /**
     * Returns a snapshot of the history values.
     * @return modifiable snapshot.
     */
    public List<HistorySample> getHistory() {
        return new ArrayList<HistorySample>(history);
    }

    /**
     * Disposes of this sampler. This instance is no longer usable and cannot be started again.
     */
    public void stop() {
        executor.shutdownNow();
        executor = null;
        history.clear();
    }

    private final class Sampler implements Runnable {

        private int historyLength = 0;
        private long lastGcTimes = -1;
        private long lastGcSampleTaken = -1;

        public void run() {
            while (historyLength > HISTORY_LENGTH) {
                history.poll();
                historyLength--;
            }
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
            history.add(new HistorySample(cpuUsageByGC, (int) (MgmtUtils.getHeapFromRuntime().getUsed() / 1024 / 1024)));
            historyLength++;
        }
    }
}
