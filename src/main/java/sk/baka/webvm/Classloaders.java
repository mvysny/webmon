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
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.markup.html.tree.BaseTree.LinkType;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.apache.wicket.markup.html.tree.LabelTree;
import org.apache.wicket.markup.html.tree.LinkTree;
import org.apache.wicket.request.target.resource.ResourceStreamRequestTarget;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.time.Time;
import sk.baka.webvm.analyzer.classloader.CLEnum;
import sk.baka.webvm.analyzer.classloader.ClassLoaderUtils;
import sk.baka.webvm.analyzer.classloader.ResourceLink;

/**
 * Performs the class loader analysis.
 * @author Martin Vysny
 */
public final class Classloaders extends WebVMPage {

    public Classloaders() {
        tree = newClassloaderHierarchy();
        analyzeClashes(border);
        border.add(tree);
    }
    private LabelTree tree;

    private void analyzeClashes(AppBorder border) {
        final Map<URI, List<Integer>> clashes = getClashes();
        final List<URI> uris = new ArrayList<URI>(clashes.keySet());
        border.add(new ListView<URI>("clRow", uris) {

            @Override
            protected void populateItem(ListItem<URI> item) {
                final URI uri = item.getModelObject();
                item.add(new Label("clURI", uri.toString()));
                final List<Integer> clNumbers = clashes.get(uri);
                item.add(new Label("clClassLoader", clNumbers.toString()));
            }
        });
    }

    private Map<URI, List<Integer>> getClashes() {
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

    private LabelTree newClassloaderHierarchy() {
        final MutableTreeNode root = new DefaultMutableTreeNode("root");
        analyzeClassloader(root, Thread.currentThread().getContextClassLoader());
        final TreeModel model = new DefaultTreeModel(root);
        final LinkTree result = new LinkTree("classloaderHierarchy", model) {

            @Override
            protected void onNodeLinkClicked(Object node, BaseTree tree, AjaxRequestTarget target) {
                final DefaultMutableTreeNode n = (DefaultMutableTreeNode) node;
                if (!(n.getUserObject() instanceof ResourceLink)) {
                    tree.getTreeState().expandNode(node);
                    return;
                }
                final ResourceLink parent = (ResourceLink) n.getUserObject();
                if (parent.isRoot()) {
                    final File container = parent.getContainer();
                    if (container != null && container.isFile()) {
                        getRequestCycle().setRequestTarget(new ResourceStreamRequestTarget(new FileResourceStream(container), container.getName()));
                        setRedirect(true);
                        return;
                    }
                }
                if (parent.isPackage()) {
                    expandNode(node);
                    tree.getTreeState().expandNode(node);
                    return;
                }
                // download the resource
                getRequestCycle().setRequestTarget(new ResourceStreamRequestTarget(toStream(parent), parent.getName()));
                setRedirect(true);
            }
        };
        result.getTreeState().addTreeStateListener(new Listener());
        result.setRootLess(true);
        // resource download does not work with AJAX links - fix this
        result.setLinkType(LinkType.REGULAR);
        result.invalidateAll();
        return result;
    }

    private IResourceStream toStream(final ResourceLink link) {
        return new IResourceStream() {

            public String getContentType() {
                return URLConnection.getFileNameMap().getContentTypeFor(link.getName());
            }

            public long length() {
                try {
                    return link.getLength();
                } catch (IOException ex) {
                    Logger.getLogger(Classloaders.class.getName()).log(Level.WARNING, null, ex);
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

    private void expandNode(final Object node) {
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
            Collections.sort(children, new Comparator<ResourceLink>() {

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
            });
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
            expandNode(node);
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
            result.add(new DefaultMutableTreeNode("URLClassLoader.getURLs() is empty"));
            return;
        }
        for (final URL url : clUrls) {
            final File file = FileUtils.toFile(url);
            final DefaultMutableTreeNode node = (file == null) ? new DefaultMutableTreeNode(url) : new TreeNode(ResourceLink.newFor(file));
            result.add(node);
        }
    }
}
