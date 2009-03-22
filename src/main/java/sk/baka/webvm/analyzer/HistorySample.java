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

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import sk.baka.webvm.misc.MgmtUtils;

/**
 * Holds history data for a single time unit.
 * @author Martin Vysny
 */
public final class HistorySample implements Serializable {

    private final int heapUsage;
    private final int heapCommitted;
    private final int gcCpuUsage;
    private final long sampleTime;
    private final int[] memPoolUsage;
    private final ThreadInfo[] threads;

    /**
     * Usages in megabytes of memory pools, ordered as returned by the {@link MgmtUtils#getMemoryPools()}.
     * @return
     */
    public int[] getMemPoolUsage() {
        return memPoolUsage;
    }

    /**
     * The time this sample was taken.
     * @return the time.
     */
    public long getSampleTime() {
        return sampleTime;
    }

    /**
     * Returns GC CPU Usage.
     * @return average CPU usage of GC for this time slice in percent, 0-100.
     */
    public int getGcCpuUsage() {
        return gcCpuUsage;
    }

    /**
     * Returns heap usage.
     * @return heap being used at the beginning of this time slice, in MB.
     */
    public int getHeapUsage() {
        return heapUsage;
    }

    /**
     * Returns heap committed.
     * @return heap committed at the beginning of this time slice, in MB.
     */
    public int getHeapCommitted() {
        return heapCommitted;
    }

    /**
     * Returns a thread dump. Does not contain any stacktraces.
     * @return a list of thread information objects.
     */
    public ThreadInfo[] getThreads() {
        return threads;
    }

    /**
     * Creates new history sample bean.
     * @param gcCpuUsage average CPU usage of GC for this time slice in percent.
     * @param memUsage memory being used at the beginning of this time slice in MB.
     */
    public HistorySample(final int gcCpuUsage) {
        this.gcCpuUsage = gcCpuUsage;
        final MemoryUsage heap = MgmtUtils.getInMB(MgmtUtils.getHeapFromRuntime());
        this.heapUsage = (int) heap.getUsed();
        this.heapCommitted = (int) heap.getCommitted();
        this.sampleTime = System.currentTimeMillis();
        memPoolUsage = new int[MgmtUtils.getMemoryPools().size()];
        int i = 0;
        for (final MemoryPoolMXBean bean : MgmtUtils.getMemoryPools().values()) {
            final MemoryUsage usage = bean.getUsage();
            final long used = usage.getUsed() / 1024 / 1024;
            memPoolUsage[i++] = (int) used;
        }
        threads = ManagementFactory.getThreadMXBean().getThreadInfo(ManagementFactory.getThreadMXBean().getAllThreadIds());
    }
}
