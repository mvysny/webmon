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

import org.jetbrains.annotations.Nullable;

/**
 * Provides utilities for the java.util.management package.
 * @author Martin Vysny
 */
public final class MemoryUsages {

    private MemoryUsages() {
        throw new AssertionError();
    }

    /**
     * Formats a memory usage instance to a compact string. Uses the following format: [used (committed) / max].
     * @param mu the memory usage object.
     * @param inMegs if true then given memory usage values are already megabytes. If false then the values are in bytes.
     * @return [used (committed) / max], or [not available] if null was given
     */
    public static String toString(final MemoryUsage2 mu, final boolean inMegs) {
        return mu == null ? "[not available]" : mu.toString(inMegs);
    }

    /**
     * Returns memory usage in the following format: xx%
     * @param mu the memory usage object, may be null
     * @return formatted percent value; "not available" if the object is null or max is -1; "none" if max is zero
     */
    public static String getUsagePerc(final MemoryUsage2 mu) {
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
    public static String getFreePerc(final MemoryUsage2 mu) {
        if (mu == null || mu.getMax() < 0) {
            return "not available";
        }
        if (mu.getMax() == 0) {
            return "none";
        }
        return (HUNDRED_PERCENT - (mu.getUsed() * HUNDRED_PERCENT / mu.getMax())) + "%";
    }
}
