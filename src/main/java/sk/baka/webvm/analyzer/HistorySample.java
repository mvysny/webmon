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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import sk.baka.webvm.misc.MgmtUtils;

/**
 * Holds history data for a single time unit.
 * @author Martin Vysny
 */
public final class HistorySample {

    private final int gcCpuUsage;
    private final long sampleTime;
    /**
     * First is the heap usage, second is the non-heap usage. Second item may be null.
     */
    private final MemoryUsage[] memUsage = new MemoryUsage[2];
    private final ThreadInfo[] threads;

    /**
     * First is the heap usage, second is the non-heap usage. May be null.
     * @return First is the heap usage, second is the non-heap usage. Second item may be null.
     */
    public MemoryUsage[] getMemPoolUsage() {
        return memUsage;
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
        memUsage[0] = MgmtUtils.getInMB(MgmtUtils.getHeapFromRuntime());
        memUsage[1] = MgmtUtils.getInMB(MgmtUtils.getNonHeapSummary());
        this.sampleTime = System.currentTimeMillis();
        threads = ManagementFactory.getThreadMXBean().getThreadInfo(ManagementFactory.getThreadMXBean().getAllThreadIds());
    }
}
