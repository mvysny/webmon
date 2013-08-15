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

import sk.baka.webvm.analyzer.hostos.linux.MemoryLinuxStrategy;
import sk.baka.webvm.analyzer.hostos.windows.CpuUsageWindowsStrategy;
import sk.baka.webvm.analyzer.hostos.windows.IOCpuUsageWindowsStrategy;
import java.lang.management.OperatingSystemMXBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.baka.webvm.analyzer.CpuUsage;
import sk.baka.webvm.analyzer.utils.Constants;
import sk.baka.webvm.analyzer.utils.MiscUtils;
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
    public static CpuUsage newHostCpu() {
        final ICpuUsageMeasure cpuusage;
        if (OS.isWindows() && WMIUtils.isAvailable()) {
            cpuusage = new CpuUsageWindowsStrategy();
        } else if (OS.isLinux() || OS.isAndroid()) {
            cpuusage = new CpuUsageLinuxStrategy();
        } else {
            cpuusage = new DummyCpuUsageStrategy();
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
        if (OS.isWindows() && WMIUtils.isAvailable()) {
            io = new IOCpuUsageWindowsStrategy();
        } else if (OS.isLinux() || OS.isAndroid()) {
            io = new IOCpuUsageLinuxStrategy();
        } else {
            io = new DummyCpuUsageStrategy();
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
     * Parses the /proc/stat file on Linux.
     */
    public static final class LinuxProcStat {
        private final long user;
        private final long nice;
        private final long system;
        private final long idle;

        public LinuxProcStat(long user, long nice, long system, long idle) {
            this.user = user;
            this.nice = nice;
            this.system = system;
            this.idle = idle;
        }
        public static boolean isAvailable() {
            return PROC_STAT.exists();
        }
        private static final File PROC_STAT = new File("/proc/stat");

        public static LinuxProcStat now() {
            try {
                final Scanner s = new Scanner(PROC_STAT);
                try {
                    for (; s.hasNextLine();) {
                        final String name = s.next();
                        if (name.equals("cpu")) {
                            return new LinuxProcStat(s.nextLong(), s.nextLong(), s.nextLong(), s.nextLong());
                        }
                        s.nextLine(); // skip this line, try the next one
                    }
                    return null;
                } finally {
                    MiscUtils.closeQuietly(s);
                }
            } catch (IOException ex) {
                log.log(Level.INFO, "Failed to parse " + PROC_STAT, ex);
            }
            return null;
        }
        public long getTotal() {
            return user + nice + system + idle;
        }
        public int getCpuUsage(LinuxProcStat prev) {
            int cpuIdle = (int) (Constants.HUNDRED_PERCENT * (idle - prev.idle) / (getTotal() - prev.getTotal()));
            if (cpuIdle < 0) {
                cpuIdle = 0;
            }
            // To compute the CPU usage, we have to perform:
            // (idle2-idle1)*HUNDRED_PERCENT/(user2+nice2+system2+idle2-user1-nice1-system1-idle1)
            return Constants.HUNDRED_PERCENT - cpuIdle;
        }
    }
    
    /**
     * Returns a Host OS CPU usage information.
     */
    private static class CpuUsageLinuxStrategy implements ICpuUsageMeasure {

        public Object measure() throws Exception {
            return LinuxProcStat.now();
        }

        public int getAvgCpuUsage(Object m1, Object m2) {
            return ((LinuxProcStat) m2).getCpuUsage((LinuxProcStat) m1);
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
    
    private static class OneProcessCpuUsageWindowsStrategy implements ICpuUsageMeasure {
        public static boolean isAvailable() {
            return WMIUtils.isAvailable();
        }
        private final int pid;
        public OneProcessCpuUsageWindowsStrategy(int pid) {
            this.pid = pid;
        }
        public Object measure() throws Exception {
            return WMIUtils.getProcessPerfRawData(pid);
        }

        public int getAvgCpuUsage(Object m1, Object m2) {
            return ((WMIUtils.Win32_PerfRawData_PerfProc_Process) m2).getCPUUsage((WMIUtils.Win32_PerfRawData_PerfProc_Process) m1);
        }
    }
    private static final Logger log = Logger.getLogger(Cpu.class.getName());
    private static class ProcessCpuUsageLinuxStrategy implements ICpuUsageMeasure {
        public final int pid;

        public ProcessCpuUsageLinuxStrategy(int pid) {
            this.pid = pid;
        }
        
        public static boolean isAvailable() {
            return LinuxProcStat.isAvailable();
        }

        public Object measure() throws Exception {
            return StatWithTime.now(pid);
        }

        public int getAvgCpuUsage(Object m1, Object m2) {
            return ((StatWithTime) m2).getCpuUsage((StatWithTime) m1);
        }
    }
    
    private static final class StatWithTime {
        public final Stat stat;
        public final LinuxProcStat procStat;
        public StatWithTime(Stat stat, LinuxProcStat procStat) {
            this.stat = stat;
            this.procStat = procStat;
        }
        public static StatWithTime now(int pid) {
            return new StatWithTime(Stat.now(pid), LinuxProcStat.now());
        }
        public int getCpuUsage(StatWithTime prev) {
            if (prev == null || stat == null || procStat == null) {
                return 0;
            }
            final long du = stat.utimeJiffies - prev.stat.utimeJiffies;
            final long ds = stat.stimeJiffies - prev.stat.stimeJiffies;
            final long dt = procStat.getTotal() - prev.procStat.getTotal();
            if (du < 0 || ds < 0 || dt < 0) {
                throw new IllegalArgumentException("Parameter prev: invalid value " + prev + ": does not precede this: " + this);
            }
            if (dt == 0) {
                return 0;
            }
            final int user = (int) (100L * Runtime.getRuntime().availableProcessors() * du / dt);
            final int sys = (int) (100L * Runtime.getRuntime().availableProcessors() * ds / dt);
            final int sum = user + sys;
            return sum;
        }

        @Override
        public String toString() {
            return "StatWithTime{" + "stat=" + stat + ", procStat=" + procStat + '}';
        }
    }
    
    /**
     * The contents of the Linux /proc/[pid]/stat file.
     */
    public static final class Stat {
        public final long utimeJiffies;
        public final long stimeJiffies;
        /**
         * RSS, Resident Set Size: number of pages the process has in real memory. This is just the pages which count toward text, data, or stack space. This does not include pages which have not been demand-loaded in, or which are swapped out.
         */
        public final int rssPages;
        
        /**
         * Returns the RSS field in bytes.
         * @return RSS in bytes.
         */
        public long getRSSAsBytes() {
            if (MemoryLinuxStrategy.PAGE_SIZE < 0) {
                throw new IllegalStateException("Invalid state: Linux page size not available");
            }
            return (long) rssPages * MemoryLinuxStrategy.PAGE_SIZE;
        }

        public Stat(long utimeJiffies, long stimeJiffies, int rssPages) {
            this.utimeJiffies = utimeJiffies;
            this.stimeJiffies = stimeJiffies;
            this.rssPages = rssPages;
        }
        
        public static Stat now(int pid) {
            final File pidstat = new File("/proc/" + pid + "/stat");
            final String[] stat;
            try {
                final Scanner s = new Scanner(pidstat);
                try {
                    stat = s.nextLine().trim().split("\\s+");
                } finally {
                    MiscUtils.closeQuietly(s);
                }
            } catch (IOException ex) {
                log.log(Level.INFO, "Failed to parse " + pidstat, ex);
                return null;
            }
            final long utimeJiffies = Long.parseLong(stat[13]);
            final long stimeJiffies = Long.parseLong(stat[14]);
            final int rssPages = Integer.parseInt(stat[23]);
            return new Stat(utimeJiffies, stimeJiffies, rssPages);
        }

        @Override
        public String toString() {
            return "Stat{" + "utimeJiffies=" + utimeJiffies + ", stimeJiffies=" + stimeJiffies + ", rssPages=" + rssPages + '}';
        }
    }

    /**
     * Measures CPU usage of a single process.
     * @param pid the process ID. If the PID is invalid, 0 will always be returned.
     * @return CPU usage, never null. Always returns 0 on unsupported platforms.
     */
    public static CpuUsage newProcessCPUUsage(int pid) {
        final ICpuUsageMeasure m;
        if (OneProcessCpuUsageWindowsStrategy.isAvailable()) {
            m = new OneProcessCpuUsageWindowsStrategy(pid);
        } else if (ProcessCpuUsageLinuxStrategy.isAvailable()) {
            m = new ProcessCpuUsageLinuxStrategy(pid);
        } else {
            m = new DummyCpuUsageStrategy();
        }
        return new CpuUsage(m);
    }
}
