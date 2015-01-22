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

import sk.baka.webvm.analyzer.hostos.windows.OneProcessCpuUsageWindowsStrategyStrategy;
import sk.baka.webvm.analyzer.hostos.linux.ProcessCpuUsageLinuxStrategyStrategy;
import sk.baka.webvm.analyzer.hostos.windows.CpuUsageWindowsStrategyStrategy;
import sk.baka.webvm.analyzer.hostos.windows.IOCpuUsageWindowsStrategyStrategy;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.baka.webvm.analyzer.CPUUsageMeasurer;
import sk.baka.webvm.analyzer.hostos.linux.CpuUsageLinuxStrategyStrategy;
import sk.baka.webvm.analyzer.hostos.linux.IOCpuUsageLinuxStrategyStrategy;
import sk.baka.webvm.analyzer.utils.Constants;
import sk.baka.webvm.analyzer.hostos.windows.WMIUtils;

/**
 * Provides a CPU measurement support.
 * @author Martin Vysny
 */
public final class Cpu {

    /**
     * Creates a new measurer for Host OS CPU usage.
     * @return the CPU measurer, never null.
     */
    public static CPUUsageMeasurer newHostCpu() {
        final ICpuUsageMeasureStrategy cpuusage;
        if (OS.isWindows() && WMIUtils.isAvailable()) {
            cpuusage = new CpuUsageWindowsStrategyStrategy();
        } else if (OS.isLinux() || OS.isAndroid()) {
            cpuusage = new CpuUsageLinuxStrategyStrategy();
        } else {
            cpuusage = new DummyCpuUsageStrategyStrategy();
        }
        return new CPUUsageMeasurer(cpuusage);
    }
    private static final CPUUsageMeasurer HOST_CPU = newHostCpu();

    /**
     * Checks if Host OS CPU usage measurement is supported.
     * @return true if supported.
     */
    public static boolean isHostCpuSupported() {
        return !(HOST_CPU.cpuUsage instanceof DummyCpuUsageStrategyStrategy);
    }

    private static class DummyCpuUsageStrategyStrategy implements ICpuUsageMeasureStrategy {

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
    public static CPUUsageMeasurer newHostIOCpu() {
        final ICpuUsageMeasureStrategy io;
        if (OS.isWindows() && WMIUtils.isAvailable()) {
            io = new IOCpuUsageWindowsStrategyStrategy();
        } else if (OS.isLinux() || OS.isAndroid()) {
            io = new IOCpuUsageLinuxStrategyStrategy();
        } else {
            io = new DummyCpuUsageStrategyStrategy();
        }
        return new CPUUsageMeasurer(io);
    }
    private static final CPUUsageMeasurer HOST_IO_CPU = newHostIOCpu();

    /**
     * Checks if measurer for Host OS CPU IO usage (% of time spent waiting for IO) is supported.
     * @return true if supported.
     */
    public static boolean isHostIOCpuSupported() {
        return !(HOST_IO_CPU.cpuUsage instanceof DummyCpuUsageStrategyStrategy);
    }

    /**
     * Creates a new measurer for CPU used by the owner java process.
     * @return the CPU measurer, never null.
     */
    public static CPUUsageMeasurer newJavaCpu() {
        return new CPUUsageMeasurer(JavaCpuUsageStrategyStrategy.supported() ? new JavaCpuUsageStrategyStrategy() : new DummyCpuUsageStrategyStrategy());
    }

    /**
     * Checks if measurer for CPU used by the owner java process is supported.
     * @return true if supported.
     */
    public static boolean isJavaCpuSupported() {
        return JavaCpuUsageStrategyStrategy.supported();
    }

    private final static int NUMBER_OF_PROCESSORS = Runtime.getRuntime().availableProcessors();

    /**
     * Returns the Java process CPU usage information.
     */
    private static class JavaCpuUsageStrategyStrategy implements ICpuUsageMeasureStrategy {

        private static final OperatingSystemMXBean BEAN;
        private static final Class<?> BEAN_CLASS;
        private static final Logger log = Logger.getLogger(JavaCpuUsageStrategyStrategy.class.getName());


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
    
    /**
     * Measures CPU usage of a single process.
     * @param pid the process ID. If the PID is invalid, 0 will always be returned.
     * @return CPU usage, never null. Always returns 0 on unsupported platforms.
     */
    public static CPUUsageMeasurer newProcessCPUUsage(int pid) {
        final ICpuUsageMeasureStrategy m;
        if (OneProcessCpuUsageWindowsStrategyStrategy.isAvailable()) {
            m = new OneProcessCpuUsageWindowsStrategyStrategy(pid);
        } else if (ProcessCpuUsageLinuxStrategyStrategy.isAvailable()) {
            m = new ProcessCpuUsageLinuxStrategyStrategy(pid);
        } else {
            m = new DummyCpuUsageStrategyStrategy();
        }
        return new CPUUsageMeasurer(m);
    }
}
