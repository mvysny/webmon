package sk.baka.webvm.analyzer.hostos;

import java.util.logging.Logger;

/**
 * @author Martin Vysny
 */
public enum OS {
    Windows,
    Linux,
    Android,
    MacOS;
    private static OS os;

    /**
     * Returns the operating system that we are running currently on.
     * @return OS, may be null on unsupported OSes.
     */
    public static OS get() {
        if (os == null) {
            final String osname = System.getProperty("os.name");
            if (isAndroid()) {
                os = Android;
            } else if (isWindows()) {
                os = Windows;
            } else if (osname.startsWith("Linux")) {
                os = Linux;
            } else if (osname.startsWith("Mac OS")) {
                os = MacOS;
            } else {
                return null;
            }
        }
        return os;
    }
    
    private static final boolean IS_WINDOWS;
    static {
        IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");
    }

    /**
     * True if we are running on Windows.
     *
     * @return true if we are running on Windows, false otherwise.
     */
    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    /**
     * True if we are running on Mac.
     * @return true if running on Mac.
     */
    public static boolean isMac() {
        return get() == OS.MacOS;
    }
    
    /**
     * True if we are running on Linux. Returns false if we are running on Android.
     * @return true if this system is a full-blown Linux, false if it is Android or some other OS.
     */
    public static boolean isLinux() {
        return get() == OS.Linux;
    }
    
    private static final Logger log = Logger.getLogger(OS.class.getName());
    
    private static final boolean IS_ANDROID;

    static {
        boolean isAndroid = true;
        try {
            OS.class.getClassLoader().loadClass("android.app.Activity");
        } catch (ClassNotFoundException ex) {
            isAndroid = false;
        }
        IS_ANDROID = isAndroid;
        log.info("Running on " + (IS_ANDROID ? "Android" : "Regular Java"));
    }

    /**
     * Checks if we are running on an Android phone/tablet.
     *
     * @return true if we are running on Android, false otherwise.
     */
    public static boolean isAndroid() {
        return IS_ANDROID;
    }
}
