package sk.baka.webvm.analyzer.utils;

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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import sk.baka.webvm.analyzer.hostos.Architecture;
import sk.baka.webvm.analyzer.hostos.OS;

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
    static {
        boolean available = false;
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
                available = true;
            } catch (Throwable ex) {
                log.log(Level.WARNING, "Cannot initialize JACOB WMI bindings", ex);
            }
        }
        JACOB_AVAILABLE = available;
    }

    public static interface HasNativeValue {

        int getNativeValue();
    }
    /**
     * Checks if methods provided by this class are available.
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
         * Value that corresponds to the type of disk drive this logical disk represents.
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
     * Lists all available Windows drives without actually touching them. This call should not block on cd-roms, floppies, network drives etc.
     * @return a list of drives, never null, may be empty.
     */
    public static List<Drive> getDrives() {
        checkAvailable();
        final List<Drive> result = new ArrayList<Drive>();
        final ActiveXComponent axWMI = new ActiveXComponent("winmgmts://");
        try {
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
        } finally {
            closeQuietly(axWMI);
        }
    }
    
    private static void closeQuietly(JacobObject obj) {
        try {
            obj.safeRelease();
        } catch (Exception ex) {
            log.log(Level.INFO, "Failed to close JacobObject " + obj.getClass(), ex);
        }
    }
}
