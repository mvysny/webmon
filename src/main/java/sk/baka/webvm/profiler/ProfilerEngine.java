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
package sk.baka.webvm.profiler;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The profiler implementation.
 * @author Martin Vysny
 */
public final class ProfilerEngine {

    private static final ProfilerEngine INSTANCE = new ProfilerEngine();

    /**
     * Returns the singleton profiler instance.
     * @return the instance.
     */
    public static ProfilerEngine getInstance() {
        return INSTANCE;
    }
    private ScheduledExecutorService threadDumper = null;
    private ScheduledFuture<?> dumperFuture = null;

    /**
     * Starts the profiler. Does nothing if the profiler is already running. The profiling data is erased upon startup.
     * @param delay the delay between thread dumps in millis.
     */
    public synchronized void start(final long delay) {
        if (threadDumper != null) {
            return;
        }
        threadDumper = Executors.newSingleThreadScheduledExecutor();
        dumper = new ProfilerRunnable();
        dumperFuture = threadDumper.scheduleAtFixedRate(dumper, 0, delay, TimeUnit.MILLISECONDS);
    }
    private ProfilerRunnable dumper = null;

    /**
     * Checks if the profiler is active.
     * @return true if the profiler is active, false if not.
     */
    public synchronized boolean isRunning() {
        return threadDumper != null;
    }

    /**
     * Stops the profiler. Does nothing if the profiler is stopped.
     */
    public synchronized void stop() {
        if (threadDumper == null) {
            return;
        }
        dumperFuture.cancel(true);
        threadDumper.shutdownNow();
        threadDumper = null;
        try {
            // This will establish happens-before relation: ProfilerRunnable#run() happens-before #stop().
            // The profiling data is thus correctly updated and visible to any following invocation of the #getData() method.
            dumperFuture.get();
        } catch (Exception ex) {
            // the task was most probably cancelled.
        }
    }

    /**
     * Returns a live immutable profiling data. The data are guaranteed to be up-to-date only after {@link #stop()} has been invoked.
     * @return map which maps class name and a method name to the invocation statistics.
     */
    public synchronized Map<String, Map<String, MethodInvocationStats>> getData() {
        if (dumper == null) {
            return null;
        }
        return Collections.unmodifiableMap(dumper.data);
    }

    /**
     * On each run the thread stack traces are polled and results are added to the map. The object does not perform any synchronization thus it must be run in the same thread only.
     * @author Martin Vysny
     */
    private static class ProfilerRunnable implements Runnable {

        private final int expectedNumberOfClasses = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
        /**
         * Maps a class name and a method name to the invocation statistics.
         */
        public final Map<String, Map<String, MethodInvocationStats>> data = new HashMap<String, Map<String, MethodInvocationStats>>(expectedNumberOfClasses);
        private final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        /**
         * Last sample was taken at this time. If zero then no samples were taken yet.
         */
        private long lastSampleTime = 0;

        public void run() {
            final ThreadInfo[] info = bean.getThreadInfo(bean.getAllThreadIds(), Integer.MAX_VALUE);
            final long sampleDelay = System.currentTimeMillis() - lastSampleTime;
            lastSampleTime += sampleDelay;
            analyzeThreads(info, sampleDelay);
        }

        private void analyzeThreads(final ThreadInfo[] infos, final long sampleDelay) {
            for (final ThreadInfo info : infos) {
                final StackTraceElement[] stackTrace = info.getStackTrace();
                if (stackTrace == null) {
                    continue;
                }
                for (final StackTraceElement e : stackTrace) {
                    // retrieve the MethodInvocationStats object
                    final String className = e.getClassName();
                    if (className == null || className.length() == 0) {
                        continue;
                    }
                    final String methodName = e.getMethodName();
                    if (methodName == null || methodName.length() == 0) {
                        continue;
                    }
                    Map<String, MethodInvocationStats> stats = data.get(className);
                    if (stats == null) {
                        stats = new HashMap<String, MethodInvocationStats>();
                        data.put(className, stats);
                    }
                    MethodInvocationStats stat = stats.get(methodName);
                    if (stat == null) {
                        stat = new MethodInvocationStats();
                        stats.put(methodName, stat);
                    }
                    // update the Statistics object
                    stat.activeTime += sampleDelay;
                }
            }
        }
    }
}
