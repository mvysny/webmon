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
package sk.baka.webvm;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.tree.LabelTree;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Performs the class loader analysis.
 * @author Martin Vysny
 */
public final class Classloaders extends WebPage {

    public Classloaders(PageParameters params) {
        final AppBorder border = new AppBorder("appBorder");
        add(border);
        border.add(newClassloaderHierarchy());
    }

    private static LabelTree newClassloaderHierarchy() {
        final LabelTree tree = new LabelTree("classloaderHierarchy", new LoadableDetachableModel<TreeModel>() {

            @Override
            protected TreeModel load() {
                final MutableTreeNode root = new DefaultMutableTreeNode("root");
                analyzeClassloader(root, Thread.currentThread().getContextClassLoader());
                return new DefaultTreeModel(root);
            }
        });
        tree.getTreeState().expandAll();
        tree.setRootLess(true);
        return tree;
    }
    private static final int MAX_CLASSLOADER_NAME_LENGTH = 60;

    private static void analyzeClassloader(final MutableTreeNode root, final ClassLoader cl) {
        ClassLoader current = cl;
        int clNumber = 1;
        while (current != null) {
            final DefaultMutableTreeNode result = new DefaultMutableTreeNode();
            String name = current.toString();
            if (name.length() > MAX_CLASSLOADER_NAME_LENGTH) {
                name = name.substring(0, MAX_CLASSLOADER_NAME_LENGTH) + "...";
            }
            result.setUserObject("[" + (clNumber++) + "] " + CLEnum.getTypes(current) + " " + current.getClass().getName() + ": " + name);
            if (current instanceof URLClassLoader) {
                addClassLoaderURLs(result, (URLClassLoader) current);
            }
            root.insert(result, root.getChildCount());
            current = current.getParent();
        }
    }

    private static enum CLEnum {

        WAR {

            @Override
            protected boolean matches(final ClassLoader cl) {
                return cl.getResource("WEB-INF/web.xml") != null;
            }
        },
        EJBJAR {

            @Override
            protected boolean matches(final ClassLoader cl) {
                return cl.getResource("META-INF/ejb-jar.xml") != null;
            }
        },
        EAR {

            protected boolean matches(final ClassLoader cl) {
                return cl.getResource("META-INF/application.xml") != null;
            }
        },
        SYSTEM {

            protected boolean matches(final ClassLoader cl) {
                return cl == ClassLoader.getSystemClassLoader();
            }
        },
        ROOT {

            protected boolean matches(final ClassLoader cl) {
                return cl.getParent() == null;
            }
        };

        protected abstract boolean matches(final ClassLoader cl);

        /**
         * Detects the type of classes this class loader manages.
         * @param cl the classloader, not null.
         * @return class loader type
         */
        public static EnumSet<CLEnum> getTypes(final ClassLoader cl) {
            final EnumSet<CLEnum> result = EnumSet.noneOf(CLEnum.class);
            for (final CLEnum e : values()) {
                if (e.matches(cl)) {
                    result.add(e);
                }
            }
            return result;
        }
    }

    private static void addClassLoaderURLs(final DefaultMutableTreeNode result, final URLClassLoader cl) {
        final URL[] clUrls = cl.getURLs();
        if (clUrls == null || clUrls.length == 0) {
            result.add(new DefaultMutableTreeNode("ClassLoader search URL path is empty"));
            return;
        }
        for (final URL url : clUrls) {
            final DefaultMutableTreeNode node = new DefaultMutableTreeNode();
            final File file = FileUtils.toFile(url);
            if (file == null) {
                node.setUserObject(url);
                continue;
            }
            node.setUserObject(file.getAbsolutePath());
            result.add(node);
            enumerateResources(node, file);
        }
    }

    private static void enumerateResources(DefaultMutableTreeNode node, File file) {

        if (file.isDirectory()) {
            @SuppressWarnings("unchecked")
            final Collection<File> files = FileUtils.listFiles(file, null, true);
            for (final File f : files) {
                final File absFile = f.getAbsoluteFile();
                assert absFile.getAbsolutePath().startsWith(
                        file.getAbsolutePath());
                final String classFilename = absFile.getAbsolutePath().substring(file.getAbsolutePath().length());
                final String classname = classNameFromFileName(classFilename);
                result.add(classname);
            }
            return result;
        }
        final ZipFile zfile = new ZipFile(file);
        for (final Enumeration<? extends ZipEntry> e = zfile.entries(); e.hasMoreElements();) {
            final ZipEntry entry = e.nextElement();
            if (!entry.getName().endsWith(".class")) {
                // skip
                continue;
            }
            final String classname = classNameFromFileName(entry.getName());
            result.add(classname);
        }
        zfile.close();
        return result;
    }

    /**
     * Lists items in given package of given class path item (jar or a directory).
     * @param pkg the package name with "/" as separators. Must not start with /
     * @param cpItem the jar/directory
     */
    private static List<Model> listCPItem(final String pkg, final File cpItem) throws ZipException, IOException {
        if (cpItem.isDirectory()) {
            return Model.fromFiles(new File(cpItem, pkg).listFiles(), cpItem);
        } else {
            return listZip(pkg, cpItem);
        }
    }

    private static List<Model> listZip(String pkg, File cpItem) throws ZipException, IOException {
        final List<Model> result = new ArrayList<Model>();
        final ZipFile zfile = new ZipFile(cpItem);
        final String prefix = pkg + "/";
        for (final Enumeration<? extends ZipEntry> e = zfile.entries(); e.hasMoreElements();) {
            final ZipEntry entry = e.nextElement();
            final String name = entry.getName();
            if (!name.startsWith(prefix) || name.equals(prefix)) {
                continue;
            }
            final String itemname = name.substring(prefix.length());
            final Model m = new Model(entry.isDirectory(), itemname, cpItem);
            result.add(m);
        }
        return result;
    }

    private static class Model implements Serializable {

        public Model(final boolean isFolder, final String name, final File cpItem) {
            this.isFolder = isFolder;
            this.name = name;
            this.cpItem = cpItem;
        }

        public static Model fromFile(final File f, final File cpItem) {
            return new Model(f.isDirectory(), f.getName(), cpItem);
        }

        public static List<Model> fromFiles(final File[] f, final File cpItem) {
            final List<Model> result = new ArrayList<Model>(f.length);
            for (final File file : f) {
                result.add(fromFile(file, cpItem));
            }
            return result;
        }
        public final boolean isFolder;
        public final String name;
        public final File cpItem;

        @Override
        public String toString() {
            return name;
        }
    }
}
