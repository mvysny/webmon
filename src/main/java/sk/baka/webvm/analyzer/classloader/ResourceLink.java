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
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
            return new DirResourceLink(file, true);
        } else {
            return new JarResourceLink(file, "", true);
        }
    }

    /**
     * Returns a list of names from given list of links
     * @param links not null
     * @return never null
     */
    public static List<String> getNames(final List<ResourceLink> links) {
        final List<String> result = new ArrayList<String>(links.size());
        for (final ResourceLink link : links) {
            result.add(link.getName());
        }
        return result;
    }

    /**
     * Finds first link with given name.
     * @param links a list of links, must not be null
     * @param name the name of the resource.
     * @return the link with given name, never null
     * @throws RuntimeException if no such link exists
     */
    public static ResourceLink findFirstByName(final List<ResourceLink> links, final String name) {
        for (final ResourceLink link : links) {
            if (link.getName().equals(name)) {
                return link;
            }
        }
        throw new RuntimeException("No such link: " + name);
    }

    /**
     * Returns length of underlying resource.
     * @return the length or -1 if not known or invoked on a package.
     */
    public abstract long getLength() throws IOException;

    /**
     * Lists all direct child packages and items of this package. It is invalid to call this method on a non-package resource. Groups single-package-child
     * names together.
     * @return list of all children.
     */
    public final List<ResourceLink> listAndGroup() throws IOException {
        final List<ResourceLink> result = list();
        for (int i = 0; i < result.size(); i++) {
            result.set(i, groupIfNecessary(result.get(i)));
        }
        return result;
    }

    private ResourceLink groupIfNecessary(final ResourceLink child) throws IOException {
        if (!child.isPackage()) {
            return child;
        }
        ResourceLink singlePackage = child;
        ResourceLink prevPackage = null;
        final StringBuilder sb = new StringBuilder(child.getName());
        do {
            final List<ResourceLink> children = singlePackage.list();
            prevPackage = singlePackage;
            singlePackage = getSinglePackage(children);
            if (singlePackage != null) {
                sb.append('.');
                sb.append(singlePackage.getName());
            }
        } while (singlePackage != null);
        if (prevPackage == child) {
            return child;
        }
        return new ResourceLinkGroup(sb.toString(), prevPackage);
    }

    private static ResourceLink getSinglePackage(final Collection<? extends ResourceLink> contents) {
        if (contents.size() != 1) {
            return null;
        }
        final ResourceLink link = contents.iterator().next();
        if (!link.isPackage()) {
            return null;
        }
        return link;
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
     * Returns the package/resource name. Does not include names of the parent packages nor any slash characters. Name of the root package is a full directory/jar file name.
     * @return the name of the resource denoted by this link.
     */
    public abstract String getName();

    /**
     * Checks if this resource link denotes a root of a jar/directory.
     * @return true if this is a jar/directory root, false otherwise.
     */
    public abstract boolean isRoot();

    /**
     * Returns a file link to the resource container containing these links. Required to be valid only for {@link #isRoot() root} links.
     * @return file denoting the container or null.
     */
    public abstract File getContainer();

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
    private final boolean isRoot;

    DirResourceLink(final File file, final boolean isRoot) {
        this.file = file;
        this.isRoot = isRoot;
    }

    @Override
    public List<ResourceLink> list() {
        final File[] children = file.listFiles();
        final List<ResourceLink> result = new ArrayList<ResourceLink>(children.length);
        for (final File child : children) {
            result.add(new DirResourceLink(child, false));
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
        return isRoot ? file.getAbsolutePath() : file.getName();
    }

    @Override
    public long getLength() {
        return file.length();
    }

    @Override
    public boolean isRoot() {
        return isRoot;
    }

    @Override
    public File getContainer() {
        return isRoot ? file : null;
    }
}

/**
 * Provides package information for an on-disk jar file.
 * @author Martin Vysny
 */
final class JarResourceLink extends ResourceLink {

    private final File jarFile;
    private final String fullEntryName;
    private final boolean isRoot;

    JarResourceLink(final File jarFile, final String fullEntryName, final boolean isRoot) {
        this.jarFile = jarFile;
        this.fullEntryName = fullEntryName;
        this.isRoot = isRoot;
    }

    @Override
    public List<ResourceLink> list() throws IOException {
        if (!isPackage()) {
            throw new IllegalStateException("not a package");
        }
        final List<ResourceLink> result = new ArrayList<ResourceLink>();
        final ZipFile zfile = new ZipFile(jarFile);
        try {
            final Set<String> resultNames = new HashSet<String>();
            for (final Enumeration<? extends ZipEntry> e = zfile.entries(); e.hasMoreElements();) {
                final ZipEntry entry = e.nextElement();
                final String name = entry.getName();
                if (!name.startsWith(fullEntryName) || name.equals(fullEntryName)) {
                    continue;
                }
                String itemname = name.substring(fullEntryName.length());
                final int slash = itemname.indexOf('/');
                if (slash >= 0) {
                    itemname = itemname.substring(0, slash + 1);
                }
                if (resultNames.add(itemname)) {
                    final ResourceLink link = new JarResourceLink(jarFile, fullEntryName + itemname, false);
                    result.add(link);
                }
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
        if (isRoot) {
            return jarFile.getAbsolutePath();
        }
        String fullName = fullEntryName;
        if (fullEntryName.endsWith("/")) {
            fullName = fullName.substring(0, fullEntryName.length() - 1);
        }
        final int lastSlash = fullName.lastIndexOf('/');
        return fullName.substring(lastSlash + 1, fullName.length());
    }

    @Override
    public long getLength() throws IOException {
        if (isPackage()) {
            return -1;
        }
        final ZipFile zfile = new ZipFile(jarFile);
        final ZipEntry entry = zfile.getEntry(fullEntryName);
        if (entry == null) {
            throw new IOException("No such entry: " + fullEntryName);
        }
        return entry.getSize();
    }

    @Override
    public boolean isRoot() {
        return isRoot;
    }

    @Override
    public File getContainer() {
        return jarFile;
    }

    @Override
    public String toString() {
        if (isRoot()) {
            return getName() + " [" + (jarFile.length() / 1024) + "K]";
        }
        return super.toString();
    }
}

/**
 * A delegate for a real resource link. Serves for multiple package grouping. Always a package.
 * @author Martin Vysny
 */
final class ResourceLinkGroup extends ResourceLink {

    private final String name;
    private final ResourceLink delegate;

    public ResourceLinkGroup(final String name, final ResourceLink delegate) {
        this.name = name;
        this.delegate = delegate;

    }

    @Override
    public List<ResourceLink> list() throws IOException {
        return delegate.list();
    }

    @Override
    public boolean isPackage() {
        return true;
    }

    @Override
    public InputStream open() throws IOException {
        throw new IOException("Not a file");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getLength() {
        return -1;
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public File getContainer() {
        return delegate.getContainer();
    }
}