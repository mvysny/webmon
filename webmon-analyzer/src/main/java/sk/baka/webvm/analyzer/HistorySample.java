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

import sk.baka.webvm.analyzer.utils.MgmtUtils;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;

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
     * The memory usage list, indexed according to the value of the <code>POOL_*</code> constants. The values are in MB.
     */
    public final MemoryUsage[] memPoolUsage = new MemoryUsage[4];
    /**
     * A thread dump. Does not contain any stacktraces. Never null.
     */
    public final ThreadMap threads;
    /**
     * Count of classes currently loaded in the VM.
     */
    public final int classesLoaded;
    /**
     * Shows the host OS CPU usage. A value of 0..100, 0 when not supported.
     */
    public final int cpuUsage;
    /**
     * Shows the owner java process CPU usage. A value of 0..100, 0 when not supported.
     */
    public final int cpuJavaUsage;
    /**
     * Shows the host OS CPU IO usage. A value of 0..100, 0 when not supported.
     */
    public final int cpuIOUsage;

    /**
     * Creates new history sample bean.
     * @param gcCpuUsage average CPU usage of GC for this time slice in percent.
     * @param cpuOSUsage Shows the host OS CPU usage. A value of 0..100, 0 when not supported.
     * @param cpuJavaUsage Shows the owner java process CPU usage. A value of 0..100, 0 when not supported.
     * @param cpuIOUsage Shows the host OS CPU IO usage. A value of 0..100, 0 when not supported.
     */
    public HistorySample(final int gcCpuUsage, final int cpuOSUsage, final int cpuJavaUsage, final int cpuIOUsage, final IMemoryInfoProvider meminfo) {
        this.gcCpuUsage = gcCpuUsage;
        memPoolUsage[POOL_HEAP] = MgmtUtils.getInMB(MgmtUtils.getHeapFromRuntime());
        memPoolUsage[POOL_NON_HEAP] = MgmtUtils.getInMB(MgmtUtils.getNonHeapSummary());
        this.sampleTime = System.currentTimeMillis();
        threads = ThreadMap.takeSnapshot();
        classesLoaded = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
        memPoolUsage[POOL_PHYS_MEM] = MgmtUtils.getInMB(meminfo.getPhysicalMemory());
        memPoolUsage[POOL_SWAP] = MgmtUtils.getInMB(meminfo.getSwap());
        cpuUsage = cpuOSUsage;
        this.cpuJavaUsage = cpuJavaUsage;
        this.cpuIOUsage = cpuIOUsage;
    }
}
