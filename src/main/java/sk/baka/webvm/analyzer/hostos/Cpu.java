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
import org.apache.commons.io.IOUtils;
import sk.baka.webvm.analyzer.CpuUsage;
import sk.baka.webvm.analyzer.ICpuUsageMeasure;

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
        return new CpuUsage(new CpuUsageLinuxStrategy());
    }

    /**
     * Creates a new measurer for Host OS CPU IO usage (% of time spent waiting for IO).
     * @return the CPU measurer, never null.
     */
    public static CpuUsage newHostIOCpu() {
        return new CpuUsage(new IOCpuUsageLinuxStrategy());
    }

    /**
     * Creates a new measurer for CPU used by the owner java process.
     * @return the CPU measurer, never null.
     */
    public static CpuUsage newJavaCpu() {
        return new CpuUsage(new JavaCpuUsageStrategy());
    }

    /**
     * Returns a Host OS CPU usage information.
     */
    private static class CpuUsageLinuxStrategy implements ICpuUsageMeasure {

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
            final long sampleTimeDelta = me2[0] + me2[1] + me2[2] + me2[3] - me1[0] - me1[1] - me1[2] - me1[3];
            if (sampleTimeDelta <= 0) {
                return 0;
            }
            final long cpuIdle = (me2[3] - me1[3]) * 100 / sampleTimeDelta;
            return 100 - ((int) cpuIdle);
        }
    }

    /**
     * Returns a Host OS CPU time waiting for IO.
     */
    private static class IOCpuUsageLinuxStrategy implements ICpuUsageMeasure {

        private final File procDisk = new File("/proc/diskstats");

        public boolean supported() {
            return procDisk.exists();
        }

        public Object measure() throws Exception {
            // the object is really an array of longs: [weightedMillisSpentIO, currentTimeMillis].
            // To compute the CPU usage, we have to perform:
            // (weightedMillisSpentIO2-weightedMillisSpentIO1)*100/(currentTimeMillis2-currentTimeMillis1)
            long weightedMillisSpentIOTotal = 0;
            final BufferedReader in = new BufferedReader(new FileReader(procDisk));
            final long currentTimeMillis = System.currentTimeMillis();
            try {
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    final StringTokenizer t = new StringTokenizer(line);
                    final List<Object> tokens = Collections.list(t);
                    final String devname = (String) tokens.get(2);
                    if (Character.isDigit(devname.charAt(devname.length() - 1))) {
                        // ignore sda2 etc - we are interested in sda only
                        continue;
                    }
                    final long weightedMillisSpentIO = Long.parseLong((String) tokens.get(12));
                    weightedMillisSpentIOTotal += weightedMillisSpentIO;
                }
            } finally {
                IOUtils.closeQuietly(in);
            }
            return new long[]{weightedMillisSpentIOTotal, currentTimeMillis};
        }

        public int getAvgCpuUsage(Object m1, Object m2) {
            final long[] me1 = (long[]) m1;
            final long[] me2 = (long[]) m2;
            final long sampleTimeDelta = me2[1] - me1[1];
            if (sampleTimeDelta <= 0) {
                return 0;
            }
            long cpuSpentIO = (me2[0] - me1[0]) * 100 / sampleTimeDelta / NUMBER_OF_PROCESSORS;
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

        public boolean supported() {
            return BEAN != null;
        }

        public Object measure() throws Exception {
            long processCpuTime = (Long) BEAN_CLASS.getMethod("getProcessCpuTime").invoke(BEAN);
            processCpuTime = processCpuTime / NUMBER_OF_PROCESSORS;
            long totalCpuTime = System.nanoTime();
            return new long[]{processCpuTime, totalCpuTime};
        }

        public int getAvgCpuUsage(Object m1, Object m2) {
            final long[] me1 = (long[]) m1;
            final long[] me2 = (long[]) m2;
            final long sampleTimeDelta = me2[1] - me1[1];
            if (sampleTimeDelta <= 0) {
                return 0;
            }
            final long cpuUsage = (me2[0] - me1[0]) * 100 / sampleTimeDelta;
            return (int) cpuUsage;
        }
    }

    private Cpu() {
        throw new AssertionError();
    }
}
