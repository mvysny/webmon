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

import java.lang.management.MemoryUsage;

/**
 * Provides utilities for the java.util.management package.
 * @author Martin Vysny
 */
public final class MgmtUtils {

    private MgmtUtils() {
        throw new AssertionError();
    }

    public static MemoryUsage getHeapFromRuntime() {
        long maxMem = Runtime.getRuntime().maxMemory();
        long heapSize = Runtime.getRuntime().totalMemory();
        long heapUsed = heapSize - Runtime.getRuntime().freeMemory();
        return new MemoryUsage(-1, heapUsed, heapSize, maxMem == Long.MAX_VALUE ? -1 : maxMem);
    }

    /**
     * Returns a new object with all values divided by 1024*1024 (converted from bytes to mebibytes).
     * @param mu the memory usage to convert
     * @return new memory object with values in mebibytes
     */
    public static MemoryUsage getInMB(final MemoryUsage mu) {
        return new MemoryUsage(mu.getInit() == -1 ? -1 : mu.getInit() / 1024 / 1024, mu.getUsed() / 1024 / 1024, mu.getCommitted() / 1024 / 1024, mu.getMax() == -1 ? -1 : mu.getMax() / 1024 / 1024);
    }

    /**
     * Formats a memory usage instance to a compact string. Uses the following format: [used (committed) / max].
     * @param mu the memory usage object.
     * @return [used (committed) / max], or [unknown] if null was given
     */
    public static String toString(final MemoryUsage mu) {
        if (mu == null) {
            return "[unknown]";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(mu.getUsed() / 1024 / 1024);
        sb.append("M (");
        sb.append(mu.getCommitted() / 1024 / 1024);
        sb.append("M)");
        if (mu.getMax() >= 0) {
            sb.append(" / ");
            sb.append(mu.getMax() / 1024 / 1024);
            sb.append("M - ");
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
}
