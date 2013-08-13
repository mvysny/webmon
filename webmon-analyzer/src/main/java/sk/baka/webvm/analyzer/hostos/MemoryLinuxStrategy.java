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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.baka.webvm.analyzer.utils.Constants;
import sk.baka.webvm.analyzer.utils.MiscUtils;

/**
 * Retrieves host OS memory info using /proc/meminfo (Linux only)
 */
public final class MemoryLinuxStrategy implements IMemoryInfoProvider {

    private static final boolean AVAIL;
    private static final String MEMINFO = "/proc/meminfo";

    private static Map<String, Long> parseMeminfo(final String memInfo) throws IOException {
        final Map<String, Long> result = new HashMap<String, Long>();
        final BufferedReader in = new BufferedReader(new FileReader(memInfo));
        try {
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                int colon = line.indexOf(':');
                if (colon < 0) {
                    continue;
                }
                final String key = line.substring(0, colon);
                String value = line.substring(colon + 1, line.length()).trim();
                if (value.endsWith(" kB")) {
                    value = value.substring(0, value.length() - " kB".length());
                }
                try {
                    final long val = Long.parseLong(value);
                    result.put(key, val);
                } catch (NumberFormatException ex) {
                    continue;
                }
            }
        } finally {
            MiscUtils.closeQuietly(in);
        }
        return result;
    }
    private static final Logger LOG = Logger.getLogger(MemoryLinuxStrategy.class.getName());

    static {
        boolean avail = false;
        if (OS.isLinuxBased()) {
            try {
                parseMeminfo(MEMINFO);
                avail = true;
            } catch (Throwable ex) {
                LOG.log(Level.INFO, "MemoryLinuxStrategy disabled: " + MEMINFO + " not available", ex);
            }
        }
        AVAIL = avail;
    }

    /**
     * Checks that this strategy is available to use.
     * @return true if available, false otherwise.
     */
    public static boolean available() {
        return AVAIL;
    }

    public MemoryUsage getPhysicalMemory() {
        if (!available()) {
            return null;
        }
        final Map<String, Long> memInfo;
        try {
            memInfo = parseMeminfo(MEMINFO);
        } catch (Exception t) {
            LOG.log(Level.INFO, "Failed to obtain Linux memory statistics", t);
            return null;
        }
        final Long total = memInfo.get("MemTotal");
        final Long free = memInfo.get("MemFree");
        final Long buffers = memInfo.get("Buffers");
        final Long cache = memInfo.get("Cached");
        if (total == null || free == null || buffers == null || cache == null) {
            return null;
        }
        final long committed = total - free;
        final long used = committed - buffers - cache;
        return new MemoryUsage(-1, used * Constants.KIBIBYTES, committed * Constants.KIBIBYTES, total * Constants.KIBIBYTES);
    }

    public MemoryUsage getSwap() {
        if (!available()) {
            return null;
        }
        final Map<String, Long> memInfo;
        try {
            memInfo = parseMeminfo(MEMINFO);
        } catch (Exception t) {
            LOG.log(Level.INFO, "Failed to obtain Linux memory statistics", t);
            throw new RuntimeException(t);
        }
        final Long total = memInfo.get("SwapTotal");
        final Long free = memInfo.get("SwapFree");
        if (total == null || free == null) {
            return null;
        }
        final long used = total - free;
        return new MemoryUsage(-1, used * Constants.KIBIBYTES, used * Constants.KIBIBYTES, total * Constants.KIBIBYTES);
    }
}
