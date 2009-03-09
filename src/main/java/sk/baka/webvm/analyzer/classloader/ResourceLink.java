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
package sk.baka.webvm.analyzer.classloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Represents an on-disk package or a package item.
 * @author Martin Vysny
 */
public abstract class ResourceLink implements Serializable {

    /**
     * Opens given directory or jar file and allows package and resource enumeration.
     * @param file the resource directory or a jar file
     * @return new resource enumerator.
     */
    public static ResourceLink newFor(final File file) {
        if (file.isDirectory()) {
            return new DirResourceLink(file);
        } else {
            return new JarResourceLink(file, "");
        }
    }

    /**
     * Lists all direct child packages and items of this package. It is invalid to call this method on a non-package resource.
     * @return list of all children.
     */
    public abstract List<ResourceLink> list() throws IOException;

    /**
     * Checks if resource denoted by this object is actually a package, or just a resource file.
     * @return
     */
    public abstract boolean isPackage();

    /**
     * Opens a stream to the file denoted by this link. It is invalid to call this method on a package resource.
     * @return the file contents.
     */
    public abstract InputStream open() throws IOException;

    /**
     * Returns the package/resource name. Does not include names of the parent packages nor any slash characters. Name of the root package is not defined.
     * @return the name of the resource denoted by this link.
     */
    public abstract String getName();

    @Override
    public String toString() {
        return getName();
    }
}
/**
 * Provides package information for a directory containing classpath items.
 * @author Martin Vysny
 */
final class DirResourceLink extends ResourceLink {

    private final File file;

    DirResourceLink(final File file) {
        this.file = file;
    }

    @Override
    public List<ResourceLink> list() {
        final File[] children = file.listFiles();
        final List<ResourceLink> result = new ArrayList<ResourceLink>(children.length);
        for (final File child : children) {
            result.add(new DirResourceLink(child));
        }
        return result;
    }

    @Override
    public boolean isPackage() {
        return file.isDirectory();
    }

    @Override
    public InputStream open() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public String getName() {
        return file.getName();
    }
}

/**
 * Provides package information for an on-disk jar file.
 * @author Martin Vysny
 */
final class JarResourceLink extends ResourceLink {

    private final File jarFile;
    private final String fullEntryName;

    JarResourceLink(final File jarFile, final String fullEntryName) {
        this.jarFile = jarFile;
        this.fullEntryName = fullEntryName;
    }

    @Override
    public List<ResourceLink> list() throws IOException {
        if (!isPackage()) {
            throw new IllegalStateException("not a package");
        }
        final List<ResourceLink> result = new ArrayList<ResourceLink>();
        final ZipFile zfile = new ZipFile(jarFile);
        try {
            for (final Enumeration<? extends ZipEntry> e = zfile.entries(); e.hasMoreElements();) {
                final ZipEntry entry = e.nextElement();
                final String name = entry.getName();
                if (!name.startsWith(fullEntryName) || name.equals(fullEntryName)) {
                    continue;
                }
                final String itemname = name.substring(fullEntryName.length());
                final int slash = itemname.indexOf('/');
                if ((slash >= 0) && (slash < itemname.length() - 1)) {
                    continue;
                }
                final ResourceLink link = new JarResourceLink(jarFile, itemname);
                result.add(link);
            }
            return result;
        } finally {
            closeQuietly(zfile);
        }
    }

    private static void closeQuietly(final ZipFile zf) {
        try {
            zf.close();
        } catch (IOException ex) {
            Logger.getLogger(JarResourceLink.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean isPackage() {
        return fullEntryName.length() == 0 || fullEntryName.endsWith("/");
    }

    @Override
    public InputStream open() throws IOException {
        final ZipFile zfile = new ZipFile(jarFile);
        final ZipEntry entry = zfile.getEntry(fullEntryName);
        return zfile.getInputStream(entry);
    }

    @Override
    public String getName() {
        String fullName = fullEntryName;
        if (fullEntryName.endsWith("/")) {
            fullName = fullName.substring(0, fullEntryName.length() - 1);
        }
        final int lastSlash = fullName.lastIndexOf('/');
        return fullName.substring(lastSlash + 1, fullName.length());
    }
}