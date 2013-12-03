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
package sk.baka.webvm.analyzer.utils;

import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.SortedMap;
import java.util.logging.Logger;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;
import sk.baka.webvm.analyzer.hostos.Memory;
import static sk.baka.webvm.analyzer.utils.Constants.*;

/**
 * Provides utilities for the java.util.management package.
 * @author Martin Vysny
 */
public final class MemoryUsages {

    /**
     * Sums up all non-heap pools and return their memory usage.
     * @return memory usage, null if no pool collects non-heap space.
     * @deprecated use {@link Memory#getNonHeapSummary()}.
     */
    public static MemoryUsage getNonHeapSummary() {
        return Memory.getNonHeapSummary();
    }

    /**
     * Checks if there is a non-heap pool in the memory pool list.
     * @return true if there is pool managing non-heap memory, false otherwise.
     * @deprecated use {@link Memory#isNonHeapPool() }.
     */
    @Deprecated
    public static boolean isNonHeapPool() {
        return Memory.isNonHeapPool();
    }

    private MemoryUsages() {
        throw new AssertionError();
    }

    /**
     * Computes and returns the memory usage object, using information only from {@link Runtime}.
     * @return non-null usage object.
     * @deprecated use {@link Memory#getHeapFromRuntime()}
     */
    @Deprecated
    public static MemoryUsage getHeapFromRuntime() {
        return Memory.getHeapFromRuntime();
    }

    /**
     * Sums two memory usages together. Does not allow null values.
     * @param u1 first usage, must not be null
     * @param u2 second usage, must not be null
     * @return a summed usage, never null
     */
    public static MemoryUsage add(final MemoryUsage u1, final MemoryUsage u2) {
        return new MemoryUsage(addMem(u1.getInit(), u2.getInit()), u1.getUsed() + u2.getUsed(), u1.getCommitted() + u2.getCommitted(), addMem(u1.getMax(), u2.getMax()));
    }

    private static long addMem(final long l1, final long l2) {
        if (l1 < 0 || l2 < 0) {
            return -1;
        }
        return l1 + l2;
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
        return new MemoryUsage(mu.getInit() == -1 ? -1 : mu.getInit() / MEBIBYTES, mu.getUsed() / MEBIBYTES, mu.getCommitted() / MEBIBYTES, mu.getMax() == -1 ? -1 : mu.getMax() / MEBIBYTES);
    }

    /**
     * Formats a memory usage instance to a compact string. Uses the following format: [used (committed) / max].
     * @param mu the memory usage object.
     * @param inMegs if true then given memory usage values are already megabytes. If false then the values are in bytes.
     * @return [used (committed) / max], or [not available] if null was given
     */
    public static String toString(final MemoryUsage mu, final boolean inMegs) {
        if (mu == null) {
            return "[not available]";
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
            if (mu.getMax() > 0) {
                sb.append(mu.getUsed() * HUNDRED_PERCENT / mu.getMax());
            } else {
                sb.append('0');
            }
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
     * @return formatted percent value; "not available" if the object is null or max is -1; "none" if max is zero
     */
    public static String getUsagePerc(final MemoryUsage mu) {
        if (mu == null || mu.getMax() < 0) {
            return "not available";
        }
        if (mu.getMax() == 0) {
            return "none";
        }
        return (mu.getUsed() * HUNDRED_PERCENT / mu.getMax()) + "%";
    }

    /**
     * Returns amount of free memory in the following format: xx%
     * @param mu the memory usage object, may be null
     * @return formatted percent value; "?" if the object is null or max is -1; "none" if max is zero
     */
    public static String getFreePerc(final MemoryUsage mu) {
        if (mu == null || mu.getMax() < 0) {
            return "not available";
        }
        if (mu.getMax() == 0) {
            return "none";
        }
        return (HUNDRED_PERCENT - (mu.getUsed() * HUNDRED_PERCENT / mu.getMax())) + "%";
    }
    
    /**
     * Returns all known memory pools which are garbage-collectable and provide meaningful usage information.
     * @return map of memory pools, never null, may be empty.
     * @deprecated use {@link Memory#getMemoryPools()}
     */
    @Deprecated
    public static SortedMap<String, MemoryPoolMXBean> getMemoryPools() {
        return Memory.getMemoryPools();
    }
    
    /**
     * @deprecated use {@link Memory#getOSMemoryInfoProvider()}.
     */
    @Deprecated
    public static IMemoryInfoProvider getMemoryInfoProvider() {
        return Memory.getOSMemoryInfoProvider();
    }
}
