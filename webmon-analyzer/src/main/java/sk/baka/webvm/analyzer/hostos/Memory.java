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
package sk.baka.webvm.analyzer.hostos;

import sk.baka.webvm.analyzer.hostos.linux.LinuxProcessMemoryProvider;
import sk.baka.webvm.analyzer.hostos.linux.MemoryLinuxStrategy;
import sk.baka.webvm.analyzer.hostos.windows.MemoryWindowsStrategy;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static sk.baka.webvm.analyzer.utils.MemoryUsages.add;
import static sk.baka.webvm.analyzer.utils.MemoryUsages.getNonHeapSummary;
import sk.baka.webvm.analyzer.hostos.windows.WMIUtils;

/**
 * Provides memory information for a process.
 * @author Martin Vysny
 */
public class Memory {
    /**
     * Provides the Working Set value.
     */
    private static class WindowsProcessMemoryProvider implements IMemoryInfoProvider {
        private final int pid;

        public WindowsProcessMemoryProvider(int pid) {
            this.pid = pid;
        }
        
        public static boolean isAvailable() {
            return WMIUtils.isAvailable();
        }

        public MemoryUsage getSwap() {
            return null;
        }

        public MemoryUsage getPhysicalMemory() {
            return WMIUtils.getWorkingSetSize(pid);
        }
    }
    
    /**
     * Returns a provider which provides memory information about given process.
     * @param pid the process ID.
     * @return the provider, never null.
     */
    public static IMemoryInfoProvider newProcessMemoryInfo(int pid) {
        if (WindowsProcessMemoryProvider.isAvailable()) {
            return new WindowsProcessMemoryProvider(pid);
        } else if (LinuxProcessMemoryProvider.isAvailable()) {
            return new LinuxProcessMemoryProvider(pid);
        }
        return new DummyMemoryStrategy();
    }

    /**
     * Returns the OS memory information provider.
     * @return OS memory info provider, never null.
     */
    public static IMemoryInfoProvider getOSMemoryInfoProvider() {
        if (MemoryLinuxStrategy.available()) {
            return new MemoryLinuxStrategy();
        }
        if (MemoryWindowsStrategy.isAvailable()) {
            return new MemoryWindowsStrategy();
        }
        if (MemoryJMXStrategy.available()) {
            return new MemoryJMXStrategy();
        }
        return new DummyMemoryStrategy();
    }

    /**
     * Sums up all non-heap pools and return their memory usage.
     * @return memory usage, null if no pool collects non-heap space.
     */
    public static MemoryUsage getNonHeapSummary() {
        MemoryUsage result = null;
        final List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
        for (final MemoryPoolMXBean bean : beans) {
            if (bean.getType() != MemoryType.NON_HEAP) {
                continue;
            }
            if (result == null) {
                result = bean.getUsage();
            } else {
                result = add(result, bean.getUsage());
            }
        }
        return result;
    }
    private static final boolean IS_NON_HEAP;

    private static final Logger log = Logger.getLogger(Memory.class.getName());
    static {
        boolean isNonHeap = false;
        try {
            isNonHeap = getNonHeapSummary() != null;
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Failed to get non-heap pools: " + ex, ex);
        }
        IS_NON_HEAP = isNonHeap;
    }

    /**
     * Checks if there is a non-heap pool in the memory pool list.
     * @return true if there is pool managing non-heap memory, false otherwise.
     */
    public static boolean isNonHeapPool() {
        return IS_NON_HEAP;
    }

    /**
     * Computes and returns the memory usage object, using information only from {@link Runtime}.
     * @return non-null usage object.
     */
    public static MemoryUsage getHeapFromRuntime() {
        long maxMem = Runtime.getRuntime().maxMemory();
        long heapSize = Runtime.getRuntime().totalMemory();
        long heapUsed = heapSize - Runtime.getRuntime().freeMemory();
        return new MemoryUsage(-1, heapUsed, heapSize, maxMem == Long.MAX_VALUE ? -1 : maxMem);
    }

    private static final SortedMap<String, MemoryPoolMXBean> MEMORY_POOLS;

    static {
        final SortedMap<String, MemoryPoolMXBean> pools = new TreeMap<String, MemoryPoolMXBean>();
        try {
            final List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
            if (beans != null && !beans.isEmpty()) {
                for (final MemoryPoolMXBean bean : beans) {
                    final MemoryUsage usage = bean.getUsage();
                    if (usage == null || !bean.isUsageThresholdSupported()) {
                        continue;
                    }
                    pools.put(bean.getName(), bean);
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Failed to get MemoryPools: " + ex, ex);
        }
        MEMORY_POOLS = Collections.unmodifiableSortedMap(pools);
    }

    /**
     * Returns all known memory pools which are garbage-collectable and provide meaningful usage information.
     * @return map of memory pools, never null, may be empty.
     */
    public static SortedMap<String, MemoryPoolMXBean> getMemoryPools() {
        return MEMORY_POOLS;
    }
}
