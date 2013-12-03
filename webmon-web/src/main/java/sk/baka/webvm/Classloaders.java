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
package sk.baka.webvm;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.tree.*;
import sk.baka.webvm.analyzer.classloader.CLEnum;
import sk.baka.webvm.analyzer.classloader.ClassLoaderUtils;
import sk.baka.webvm.analyzer.classloader.ResourceLink;
import sk.baka.webvm.wicket.WicketUtils;

/**
 * Performs the class loader analysis.
 * @author Martin Vysny
 */
public final class Classloaders extends WebVMPage {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public Classloaders() {
        tree = newClassloaderHierarchy();
        analyzeClashes(border);
        border.add(tree);
    }
    private LabelTree tree;

    private void analyzeClashes(AppBorder border) {
        final Map<URI, List<Integer>> clashes = ClassLoaderUtils.getClashes();
        final List<URI> uris = new ArrayList<URI>(clashes.keySet());
        border.add(new ClassloaderClashes("clRow", uris, clashes));
    }

    private LabelTree newClassloaderHierarchy() {
        final MutableTreeNode root = new DefaultMutableTreeNode("root");
        analyzeClassloader(root, Thread.currentThread().getContextClassLoader());
        final TreeModel model = new DefaultTreeModel(root);
        final LinkTree result = new ClassloaderHierarchyTree("classloaderHierarchy", model);
        result.setRootLess(true);
        // resource download does not work with AJAX links
        // @TODO fix this - provide regular links for downloads, ajax links for anything else
        result.setLinkType(LinkType.REGULAR);
        result.invalidateAll();
        result.getTreeState().addTreeStateListener(new Listener());
        return result;
    }

    private static void expandNode(final Object node, final DefaultTreeModel tree) {
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
            final List<ResourceLink> children = parent.listAndGroup();
            Collections.sort(children, new DirAndNameComparator());
            for (final ResourceLink link : children) {
                n.add(new TreeNode(link));
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error while retrieving resources", ex);
            n.add(new DefaultMutableTreeNode("Error while retrieving resources: " + ex.toString()));
        }
        tree.nodeStructureChanged(n);
    }

    private DefaultTreeModel getModel() {
        return (DefaultTreeModel) tree.getModelObject();
    }

    private class Listener implements ITreeStateListener, Serializable {

        private static final long serialVersionUID = 1L;

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
            expandNode(node, getModel());
        }

        public void nodeSelected(Object node) {
            // do nothing
        }

        public void nodeUnselected(Object node) {
            // do nothing
        }
    }
    private static final Logger LOG = Logger.getLogger(Classloaders.class.getName());
    private static final int MAX_CLASSLOADER_NAME_LENGTH = 60;

    private static class TreeNode extends DefaultMutableTreeNode {

        private static final long serialVersionUID = 1L;

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
            result.add(new DefaultMutableTreeNode("URLClassLoader.getURLs() is empty"));
            return;
        }
        for (final URL url : clUrls) {
            final File file = toFile(url);
            final DefaultMutableTreeNode node = (file == null) ? new DefaultMutableTreeNode(url) : new TreeNode(ResourceLink.newFor(file));
            result.add(node);
        }
    }

    /**
     * Shows class/resource clashes between different classloaders.
     */
    private static class ClassloaderClashes extends ListView<URI> {

        private static final long serialVersionUID = 1L;
        private final Map<URI, List<Integer>> clashes;

        public ClassloaderClashes(String id, List<? extends URI> list, Map<URI, List<Integer>> clashes) {
            super(id, list);
            this.clashes = clashes;
        }

        @Override
        protected void populateItem(final ListItem<URI> item) {
            final URI uri = item.getModelObject();
            item.add(new Label("clURI", uri.toString()));
            final List<Integer> clNumbers = clashes.get(uri);
            item.add(new Label("clClassLoader", clNumbers.toString()));
        }
    }

    /**
     * A tree component showing the classloader hierarchy.
     */
    private static class ClassloaderHierarchyTree extends LinkTree {

        private static final long serialVersionUID = 1L;

        public ClassloaderHierarchyTree(String id, TreeModel model) {
            super(id, model);
        }

        @Override
        protected void onNodeLinkClicked(Object node, BaseTree tree, AjaxRequestTarget target) {
            final DefaultMutableTreeNode n = (DefaultMutableTreeNode) node;
            if (!(n.getUserObject() instanceof ResourceLink)) {
                tree.getTreeState().expandNode(node);
                return;
            }
            final ResourceLink parent = (ResourceLink) n.getUserObject();
            final boolean redirected = WicketUtils.redirectTo(parent);
            if (redirected) {
                return;
            }
            if (parent.isPackage()) {
                expandNode(node, (DefaultTreeModel) getModelObject());
                tree.getTreeState().expandNode(node);
                return;
            }
        }
    }

    /**
     * Sorts ResourceLinks, packages first, then by a name.
     */
    private static class DirAndNameComparator implements Comparator<ResourceLink> {

        public int compare(ResourceLink o1, ResourceLink o2) {
            if (o1.isPackage()) {
                if (!o2.isPackage()) {
                    return -1;
                }
            } else {
                if (o2.isPackage()) {
                    return 1;
                }
            }
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }
}
