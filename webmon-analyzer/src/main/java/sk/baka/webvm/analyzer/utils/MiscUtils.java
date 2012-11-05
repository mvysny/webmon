package sk.baka.webvm.analyzer.utils;

import java.io.Closeable;
import java.lang.management.ThreadInfo;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Martin Vysny
 */
public class MiscUtils {

    public static boolean isBlank(Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return true;
        }
        for (Object obj: collection) {
            if (obj != null) {
                return false;
            }
        }
        return true;
    }
    public static boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }
    
    public static void closeQuietly(Closeable c) {
        try {
            c.close();
        } catch (Throwable t) {
            log.log(Level.INFO, "Failed to close " + c, t);
        }
    }
    private static final Logger log = Logger.getLogger(MiscUtils.class.getName());

    /**
     * Pretty-prints a thread stacktrace, similar to {@link Throwable#printStackTrace()} except that it handles nulls correctly.
     * @param info
     * @return string representation of the stacktrace.
     */
    public static String getThreadStacktrace(final ThreadInfo info) {
        final StringBuilder sb = new StringBuilder();
        final StackTraceElement[] stack = info.getStackTrace();
        if (stack == null) {
            sb.append("  stack trace not available");
        } else if (stack.length == 0) {
            sb.append("  stack trace is empty");
        } else {
            for (final StackTraceElement ste : stack) {
                sb.append("  at ");
                sb.append(ste.toString());
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Shows a basic thread info: thread ID, whether the thread is native, suspended, etc.
     * @param info the thread info.
     * @return pretty-printed thread info.
     */
    public static String getThreadMetadata(final ThreadInfo info) {
        final StringBuilder sb = new StringBuilder();
        sb.append("0x");
        sb.append(Long.toHexString(info.getThreadId()));
        sb.append(" [");
        sb.append(info.getThreadName());
        sb.append("] ");
        sb.append(info.getThreadState().toString());
        if (info.isInNative()) {
            sb.append(", in native");
        }
        if (info.isSuspended()) {
            sb.append(", suspended");
        }
        final String lockName = info.getLockName();
        if (lockName != null) {
            sb.append(", locked on [");
            sb.append(lockName);
            sb.append("]");
            sb.append(" owned by thread ");
            sb.append(info.getLockOwnerId());
            sb.append(" [");
            sb.append(info.getLockOwnerName());
            sb.append("]");
        }
        return sb.toString();
    }
}