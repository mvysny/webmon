package sk.baka.webvm.analyzer.hostos;

import java.util.logging.Logger;

/**
 *
 * @author Martin Vysny
 */
public enum Architecture {

    X86("x86", new String[]{"x86", "i386"}),
    Sparc("sparc", new String[]{"sparc"}),
    PowerPC("ppc", new String[]{"ppc", "PowerPC"}),
    ARM("arm", new String[]{"arm"}),
    MIPS("mips", new String[]{"mips"}),
    Alpha("alpha", new String[]{"alpha"});
    private final String[] osarch;
    public final String resourceFriendlyName;

    private Architecture(String resourceFriendlyName, String[] osarch) {
        this.osarch = osarch;
        this.resourceFriendlyName = resourceFriendlyName;
    }
    private static Architecture current;

    public static Architecture get() {
        if (current == null) {
            final String osarch = System.getProperty("os.arch");
            for (Architecture arch : Architecture.values()) {
                for (String a : arch.osarch) {
                    if (osarch.startsWith(a)) {
                        current = arch;
                        break;
                    }
                }
            }
            if (current == null) {
                throw new RuntimeException("Unknown/unsupported architecture: " + osarch);
            }
        }
        return current;
    }
    private static String FULL_ARCH_NAME;

    public static String getFullName() {
        if (FULL_ARCH_NAME == null) {
            FULL_ARCH_NAME = get().resourceFriendlyName + "_" + (is64Bit() ? "64" : "32");
        }
        return FULL_ARCH_NAME;
    }

    private static final boolean IS_64BIT;

    static {
        IS_64BIT = System.getProperty("os.arch").contains("64");
    }

    /**
     * True if we are running on 64bit java, false if 32bit.
     *
     * @return true if we are running on 64bit java, false if 32bit.
     */
    public static boolean is64Bit() {
        return IS_64BIT;
    }

    private static final Logger log = Logger.getLogger(Architecture.class.getName());
    
    static {
        log.info("os.arch: " + System.getProperty("os.arch") + ", os.name: " + System.getProperty("os.name"));
        log.info("Detected Java platform: " + Architecture.getFullName() + ", running on " + OS.get());
    }
}
