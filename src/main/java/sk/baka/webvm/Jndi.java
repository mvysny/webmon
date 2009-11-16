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

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import org.apache.wicket.markup.html.tree.LabelTree;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Shows the JNDI tree.
 * @author Martin Vysny
 */
public final class Jndi extends WebVMPage {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public Jndi() {
        border.add(newJndiTree("jndiTree", null));
        border.add(newJndiTree("jndiJavaTree", "java:comp"));
    }

    private static LabelTree newJndiTree(final String treeId, final String context) {
        final LabelTree tree = new LabelTree(treeId, new LoadableDetachableModel<TreeModel>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected TreeModel load() {
                try {
                    Context c = new InitialContext();
                    if (context != null) {
                        c = (Context) c.lookup(context);
                    }
                    return listJNDI(c);
                } catch (Exception ex) {
                    return toModel(ex);
                }
            }
        });
        tree.getTreeState().expandAll();
        tree.setRootLess(true);
        return tree;
    }

    private static TreeModel toModel(Exception ex) {
        final TreeNode root = new DefaultMutableTreeNode("Error listing JNDI tree: " + ex.toString());
        return new DefaultTreeModel(root);
    }

    /**
     * Lists the JNDI context and returns it as a tree.
     * @param c the context to list
     * @return JNDI pretty-printed JNDI tree
     */
    public static TreeModel listJNDI(final Context c) {
        final MutableTreeNode root = new DefaultMutableTreeNode("root");
        list(c, root, 0);
        return new DefaultTreeModel(root);
    }

    /**
     * Prints the context recursively.
     *
     * @param ctx
     *            the context contents to print.
     * @param parent print children of this parent.
     * @param depth current depth.
     */
    private static void list(final Context ctx, final MutableTreeNode parent, final int depth) {
        try {
            final NamingEnumeration<NameClassPair> ne = ctx.list("");
            try {
                // sort the JNDI listing first
                final List<NameClassPair> children = Collections.list(ne);
                Collections.sort(children, new Comparator<NameClassPair>() {

                    public int compare(NameClassPair o1, NameClassPair o2) {
                        return o1.getName().compareToIgnoreCase(o2.getName());
                    }
                });
                for (final NameClassPair pair : children) {
                    try {
                        listUnprotected(ctx, parent, pair, depth);
                    } catch (final Exception e) {
                        LOG.log(Level.SEVERE, "JNDI examination error", e);
                        final StringBuilder sb = new StringBuilder();
                        sb.append(pair.getName());
                        sb.append(": Failed to examine: ");
                        sb.append(e.toString());
                        addWarningNode(parent, sb.toString());
                    }
                }
            } finally {
                closeQuietly(ne);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "JNDI examination error", ex);
            String name = "[unknown]";
            try {
                name = ctx.getNameInNamespace();
            } catch (Exception e) {
                // do nothing
            }
            addWarningNode(parent, name + ": Failed to examine: " + ex.toString());
        }
    }
    /**
     * Enumerates the JNDI tree up to this depth.
     */
    private static final int MAX_DEPTH = 5;

    private static void listUnprotected(final Context ctx, final MutableTreeNode parent, final NameClassPair pair, final int depth)
            throws NamingException, ClassNotFoundException {
        final JndiTreeNode node = new JndiTreeNode(ctx, pair);
        if (node.isContext) {
            //sometimes there is an StackOverflow exception, we rather check it with primitive condition
            if (depth < MAX_DEPTH) {
                final Object value = ctx.lookup(pair.getName());
                final Context subctx = (Context) value;
                list(subctx, node, depth + 1);
            } else {
                addWarningNode(node, "Maximum depth of " + MAX_DEPTH + " reached");
            }
        }
        parent.insert(node, parent.getChildCount());
    }

    private static void addWarningNode(final MutableTreeNode node,
            final String text) {
        node.insert(new DefaultMutableTreeNode(text), node.getChildCount());
    }

    /**
     * Closes given object quietly. Any exceptions are logged using debug level.
     *
     * @param c
     *            the object to close.
     */
    public static void closeQuietly(final NamingEnumeration<?> c) {
        try {
            c.close();
        } catch (final Exception ex) {
            LOG.log(Level.FINE, "Failed to close an object", ex);
        }
    }
    private static final Logger LOG = Logger.getLogger(Jndi.class.getName());

    /**
     * A model object which constructs itself from given JNDI information.
     */
    private static class JndiTreeNode extends DefaultMutableTreeNode {

        private static final long serialVersionUID = 1L;
        /**
         * true if given nameclass pair is a context which may contain other pairs.
         */
        private boolean isContext = false;
        /**
         * true if given nameclass pair is a reference object.
         */
        private boolean isLinkRef = false;
        /**
         * true if given nameclass pair is a proxy object.
         */
        private boolean isProxy = false;
        /**
         * true if given nameclass pair class was not found.
         */
        private Throwable classLoadFailure = null;

        /**
         * Creates new node model object from given JNDI pair.
         * @param ctx the context instance
         * @param pair the name class pair
         * @throws javax.naming.NamingException
         */
        public JndiTreeNode(final Context ctx, final NameClassPair pair) throws NamingException {
            super();
            final StringBuilder sb = new StringBuilder();
            final String name = pair.getName();
            sb.append(name);
            Class<?> clazz = null;
            try {
                clazz = loadClass(pair, ctx);
                isContext = Context.class.isAssignableFrom(clazz);
                isLinkRef = LinkRef.class.isAssignableFrom(clazz);
                isProxy = Proxy.isProxyClass(clazz) || pair.getClassName().startsWith("$Proxy");
            } catch (ClassNotFoundException ex) {
                classLoadFailure = ex;
            }
            // Display reference targets
            if (isLinkRef) {
                final Object obj = ctx.lookupLink(name);
                final LinkRef link = (LinkRef) obj;
                sb.append("[link -> ");
                sb.append(link.getLinkName());
                sb.append(']');
            }
            // Display proxy interfaces
            if (isProxy) {
                sb.append(" (proxy: " + pair.getClassName());
                final Class<?>[] ifaces = clazz.getInterfaces();
                sb.append(" implements ");
                for (int i = 0; i < ifaces.length; i++) {
                    sb.append(ifaces[i]);
                    sb.append(',');
                }
                sb.setCharAt(sb.length() - 1, ')');
            } else if (isContext) {
                sb.append(" (Context)");
            } else {
                sb.append(" (class: ");
                sb.append(pair.getClassName());
                sb.append(")");
            }
            if (classLoadFailure != null) {
                sb.append(" - failed to load class: ");
                sb.append(classLoadFailure.toString());
            } else {
                if (String.class.isAssignableFrom(clazz)) {
                    sb.append(": ");
                    final String str = (String) ctx.lookup(name);
                    if (str == null) {
                        sb.append("null");
                    } else {
                        sb.append("\"");
                        sb.append(str);
                        sb.append("\"");
                    }
                }
            }
            setUserObject(sb.toString());
        }

        /**
         * Try to get the class from given JNDI pair.
         *
         * @param pair
         *            the pair
         * @param ctx
         *            the context
         * @return non-<code>null</code> class instance.
         * @throws ClassNotFoundException
         * @throws NamingException
         */
        private static Class<?> loadClass(final NameClassPair pair,
                final Context ctx) throws ClassNotFoundException, NamingException {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try {
                return loader.loadClass(pair.getClassName());
            } catch (ClassNotFoundException e) {
                if (!pair.getClassName().startsWith("$Proxy")) {
                    throw e;
                }
            // try some other methods of obtaining the class.
            }
            // We have to get the class from the binding
            final Object p = ctx.lookup(pair.getName());
            return p.getClass();
        }

        @Override
        public boolean isLeaf() {
            return !isContext;
        }
    }
}