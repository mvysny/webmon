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
package sk.baka.webvm.misc;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Provides utilities for the java.util.management package.
 * @author Martin Vysny
 */
public final class MgmtUtils {

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


    static {
        IS_NON_HEAP = getNonHeapSummary() != null;
    }

    /**
     * Checks if there is a non-heap pool.
     * @return true if there is, false otherwise.
     */
    public static boolean isNonHeapPool() {
        return IS_NON_HEAP;
    }

    private MgmtUtils() {
        throw new AssertionError();
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

    public static MemoryUsage add(final MemoryUsage u1, final MemoryUsage u2) {
        return new MemoryUsage(u1.getInit() + u2.getInit(), u1.getUsed() + u2.getUsed(), u1.getCommitted() + u2.getCommitted(), u1.getMax() + u2.getMax());
    }

    /**
     * Returns a new object with all values divided by 1024*1024 (converted from bytes to mebibytes).
     * @param mu the memory usage to convert
     * @return new memory object with values in mebibytes. Returns null if null was supplied.
     */
    public static MemoryUsage getInMB(final MemoryUsage mu) {
        if (mu == null) {
            return null;
        }
        return new MemoryUsage(mu.getInit() == -1 ? -1 : mu.getInit() / 1024 / 1024, mu.getUsed() / 1024 / 1024, mu.getCommitted() / 1024 / 1024, mu.getMax() == -1 ? -1 : mu.getMax() / 1024 / 1024);
    }

    /**
     * Formats a memory usage instance to a compact string. Uses the following format: [used (committed) / max].
     * @param mu the memory usage object.
     * @return [used (committed) / max], or [unknown] if null was given
     */
    public static String toString(final MemoryUsage mu, final boolean inMegs) {
        if (mu == null) {
            return "[unknown]";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(mu.getUsed());
        if (inMegs) {
            sb.append("M");
        }
        sb.append(" (");
        sb.append(mu.getCommitted());
        if (inMegs) {
            sb.append("M");
        }
        sb.append(")");
        if (mu.getMax() >= 0) {
            sb.append(" / ");
            sb.append(mu.getMax());
            if (inMegs) {
                sb.append("M");
            }
            sb.append(" - ");
            sb.append(mu.getUsed() * 100 / mu.getMax());
            sb.append('%');
        } else {
            sb.append(" / ?");
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Returns memory usage in the following format: xx%
     * @param mu the memory usage object, may be null
     * @return formatted percent value or "?" if the object is null or max is -1
     */
    public static String getUsagePerc(final MemoryUsage mu) {
        if (mu == null || mu.getMax() < 0) {
            return "?";
        }
        return (mu.getUsed() * 100 / mu.getMax()) + "%";
    }

    /**
     * Returns memory usage in the following format: xx%
     * @param mu the memory usage object, may be null
     * @return formatted percent value or "?" if the object is null or max is -1
     */
    public static String getFreePerc(final MemoryUsage mu) {
        if (mu == null || mu.getMax() < 0) {
            return "?";
        }
        return (100 - (mu.getUsed() * 100 / mu.getMax())) + "%";
    }
    private static final SortedMap<String, MemoryPoolMXBean> MEMORY_POOLS;


    static {
        final SortedMap<String, MemoryPoolMXBean> pools = new TreeMap<String, MemoryPoolMXBean>();
        final List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
        if (beans != null && !beans.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            for (final MemoryPoolMXBean bean : beans) {
                final MemoryUsage usage = bean.getUsage();
                if (usage == null || !bean.isUsageThresholdSupported()) {
                    continue;
                }
                pools.put(bean.getName(), bean);
            }
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
