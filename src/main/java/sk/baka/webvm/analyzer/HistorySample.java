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
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import sk.baka.webvm.misc.MgmtUtils;

/**
 * Holds history data for a single time unit.
 * @author Martin Vysny
 */
public final class HistorySample {

    /**
     * Returns GC CPU Usage.
     * @return average CPU usage of GC for this time slice in percent, 0-100.
     */
    public final int gcCpuUsage;
    /**
     * The time this sample was taken.
     */
    public final long sampleTime;
    /**
     * The {@link MgmtUtils#getHeapFromRuntime() heap usage}.
     */
    public static final int POOL_HEAP = 0;
    /**
     * The {@link MgmtUtils#getNonHeapSummary() non-heap usage}, may be null.
     */
    public static final int POOL_NON_HEAP = 1;
    /**
     * The {@link HostOS.getPhysicalMemory() host OS physical memory}, may be null.
     */
    public static final int POOL_PHYS_MEM = 2;
    /**
     * The {@link HostOS.getSwap() swap}, may be null.
     */
    public static final int POOL_SWAP = 3;
    /**
     * The memory usage list, indexed according to the value of the <code>POOL_*</code> constants.
     */
    public final MemoryUsage[] memPoolUsage = new MemoryUsage[4];
    /**
     * A thread dump. Does not contain any stacktraces. Never null.
     */
    public final ThreadInfo[] threads;
    /**
     * Count of classes currently loaded in the VM.
     */
    public final int classesLoaded;
    /**
     * Current count of daemon threads.
     */
    public final int daemonThreadCount;
    /**
     * Returns current count of all threads.
     * @return current count of all threads.
     */
    public final int threadCount;

    /**
     * Creates new history sample bean.
     * @param gcCpuUsage average CPU usage of GC for this time slice in percent.
     */
    public HistorySample(final int gcCpuUsage) {
        this.gcCpuUsage = gcCpuUsage;
        memPoolUsage[POOL_HEAP] = MgmtUtils.getInMB(MgmtUtils.getHeapFromRuntime());
        memPoolUsage[POOL_NON_HEAP] = MgmtUtils.getInMB(MgmtUtils.getNonHeapSummary());
        this.sampleTime = System.currentTimeMillis();
        final ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
        threads = tbean.getThreadInfo(tbean.getAllThreadIds());
        threadCount = tbean.getThreadCount();
        daemonThreadCount = tbean.getDaemonThreadCount();
        classesLoaded = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
        memPoolUsage[POOL_PHYS_MEM] = MgmtUtils.getInMB(HostOS.getPhysicalMemory());
        memPoolUsage[POOL_SWAP] = MgmtUtils.getInMB(HostOS.getSwap());
    }
}
