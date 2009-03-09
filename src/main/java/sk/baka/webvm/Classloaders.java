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
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.apache.wicket.markup.html.tree.LabelTree;
import sk.baka.webvm.analyzer.classloader.CLEnum;
import sk.baka.webvm.analyzer.classloader.ResourceLink;

/**
 * Performs the class loader analysis.
 * @author Martin Vysny
 */
public final class Classloaders extends WebPage {

    public Classloaders(PageParameters params) {
        final AppBorder border = new AppBorder("appBorder");
        add(border);
        tree = newClassloaderHierarchy();
        border.add(tree);
    }
    private LabelTree tree;

    private LabelTree newClassloaderHierarchy() {
        final MutableTreeNode root = new DefaultMutableTreeNode("root");
        analyzeClassloader(root, Thread.currentThread().getContextClassLoader());
        final TreeModel model = new DefaultTreeModel(root);
        final LabelTree result = new LabelTree("classloaderHierarchy", model);
        result.getTreeState().addTreeStateListener(new Listener());
        result.setRootLess(true);
        return result;
    }

    private DefaultTreeModel getModel() {
        return (DefaultTreeModel) tree.getModelObject();
    }

    private class Listener implements ITreeStateListener, Serializable {

        public void allNodesCollapsed() {
            // do nothing
        }

        public void allNodesExpanded() {
            // do nothing
        }

        public void nodeCollapsed(Object node) {
            final DefaultMutableTreeNode n = (DefaultMutableTreeNode) node;
            if (!(n.getUserObject() instanceof ResourceLink)) {
                return;
            }
            // cleanup children
            n.removeAllChildren();
            final DefaultTreeModel model = getModel();
            model.nodeStructureChanged(n);
        }

        public void nodeExpanded(Object node) {
            final DefaultMutableTreeNode n = (DefaultMutableTreeNode) node;
            if (!(n.getUserObject() instanceof ResourceLink)) {
                return;
            }
            final ResourceLink parent = (ResourceLink) n.getUserObject();
            if (!parent.isPackage()) {
                return;
            }
            n.removeAllChildren();
            try {
                final List<ResourceLink> children = parent.list();
                for (final ResourceLink link : children) {
                    n.add(new TreeNode(link));
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, "Error while retrieving resources", ex);
                n.add(new DefaultMutableTreeNode("Error while retrieving resources: " + ex.toString()));
            }
            final DefaultTreeModel model = getModel();
            model.nodeStructureChanged(n);
        }

        public void nodeSelected(Object node) {
            // do nothing
        }

        public void nodeUnselected(Object node) {
            // do nothing
        }
    }
    private static final Logger log = Logger.getLogger(Classloaders.class.getName());
    private static final int MAX_CLASSLOADER_NAME_LENGTH = 60;

    private static class TreeNode extends DefaultMutableTreeNode {

        public TreeNode(final ResourceLink link) {
            super(link);
        }

        @Override
        public boolean isLeaf() {
            return !((ResourceLink) getUserObject()).isPackage();
        }
    }

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

    private static void addClassLoaderURLs(final DefaultMutableTreeNode result, final URLClassLoader cl) {
        final URL[] clUrls = cl.getURLs();
        if (clUrls == null || clUrls.length == 0) {
            result.add(new DefaultMutableTreeNode("ClassLoader search URL path is empty"));
            return;
        }
        for (final URL url : clUrls) {
            final File file = FileUtils.toFile(url);
            final DefaultMutableTreeNode node = (file == null) ? new DefaultMutableTreeNode(url) : new TreeNode(ResourceLink.newFor(file));
            result.add(node);
        }
    }
}
