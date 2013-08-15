/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * WebMon is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * WebMon. If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.analyzer.hostos.windows;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.jacob.com.EnumVariant;
import com.jacob.com.JacobObject;
import com.jacob.com.LibraryLoader;
import com.jacob.com.Variant;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import sk.baka.webvm.analyzer.hostos.Architecture;
import sk.baka.webvm.analyzer.hostos.MemoryJMXStrategy;
import sk.baka.webvm.analyzer.hostos.OS;
import sk.baka.webvm.analyzer.utils.MiscUtils;

/**
 * Utility methods which access Windows WMI API.
 *
 * @author Martin Vysny
 */
public class WMIUtils {

    private WMIUtils() {
        throw new AssertionError();
    }
    private static final Logger log = Logger.getLogger(WMIUtils.class.getName());
    private static final boolean JACOB_AVAILABLE;
    private static final ActiveXComponent axWMI;

    static {
        boolean available = false;
        ActiveXComponent wmi = null;
        // initialize Jacob
        if (OS.isWindows()) {
            try {
                final File tmp = File.createTempFile("jacob", ".dll");
                final InputStream in = MiscUtils.getResource("jacob/jacob-1.14.3-" + (Architecture.is64Bit() ? "x64" : "x86") + ".dll");
                try {
                    final OutputStream out = new FileOutputStream(tmp);
                    try {
                        IOUtils.copy(in, out);
                    } finally {
                        MiscUtils.closeQuietly(out);
                    }
                } finally {
                    MiscUtils.closeQuietly(in);
                }
                System.setProperty(LibraryLoader.JACOB_DLL_PATH, tmp.getAbsolutePath());
                // cache the WMI instance - this is way faster than creating it anew on each call. Let's hope it is thread-safe :)
                wmi = new ActiveXComponent("winmgmts://");
                log.log(Level.INFO, "JACOB WMI API initialized successfully");
                available = true;
            } catch (Throwable ex) {
                log.log(Level.WARNING, "Cannot initialize JACOB WMI bindings", ex);
            }
        }
        axWMI = wmi;
        JACOB_AVAILABLE = available;
    }

    public static interface HasNativeValue {

        int getNativeValue();
    }

    /**
     * Checks if methods provided by this class are available.
     *
     * @return true if the methods are available, false otherwise.
     */
    public static boolean isAvailable() {
        return JACOB_AVAILABLE;
    }

    private static void checkAvailable() {
        if (!isAvailable()) {
            throw new IllegalStateException("Invalid state: couldn't load JACOB WMI bindings");
        }
    }

    public static enum DriveTypeEnum implements HasNativeValue {

        Unknown(0),
        NoRootDirectory(1),
        RemovableDisk(2),
        LocalDisk(3),
        NetworkDrive(4),
        CompactDisc(5),
        RAMDisk(6);
        public final int nativeValue;

        private DriveTypeEnum(int nativeValue) {
            this.nativeValue = nativeValue;
        }

        public int getNativeValue() {
            return nativeValue;
        }
    }

    private static <T extends Enum<T> & HasNativeValue> T fromNative(Class<T> clazz, int value) {
        for (T c : clazz.getEnumConstants()) {
            if (c.getNativeValue() == value) {
                return c;
            }
        }
        return null;
    }

    /**
     * The drive information.
     */
    public static final class Drive {

        /**
         * File system on the logical disk. Example: NTFS. null if not known.
         */
        public final String fileSystem;
        /**
         * Value that corresponds to the type of disk drive this logical disk
         * represents.
         */
        public final DriveTypeEnum driveType;
        /**
         * The Java file, e.g. "C:\". Never null.
         */
        public final File file;

        public Drive(String fileSystem, DriveTypeEnum driveType, File file) {
            this.fileSystem = fileSystem;
            this.driveType = driveType;
            this.file = file;
        }

        @Override
        public String toString() {
            return "Drive{" + file + ": " + driveType + ", fileSystem=" + fileSystem + "}";
        }
    }

    /**
     * Lists all available Windows drives without actually touching them. This
     * call should not block on cd-roms, floppies, network drives etc.
     *
     * @return a list of drives, never null, may be empty.
     */
    public static List<Drive> getDrives() {
        checkAvailable();
        final List<Drive> result = new ArrayList<Drive>();
        final Variant devices = axWMI.invoke("ExecQuery", new Variant("Select DeviceID,DriveType,FileSystem from Win32_LogicalDisk"));
        final EnumVariant deviceList = new EnumVariant(devices.toDispatch());
        while (deviceList.hasMoreElements()) {
            final Dispatch item = deviceList.nextElement().toDispatch();
            final String drive = Dispatch.call(item, "DeviceID").toString().toUpperCase();
            final File file = new File(drive + "/");
            final DriveTypeEnum driveType = fromNative(DriveTypeEnum.class, Dispatch.call(item, "DriveType").getInt());
            final String fileSystem = Dispatch.call(item, "FileSystem").toString();
            result.add(new Drive(fileSystem, driveType, file));
        }
        return result;
    }

    public static int getCPUUsage() {
        checkAvailable();
        final Variant cpu = axWMI.invoke("ExecQuery", new Variant("Select PercentProcessorTime from Win32_PerfFormattedData_PerfOS_Processor where Name=\"_Total\""));
        final EnumVariant cpuList = new EnumVariant(cpu.toDispatch());
        while (cpuList.hasMoreElements()) {
            final Dispatch item = cpuList.nextElement().toDispatch();
            final String cpuUsagePerc = Dispatch.call(item, "PercentProcessorTime").getString();
            return Integer.parseInt(cpuUsagePerc);
        }
        return 0;
    }

    public static int getIOCPUUsage() {
        checkAvailable();
        final Variant cpu = axWMI.invoke("ExecQuery", new Variant("Select PercentDiskTime from Win32_PerfFormattedData_PerfDisk_PhysicalDisk where Name=\"_Total\""));
        final EnumVariant cpuList = new EnumVariant(cpu.toDispatch());
        while (cpuList.hasMoreElements()) {
            final Dispatch item = cpuList.nextElement().toDispatch();
            final String cpuUsagePerc = Dispatch.call(item, "PercentDiskTime").getString();
            return Integer.parseInt(cpuUsagePerc);
        }
        return 0;
    }
    
    public static Win32_PerfRawData_PerfProc_Process getProcessPerfRawData(int pid) {
        checkAvailable();
        final Variant cpu = axWMI.invoke("ExecQuery", new Variant("Select PercentProcessorTime,TimeStamp_Sys100NS from Win32_PerfRawData_PerfProc_Process where IDProcess=" + pid));
        final EnumVariant cpuList = new EnumVariant(cpu.toDispatch());
        while (cpuList.hasMoreElements()) {
            final Dispatch item = cpuList.nextElement().toDispatch();
            final long p = Long.parseLong(Dispatch.call(item, "PercentProcessorTime").getString());
            final long t = Long.parseLong(Dispatch.call(item, "TimeStamp_Sys100NS").getString());
            return new Win32_PerfRawData_PerfProc_Process(p, t);
        }
        return null;
    }
    
    /**
     * http://msdn.microsoft.com/en-us/library/windows/desktop/aa394323(v=vs.85).aspx
     */
    public static final class Win32_PerfRawData_PerfProc_Process {
        /**
         * Returns elapsed time that all of the threads of this process used the processor to execute instructions in 100 nanoseconds ticks. An instruction is the basic unit of execution in a computer, a thread is the object that executes instructions, and a process is the object created when a program is run. Code executed to handle some hardware interrupts and trap conditions is included in this count.
         */
        public final long percentProcessorTime;
        /**
         * Timestamp value in 100 nanosecond units. This property is inherited from Win32_Perf.
         */
        public final long timeStamp_Sys100NS;

        public Win32_PerfRawData_PerfProc_Process(long percentProcessorTime, long timeStamp_Sys100NS) {
            this.percentProcessorTime = percentProcessorTime;
            this.timeStamp_Sys100NS = timeStamp_Sys100NS;
        }
        
        public static final Win32_PerfRawData_PerfProc_Process ZERO = new Win32_PerfRawData_PerfProc_Process(0, 1);
        /**
         * Returns average CPU usage over given interval.
         * @param previous the previous sample, may be null - in such case zero is returned.
         * @return a value in percent, 0..100
         */
        public int getCPUUsage(Win32_PerfRawData_PerfProc_Process previous) {
            if (previous == null) {
                return 0;
            }
            final long nd = percentProcessorTime - previous.percentProcessorTime;
            final long dd = timeStamp_Sys100NS - previous.timeStamp_Sys100NS;
            if (dd < 0 || nd < 0) {
                throw new IllegalArgumentException("Parameter previous: invalid value " + previous + ": sampled later than this: " + this);
            }
            if (dd == 0) {
                return 0;
            }
            return (int) (nd * 100 / Runtime.getRuntime().availableProcessors() / dd);
        }

        @Override
        public String toString() {
            return "Win32_PerfRawData_PerfProc_Process{" + "percentProcessorTime=" + percentProcessorTime + ", timeStamp_Sys100NS=" + timeStamp_Sys100NS + '}';
        }
    }
    
    public static MemoryUsage getWorkingSetSize(int pid) {
        checkAvailable();
        final Variant cpu = axWMI.invoke("ExecQuery", new Variant("Select WorkingSetSize,PeakWorkingSetSize,MaximumWorkingSetSize from Win32_Process where ProcessId=" + pid));
        final EnumVariant cpuList = new EnumVariant(cpu.toDispatch());
        while (cpuList.hasMoreElements()) {
            final Dispatch item = cpuList.nextElement().toDispatch();
            final long workingSetSize = Long.valueOf(Dispatch.call(item, "WorkingSetSize").getString());
            final long peakWorkingSetSize = (long) Dispatch.call(item, "PeakWorkingSetSize").getInt() * 1024L;
//            final long maximumWorkingSetSize = (long) Dispatch.call(item, "MaximumWorkingSetSize").getInt() * 1024L;
            return new MemoryUsage(0, workingSetSize, peakWorkingSetSize, Math.max(workingSetSize, peakWorkingSetSize));
        }
        return new MemoryUsage(0, 0, 0, 0);
    }
    
    /**
     * Detects Windows swap usage, in bytes.
     *
     * @return swap usage.
     */
    public static MemoryUsage getSwapUsage() {
        checkAvailable();
        final Variant currentSwap = axWMI.invoke("ExecQuery", new Variant("Select AllocatedBaseSize,CurrentUsage from Win32_PageFileUsage"));
        long used = 0;
        long comitted = 0;
        final EnumVariant currentSwapList = new EnumVariant(currentSwap.toDispatch());
        while (currentSwapList.hasMoreElements()) {
            final Dispatch item = currentSwapList.nextElement().toDispatch();
            used += (long) Dispatch.call(item, "CurrentUsage").getInt() * 1024 * 1024;
            comitted += (long) Dispatch.call(item, "AllocatedBaseSize").getInt() * 1024 * 1024;
        }
        final Variant maxSwap = axWMI.invoke("ExecQuery", new Variant("Select InitialSize,MaximumSize from Win32_PageFileSetting"));
        long initial = 0;
        long max = 0;
        final EnumVariant maxSwapList = new EnumVariant(maxSwap.toDispatch());
        while (maxSwapList.hasMoreElements()) {
            final Dispatch item = currentSwapList.nextElement().toDispatch();
            initial += (long) Dispatch.call(item, "InitialSize").getInt() * 1024 * 1024;
            max += (long) Dispatch.call(item, "MaximumSize").getInt() * 1024 * 1024;
        }
        if (max < comitted) {
            // incorrect maximum? Ask JMX
            final MemoryUsage swap = new MemoryJMXStrategy().getSwap();
            if (swap != null) {
                max = swap.getMax();
            }
        }
        return max < comitted ? new MemoryUsage(initial, used, used, comitted) : new MemoryUsage(initial, used, comitted, max);
    }

    private static void closeQuietly(JacobObject obj) {
        try {
            obj.safeRelease();
        } catch (Exception ex) {
            log.log(Level.INFO, "Failed to close JacobObject " + obj.getClass(), ex);
        }
    }
}
