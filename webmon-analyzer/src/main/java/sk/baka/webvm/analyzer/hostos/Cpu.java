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

import java.lang.management.OperatingSystemMXBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.baka.webvm.analyzer.CpuUsage;
import sk.baka.webvm.analyzer.utils.Constants;
import sk.baka.webvm.analyzer.utils.MiscUtils;
import sk.baka.webvm.analyzer.utils.WMIUtils;

/**
 * Provides a CPU measurement support.
 * @author Martin Vysny
 */
public final class Cpu {

    /**
     * Creates a new measurer for Host OS CPU usage.
     * @return the CPU measurer, never null.
     */
    public static CpuUsage newHostCpu() {
        final ICpuUsageMeasure cpuusage;
        switch (OS.get()) {
            case Linux:
            case Android: cpuusage = new CpuUsageLinuxStrategy(); break;
            case Windows: cpuusage = new CpuUsageWindowsStrategy(); break;
            default: cpuusage = new DummyCpuUsageStrategy(); break;
        }
        return new CpuUsage(cpuusage);
    }
    private static final CpuUsage HOST_CPU = newHostCpu();

    /**
     * Checks if Host OS CPU usage measurement is supported.
     * @return true if supported.
     */
    public static boolean isHostCpuSupported() {
        return !(HOST_CPU.cpuUsage instanceof DummyCpuUsageStrategy);
    }

    private static class DummyCpuUsageStrategy implements ICpuUsageMeasure {

        public Object measure() throws Exception {
            return null;
        }

        public int getAvgCpuUsage(Object m1, Object m2) {
            return 0;
        }
    }
    
    /**
     * Creates a new measurer for Host OS CPU IO usage (% of time spent waiting for IO).
     * @return the CPU measurer, never null.
     */
    public static CpuUsage newHostIOCpu() {
        final ICpuUsageMeasure io;
        switch (OS.get()) {
            case Linux:
            case Android: io = new IOCpuUsageLinuxStrategy(); break;
            default: io = new DummyCpuUsageStrategy();
        }
        return new CpuUsage(io);
    }
    private static final CpuUsage HOST_IO_CPU = newHostIOCpu();

    /**
     * Checks if measurer for Host OS CPU IO usage (% of time spent waiting for IO) is supported.
     * @return true if supported.
     */
    public static boolean isHostIOCpuSupported() {
        return !(HOST_IO_CPU.cpuUsage instanceof DummyCpuUsageStrategy);
    }

    /**
     * Creates a new measurer for CPU used by the owner java process.
     * @return the CPU measurer, never null.
     */
    public static CpuUsage newJavaCpu() {
        return new CpuUsage(JavaCpuUsageStrategy.supported() ? new JavaCpuUsageStrategy() : new DummyCpuUsageStrategy());
    }

    /**
     * Checks if measurer for CPU used by the owner java process is supported.
     * @return true if supported.
     */
    public static boolean isJavaCpuSupported() {
        return JavaCpuUsageStrategy.supported();
    }

    /**
     * Returns a Host OS CPU usage information.
     */
    private static class CpuUsageLinuxStrategy implements ICpuUsageMeasure {

        private static final File PROC_STAT = new File("/proc/stat");

        public Object measure() throws Exception {
            // the object is really an array of longs: [user, nice, system, idle].
            // To compute the CPU usage, we have to perform:
            // (idle2-idle1)*HUNDRED_PERCENT/(user2+nice2+system2+idle2-user1-nice1-system1-idle1)
            final BufferedReader in = new BufferedReader(new FileReader(PROC_STAT));
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
                MiscUtils.closeQuietly(in);
            }
            throw new IllegalStateException("No cpu line");
        }
        private static final int MEASURE_USER = 0;
        private static final int MEASURE_NICE = 1;
        private static final int MEASURE_SYSTEM = 2;
        private static final int MEASURE_IDLE = 3;

        public int getAvgCpuUsage(Object m1, Object m2) {
            final long[] me1 = (long[]) m1;
            final long[] me2 = (long[]) m2;
            final long sampleTimeDelta = me2[MEASURE_USER] + me2[MEASURE_NICE] + me2[MEASURE_SYSTEM] + me2[MEASURE_IDLE] -
                    me1[MEASURE_USER] - me1[MEASURE_NICE] - me1[MEASURE_SYSTEM] - me1[MEASURE_IDLE];
            if (sampleTimeDelta <= 0) {
                return 0;
            }
            final long cpuIdle = (me2[MEASURE_IDLE] - me1[MEASURE_IDLE]) * Constants.HUNDRED_PERCENT / sampleTimeDelta;
            return Constants.HUNDRED_PERCENT - ((int) cpuIdle);
        }
    }

    private static class CpuUsageWindowsStrategy implements ICpuUsageMeasure {

        public Object measure() throws Exception {
            return WMIUtils.getCPUUsage();
        }

        public int getAvgCpuUsage(Object m1, Object m2) {
            return (Integer) m2;
        }
    }
    
    /**
     * Returns a Host OS CPU time waiting for IO.
     */
    private static class IOCpuUsageLinuxStrategy implements ICpuUsageMeasure {

        private final static File DISKSTATS = new File("/proc/diskstats");

        public Object measure() throws Exception {
            // the object is really an array of longs: [weightedMillisSpentIO, currentTimeMillis].
            // To compute the CPU usage, we have to perform:
            // (weightedMillisSpentIO2-weightedMillisSpentIO1)*100/(currentTimeMillis2-currentTimeMillis1)
            long weightedMillisSpentIOTotal = 0;
            final BufferedReader in = new BufferedReader(new FileReader(DISKSTATS));
            final long currentTimeMillis = System.currentTimeMillis();
            try {
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    final StringTokenizer t = new StringTokenizer(line);
                    final List<Object> tokens = Collections.list(t);
                    final String devname = (String) tokens.get(DISKSTATS_DEVNAME);
                    if (Character.isDigit(devname.charAt(devname.length() - 1))) {
                        // ignore sda2 etc - we are interested in sda only
                        continue;
                    }
                    final long weightedMillisSpentIO = Long.parseLong((String) tokens.get(DISKSTATS_MILLIS_SPENT_IO));
                    weightedMillisSpentIOTotal += weightedMillisSpentIO;
                }
            } finally {
                MiscUtils.closeQuietly(in);
            }
            return new long[]{weightedMillisSpentIOTotal, currentTimeMillis};
        }
        private static final int MEASURE_MILLIS_SPENT_IO = 0;
        private static final int MEASURE_CURRENT_TIME_MILLIS = 1;
        private static final int DISKSTATS_DEVNAME = 2;
        private static final int DISKSTATS_MILLIS_SPENT_IO = 12;

        public int getAvgCpuUsage(Object m1, Object m2) {
            final long[] me1 = (long[]) m1;
            final long[] me2 = (long[]) m2;
            final long sampleTimeDelta = me2[MEASURE_CURRENT_TIME_MILLIS] - me1[MEASURE_CURRENT_TIME_MILLIS];
            if (sampleTimeDelta <= 0) {
                return 0;
            }
            long cpuSpentIO = (me2[MEASURE_MILLIS_SPENT_IO] - me1[MEASURE_MILLIS_SPENT_IO]) * Constants.HUNDRED_PERCENT / sampleTimeDelta / NUMBER_OF_PROCESSORS;
            return (int) cpuSpentIO;
        }
    }
    private final static int NUMBER_OF_PROCESSORS = Runtime.getRuntime().availableProcessors();

    /**
     * Returns the Java process CPU usage information.
     */
    private static class JavaCpuUsageStrategy implements ICpuUsageMeasure {

        private static final OperatingSystemMXBean BEAN;
        private static final Class<?> BEAN_CLASS;
        private static final Logger log = Logger.getLogger(JavaCpuUsageStrategy.class.getName());


        static {
            OperatingSystemMXBean b = null;
            Class<?> clazz = null;
            try {
                clazz = Class.forName("com.sun.management.OperatingSystemMXBean");
                b = OperatingSystemMXBean.class.cast(clazz.cast(ManagementFactory.getOperatingSystemMXBean()));
            } catch (Throwable ex) {
                log.log(Level.INFO, "MemoryJMXStrategy disabled: com.sun.management.OperatingSystemMXBean unavailable", ex);
            }
            BEAN = b;
            BEAN_CLASS = clazz;
        }

        public static boolean supported() {
            return BEAN != null;
        }

        public Object measure() throws Exception {
            long processCpuTime = (Long) BEAN_CLASS.getMethod("getProcessCpuTime").invoke(BEAN);
            processCpuTime = processCpuTime / NUMBER_OF_PROCESSORS;
            long totalCpuTime = System.nanoTime();
            return new long[]{processCpuTime, totalCpuTime};
        }
        private static final int TOTAL_CPU_TIME = 1;
        private static final int PROCESS_CPU_TIME = 0;

        public int getAvgCpuUsage(Object m1, Object m2) {
            final long[] me1 = (long[]) m1;
            final long[] me2 = (long[]) m2;
            final long sampleTimeDelta = me2[TOTAL_CPU_TIME] - me1[TOTAL_CPU_TIME];
            if (sampleTimeDelta <= 0) {
                return 0;
            }
            return (int) ((me2[PROCESS_CPU_TIME] - me1[PROCESS_CPU_TIME]) * Constants.HUNDRED_PERCENT / sampleTimeDelta);
        }
    }

    private Cpu() {
        throw new AssertionError();
    }
}
