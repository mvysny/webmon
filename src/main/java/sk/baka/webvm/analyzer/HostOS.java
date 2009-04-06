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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
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
    private final ICpuUsage cpuUsage;


    {
        ICpuUsage imp1 = new CpuUsageLinux();
        if (imp1.supported()) {
            cpuUsage = imp1;
        } else {
            cpuUsage = null;
        }
    }

    /**
     * Checks if the CPU usage measurement is supported on this host OS
     * @return true if supported, false otherwise.
     */
    public static boolean isCpuUsageSupported() {
        return new CpuUsageLinux().supported();
    }

    /**
     * Returns an average CPU usage in a time slice starting at the previous call of this method.
     * @return average overall CPU usage or -1 if CPU sampling is unsupported or error occurred.
     */
    public int getCpuUsage() {
        if (cpuUsage == null) {
            return -1;
        }
        if (cpuMeasurement == null) {
            try {
                cpuMeasurement = cpuUsage.measure();
            } catch (Exception ex) {
                log.log(Level.SEVERE, null, ex);
                return -1;
            }
            return 0;
        }
        final Object newMeasurement;
        try {
            newMeasurement = cpuUsage.measure();
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
            return -1;
        }
        final int result = cpuUsage.getAvgCpuUsage(cpuMeasurement, newMeasurement);
        cpuMeasurement = newMeasurement;
        return result;
    }
    private Object cpuMeasurement = null;

    /**
     * Interface for measuring CPU usage.
     */
    private static interface ICpuUsage {

        /**
         * Checks if this particular implementation is supported on given host OS.
         * @return true if it is supported, false otherwise.
         */
        boolean supported();

        /**
         * Measures an implementation-dependent CPU usage statistics. Used in {@link #getAvgCpuUsage(java.lang.Object, java.lang.Object)} to compute the real CPU usage.
         * @return the measurement object
         * @throws Exception if something happens.
         */
        Object measure() throws Exception;

        /**
         * Computes an average CPU usage between two measurements. The first measurement was taken before the second one was taken.
         * @param m1 first measurement.
         * @param m2 second measurement
         * @return CPU usage in percent, must be a value between 0 and 100.
         */
        int getAvgCpuUsage(final Object m1, final Object m2);
    }

    private static class CpuUsageLinux implements ICpuUsage {

        private final File procStat = new File("/proc/stat");

        public boolean supported() {
            return procStat.exists();
        }

        public Object measure() throws Exception {
            // the object is really an array of longs: [user, nice, system, idle].
            // To compute the CPU usage, we have to perform:
            // (idle2-idle1)*100/(user2+nice2+system2+idle2-user1-nice1-system1-idle1)
            final BufferedReader in = new BufferedReader(new FileReader(procStat));
            try {
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    if (line.startsWith("cpu ")) {
                        final StringTokenizer t = new StringTokenizer(line);
                        t.nextToken();
                        final long user = Long.parseLong(t.nextToken());
                        final long nice = Long.parseLong(t.nextToken());
                        final long system = Long.parseLong(t.nextToken());
                        final long idle = Long.parseLong(t.nextToken());
                        return new long[]{user, nice, system, idle};
                    }
                }
            } finally {
                IOUtils.closeQuietly(in);
            }
            throw new RuntimeException("No cpu line");
        }

        public int getAvgCpuUsage(Object m1, Object m2) {
            final long[] me1 = (long[]) m1;
            final long[] me2 = (long[]) m2;
            final long cpuUsage = (me2[3] - me1[3]) * 100 / (me2[0] + me2[1] + me2[2] + me2[3] - me1[0] - me1[1] - me1[2] - me1[3]);
            return 100 - ((int) cpuUsage);
        }
    }
}
