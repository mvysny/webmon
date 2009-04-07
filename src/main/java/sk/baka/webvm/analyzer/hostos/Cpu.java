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
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 * Provides a CPU measurement support.
 * @author Martin Vysny
 */
public final class Cpu {

    private final static Logger log = Logger.getLogger(Cpu.class.getName());
    /**
     * The CPU usage measurer.
     */
    private final ICpuUsage cpuUsage;

    private Cpu(final ICpuUsage usage) {
        if (usage.supported()) {
            cpuUsage = usage;
        } else {
            cpuUsage = null;
        }
    }

    /**
     * Creates a new measurer for Host OS CPU usage.
     * @return the CPU measurer, never null.
     */
    public static Cpu newHostCpu() {
        return new Cpu(new CpuUsageLinuxStrategy());
    }

    /**
     * Creates a new measurer for CPU used by the owner java process.
     * @return the CPU measurer, never null.
     */
    public static Cpu newJavaCpu() {
        return new Cpu(new JavaCpuUsageStrategy());
    }

    /**
     * Checks if this measurement is supported.
     * @return true if supported, false if {@link #getCpuUsage()} will always return -1.
     */
    public boolean supported() {
        return cpuUsage != null;
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
                log.log(Level.SEVERE, "Failed to measure a CPU usage", ex);
                return -1;
            }
            return 0;
        }
        final Object newMeasurement;
        try {
            newMeasurement = cpuUsage.measure();
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Failed to measure a CPU usage", ex);
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

    /**
     * Returns a Host OS CPU usage information.
     */
    private static class CpuUsageLinuxStrategy implements ICpuUsage {

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
            final long cpuIdle = (me2[3] - me1[3]) * 100 / (me2[0] + me2[1] + me2[2] + me2[3] - me1[0] - me1[1] - me1[2] - me1[3]);
            return 100 - ((int) cpuIdle);
        }
    }

    /**
     * Returns the Java process CPU usage information.
     */
    private static class JavaCpuUsageStrategy implements ICpuUsage {

        private final static int numberOfProcessors = Runtime.getRuntime().availableProcessors();
        private static final OperatingSystemMXBean BEAN;
        private static final Class<?> BEAN_CLASS;


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
            processCpuTime = processCpuTime / numberOfProcessors;
            long totalCpuTime = System.nanoTime();
            return new long[]{processCpuTime, totalCpuTime};
        }

        public int getAvgCpuUsage(Object m1, Object m2) {
            final long[] me1 = (long[]) m1;
            final long[] me2 = (long[]) m2;
            final long cpuUsage = (me2[0] - me1[0]) * 100 / (me2[1] - me1[1]);
            return (int) cpuUsage;
        }
    }
}
