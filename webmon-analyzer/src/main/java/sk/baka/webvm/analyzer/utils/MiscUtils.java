package sk.baka.webvm.analyzer.utils;

import sk.baka.webvm.analyzer.hostos.windows.WMIUtils;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.baka.webvm.analyzer.hostos.OS;
import sk.baka.webvm.analyzer.hostos.windows.WMIUtils.Drive;

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
//            log.log(Level.INFO, "Failed to close " + c + ": " + t);  // do not log exception stacktrace: it will only clutter the log
        }
    }
    private static final Logger log = Logger.getLogger(MiscUtils.class.getName());

    /**
     * Returns stacktrace of given throwable.
     * @param t throwable, not null.
     * @return the stacktrace.
     */
    public static String getStacktrace(final Throwable t) {
        final StringWriter out = new StringWriter();
        t.printStackTrace(new PrintWriter(out));
        return out.toString();
    }

    public static InputStream getResource(String resource) {
        final InputStream result = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (result == null) {
            throw new IllegalArgumentException("Parameter result: invalid value " + result + ": no such resource");
        }
        return result;
    }

    /**
     * Returns a list of local hard-drives.
     * @return 
     */
    public static List<File> getLocalHarddrives() {
        final List<File> result = new ArrayList<File>();
        if (WMIUtils.isAvailable()) {
            for (Drive drive: WMIUtils.getDrives()) {
                if (drive.driveType == WMIUtils.DriveTypeEnum.LocalDisk) {
                    result.add(drive.file);
                }
            }
        } else {
            // fallback to Java means. Java cannot detect local harddrives for Windows properly, therefore we will just return C: (if available).
            if (OS.isWindows()) {
                final File c = new File("c:/");
                if (c.exists()) {
                    result.add(c);
                }
            } else {
                result.add(new File("/"));
            }
        }
        return result;
    }

    /**
     * Normalizes slashes in given string, i.e. backslashes are converted to forward slashes.
     * @param fileName the file name to normalize
     * @return name with backslashes converted to forward slashes.
     */
    public static String normalizeSlashes(final String fileName) {
        if (fileName == null) {
            return null;
        }
        return fileName.replace('\\', '/');
    }
    /**
     * Converts given file object to a local filesystem file with absolute path. Returns null if the file is not located on a local filesystem.
     * @param fileUri the file to convert, in the URI format. WARNING: if given string is a local filesystem-dependent path
     * then the function may return null.
     * @return filesystem-dependent location of the file. null if given URL is not a local-filesystem URL.
     * @throws IllegalArgumentException if the file is not located on the local filesystem.
     */
    public static File toLocalFile(final String fileUri) {
        final String uri = normalizeSlashes(fileUri);
        final URI url;
        try {
            url = new URI(uri);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse " + uri + ": " + ex, ex);
        }
        if (url.isOpaque() && !"file".equals(url.getScheme())) {
            // Opaque URI: cannot be a filesystem reference
            return null;
        }
        if (!url.isAbsolute() && !uri.startsWith("/")) {
            // relative URI, already a local filesystem reference
            return new File(uri);
        }
        if (url.getScheme() == null) {
            // no scheme, perhaps already a local filesystem reference. return null
            return null;
        }
        String localFile = null;
        try {
            localFile = URLDecoder.decode(uri, "UTF-8");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse " + uri + ": " + ex, ex);
        }
        if (localFile.startsWith(FILE_URI_HIERARCHICAL_PREFIX)) {
            localFile = localFile.substring(FILE_URI_HIERARCHICAL_PREFIX.length());
        } else if (localFile.startsWith(FILE_URI_OPAQUE_PREFIX)) {
            localFile = localFile.substring(FILE_URI_OPAQUE_PREFIX.length());
        } else {
            return null;
        }
        return new File(localFile);
    }
    /**
     * An opaque URI file prefix: {@value #FILE_URI_OPAQUE_PREFIX}.
     */
    private static final String FILE_URI_OPAQUE_PREFIX = "file:";
    /**
     * A hierarchical URI file prefix: {@value #FILE_URI_HIERARCHICAL_PREFIX}.
     */
    private static final String FILE_URI_HIERARCHICAL_PREFIX = "file://";

    private static final int BUFSIZE = 8192;
    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        try {
            final byte[] buffer = new byte[BUFSIZE];
            for (int bytesRead = inputStream.read(buffer); bytesRead >= 0; bytesRead = inputStream.read(buffer)) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } finally {
            closeQuietly(inputStream);
        }
    }
    public static String toString(InputStream inputStream) throws IOException {
        return new String(toByteArray(inputStream));
    }

    public static String toString(InputStream inputStream, String charset) throws IOException {
        return new String(toByteArray(inputStream), charset);
    }

    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        copy(inputStream, bout);
        return bout.toByteArray();
    }
}
