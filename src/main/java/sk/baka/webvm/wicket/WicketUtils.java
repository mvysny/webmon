/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.baka.webvm.wicket;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.request.target.resource.ResourceStreamRequestTarget;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.time.Time;
import sk.baka.webvm.analyzer.classloader.ResourceLink;

/**
 * Contains Wicket utilities.
 * @author Martin Vysny
 */
public final class WicketUtils {

    /**
     * Redirects to (and starts download process of) given resource.
     * @param resLink the link to download, must not be null
     * @return true if the redirect was succesfull, false if given link is not a downloadable content.
     */
    public static boolean redirectTo(final ResourceLink resLink) {
        final RequestCycle rc = RequestCycle.get();
        if (resLink.isRoot()) {
            final File container = resLink.getContainer();
            if (container.isFile()) {
                rc.setRequestTarget(new ResourceStreamRequestTarget(new FileResourceStream(container), container.getName()));
                rc.setRedirect(true);
                return true;
            }
            return false;
        }
        if (resLink.isPackage()) {
            return false;
        }
        rc.setRequestTarget(new ResourceStreamRequestTarget(toStream(resLink), resLink.getName()));
        rc.setRedirect(true);
        return true;
    }

    private WicketUtils() {
        throw new AssertionError();
    }

    private final static Logger log = Logger.getLogger(WicketUtils.class.getName());

    /**
     * Converts a resource link to a resource stream.
     * @param link the link to convert. Must not be a package nor a root link.
     * @return converted resource stream.
     */
    public static IResourceStream toStream(final ResourceLink link) {
        if (link.isPackage()) {
            throw new IllegalArgumentException(link.getFullName() + " is a package");
        }
        return new IResourceStream() {

            public String getContentType() {
                return URLConnection.getFileNameMap().getContentTypeFor(link.getName());
            }

            public long length() {
                try {
                    return link.getLength();
                } catch (IOException ex) {
                    log.log(Level.WARNING, null, ex);
                    return -1;
                }
            }

            public InputStream getInputStream() throws ResourceStreamNotFoundException {
                final InputStream result;
                try {
                    result = link.open();
                } catch (IOException ex) {
                    throw new ResourceStreamNotFoundException(ex);
                }
                streams.add(result);
                return result;
            }
            private final List<InputStream> streams = new ArrayList<InputStream>();

            public void close() throws IOException {
                for (final InputStream is : streams) {
                    IOUtils.closeQuietly(is);
                }
                streams.clear();
            }

            public Locale getLocale() {
                return locale;
            }
            private Locale locale;

            public void setLocale(Locale locale) {
                this.locale = locale;
            }

            public Time lastModifiedTime() {
                return Time.milliseconds(1);
            }
        };
    }
}
