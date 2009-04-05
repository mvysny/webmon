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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 * Delivers information from a host operating system.
 * @author Martin Vysny
 */
public final class HostOS {

    private final static Logger log = Logger.getLogger(HostOS.class.getName());

    /**
     * Returns physical memory information for host OS.
     * @return memory usage or null if the information is unavailable. The committed memory size includes buffers and/or disk cache. If committed memory size is equal to the used memory size then the buffer/cache information is not available.
     */
    public static MemoryUsage getPhysicalMemory() {
        MemoryUsage result = MemoryLinuxStrategy.getPhysicalMemory();
        if (result != null) {
            return result;
        }
        return MemoryJMXStrategy.getPhysicalMemory();
    }

    /**
     * Returns swap memory information for host OS.
     * @return memory usage or null if the information is unavailable.
     */
    public static MemoryUsage getSwap() {
        MemoryUsage result = MemoryLinuxStrategy.getSwap();
        if (result != null) {
            return result;
        }
        return MemoryJMXStrategy.getSwap();
    }

    /**
     * Retrieves host OS memory info using /proc/meminfo (Linux only)
     */
    private static class MemoryLinuxStrategy {

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
                        value = value.substring(0, value.length() - 3);
                    }
                    try {
                        final long val = Long.parseLong(value);
                        result.put(key, val);
                    } catch (NumberFormatException ex) {
                        continue;
                    }
                }
            } finally {
                IOUtils.closeQuietly(in);
            }
            return result;
        }


        static {
            boolean avail = false;
            try {
                parseMeminfo(MEMINFO);
                avail = true;
            } catch (Exception ex) {
                log.log(Level.INFO, "MemoryLinuxStrategy disabled: " + MEMINFO + " not available", ex);
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

        /**
         * Returns physical memory information for host OS.
         * @return memory usage or null if the information is unavailable.
         */
        public static MemoryUsage getPhysicalMemory() {
            if (!available()) {
                return null;
            }
            final Map<String, Long> memInfo;
            try {
                memInfo = parseMeminfo(MEMINFO);
            } catch (Exception t) {
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
            return new MemoryUsage(-1, used * 1024, committed * 1024, total * 1024);
        }

        /**
         * Returns physical memory information for host OS.
         * @return memory usage or null if the information is unavailable.
         */
        public static MemoryUsage getSwap() {
            if (!available()) {
                return null;
            }
            final Map<String, Long> memInfo;
            try {
                memInfo = parseMeminfo(MEMINFO);
            } catch (Exception t) {
                return null;
            }
            final Long total = memInfo.get("SwapTotal");
            final Long free = memInfo.get("SwapFree");
            if (total == null || free == null) {
                return null;
            }
            final long used = total - free;
            return new MemoryUsage(-1, used * 1024, used * 1024, total * 1024);
        }

        private MemoryLinuxStrategy() {
            throw new AssertionError();
        }
    }

    /**
     * Retrieves host OS memory info using com.sun.management.OperatingSystemMXBean
     */
    private static class MemoryJMXStrategy {

        private static final OperatingSystemMXBean BEAN;


        static {
            OperatingSystemMXBean b = null;
            try {
                final Class<?> comSunBean = Class.forName("com.sun.management.OperatingSystemMXBean");
                b = OperatingSystemMXBean.class.cast(comSunBean.cast(ManagementFactory.getOperatingSystemMXBean()));
            } catch (Throwable ex) {
                log.log(Level.INFO, "MemoryJMXStrategy disabled: com.sun.management.OperatingSystemMXBean unavailable", ex);
            }
            BEAN = b;
        }

        /**
         * Checks that this strategy is available to use.
         * @return true if available, false otherwise.
         */
        public static boolean available() {
            return BEAN != null;
        }

        /**
         * Returns physical memory information for host OS.
         * @return memory usage or null if the information is unavailable.
         */
        public static MemoryUsage getPhysicalMemory() {
            if (!available()) {
                return null;
            }
            try {
                final Class<?> comSunBean = Class.forName("com.sun.management.OperatingSystemMXBean");
                final long total = (Long) comSunBean.getMethod("getTotalPhysicalMemorySize").invoke(BEAN);
                final long free = (Long) comSunBean.getMethod("getFreePhysicalMemorySize").invoke(BEAN);
                final long used = total - free;
                return new MemoryUsage(-1, used, used, total);
            } catch (Throwable t) {
                return null;
            }
        }

        /**
         * Returns physical memory information for host OS.
         * @return memory usage or null if the information is unavailable.
         */
        public static MemoryUsage getSwap() {
            if (!available()) {
                return null;
            }
            try {
                final Class<?> comSunBean = Class.forName("com.sun.management.OperatingSystemMXBean");
                final long total = (Long) comSunBean.getMethod("getTotalSwapSpaceSize").invoke(BEAN);
                final long free = (Long) comSunBean.getMethod("getFreeSwapSpaceSize").invoke(BEAN);
                final long used = total - free;
                return new MemoryUsage(-1, used, used, total);
            } catch (Throwable t) {
                return null;
            }
        }

        private MemoryJMXStrategy() {
            throw new AssertionError();
        }
    }

    private HostOS() {
        throw new AssertionError();
    }
}
