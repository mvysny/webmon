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
package sk.baka.webvm.analyzer.classloader;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Contains utilities for classloader analysis.
 * @author Martin Vysny
 */
public final class ClassLoaderUtils {

    private ClassLoaderUtils() {
        throw new AssertionError();
    }
    private static final URL[] EMPTY_URL_ARRAY = new URL[0];

    /**
     * Returns URLs which given class loader claims to see. Uses {@link URLClassLoader#getURLs()}.
     * @param cl the class loader to analyze
     * @return never null, may be empty array in case the classloader is not an URLClassLoader.
     */
    public static URL[] getURLs(final ClassLoader cl) {
        if (!(cl instanceof URLClassLoader)) {
            return EMPTY_URL_ARRAY;
        }
        final URL[] result = ((URLClassLoader) cl).getURLs();
        if (result == null) {
            return EMPTY_URL_ARRAY;
        }
        return result;
    }

    /**
     * Return the class loader parent chain as a list.
     * @param cl the class loader to analyze
     * @return the chain with given class loader first and the root class loader last.
     */
    public static List<ClassLoader> getClassLoaderChain(final ClassLoader cl) {
        final List<ClassLoader> result = new ArrayList<ClassLoader>();
        for (ClassLoader c = cl; c != null; c = c.getParent()) {
            result.add(c);
        }
        return result;
    }

    /**
     * Returns a map of URIs visible to class loaders.
     * @param cl the class loader to analyze
     * @return maps URIs loadable by a class loader to a list of class loaders which are able to load the URI.
     * @throws java.net.URISyntaxException
     */
    public static Map<URI, List<ClassLoader>> getClassLoaderURIs(final ClassLoader cl) throws URISyntaxException {
        final Map<URI, List<ClassLoader>> result = new HashMap<URI, List<ClassLoader>>();
        final List<ClassLoader> cls = getClassLoaderChain(cl);
        for (final ClassLoader c : cls) {
            final URL[] urls = getURLs(c);
            for (final URL url : urls) {
                final URI uri = url.toURI();
                List<ClassLoader> list = result.get(uri);
                if (list == null) {
                    list = new ArrayList<ClassLoader>();
                    result.put(uri, list);
                }
                list.add(c);
            }
        }
        return result;
    }

    /**
     * Filters out regular class loader items, leaving only URIs loadable by multiple class loaders.
     * @param map URI - ClassLoaders map, must not be null.
     */
    public static void filterClashes(final Map<URI, List<ClassLoader>> map) {
        for (final Iterator<Map.Entry<URI, List<ClassLoader>>> i = map.entrySet().iterator(); i.hasNext();) {
            final Map.Entry<URI, List<ClassLoader>> e = i.next();
            if (e.getValue().size() <= 1) {
                i.remove();
            }
        }
    }

    public static Map<URI, List<Integer>> getClashes() {
        final List<ClassLoader> cls = ClassLoaderUtils.getClassLoaderChain(Thread.currentThread().getContextClassLoader());
        final Map<URI, List<ClassLoader>> clashes;
        try {
            clashes = ClassLoaderUtils.getClassLoaderURIs(Thread.currentThread().getContextClassLoader());
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
        ClassLoaderUtils.filterClashes(clashes);
        final Map<URI, List<Integer>> result = new HashMap<URI, List<Integer>>();
        for (final Map.Entry<URI, List<ClassLoader>> e : clashes.entrySet()) {
            final List<Integer> clNumbers = new ArrayList<Integer>();
            for (final ClassLoader cl : e.getValue()) {
                clNumbers.add(cls.indexOf(cl) + 1);
            }
            result.put(e.getKey(), clNumbers);
        }
        return result;
    }
}
