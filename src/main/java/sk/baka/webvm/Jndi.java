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
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.tree.LabelTree;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Prints the JNDI tree
 * @author Martin Vysny
 */
public final class Jndi extends WebPage {

    public Jndi(PageParameters params) {
        final AppBorder border = new AppBorder("appBorder");
        add(border);
        border.add(newJndiTree("jndiTree", null));
        border.add(newJndiTree("jndiJavaTree", "java:"));
    }

    private static LabelTree newJndiTree(final String treeId, final String context) {
        final LabelTree tree = new LabelTree(treeId, new LoadableDetachableModel<TreeModel>() {

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
        return tree;
    }

    private static TreeModel toModel(Exception ex) {
        final TreeNode root = new DefaultMutableTreeNode("Error listing JNDI tree: " + ex.toString());
        final TreeModel m = new DefaultTreeModel(root);
        return m;
    }

    /**
     * Lists the JNDI context and returns it as a tree.
     * @return JNDI
     */
    public static TreeModel listJNDI(final Context c) throws NamingException {
        final MutableTreeNode root = new DefaultMutableTreeNode("root");
        list(c, root, 0);
        return new DefaultTreeModel(root);
    }

    /**
     * Prints the context recursively.
     *
     * @param ctx
     *            the context contents to print.
     * @param indent
     * @param buffer
     */
    private static void list(final Context ctx, final MutableTreeNode parent, final int depth)
            throws NamingException {
        final NamingEnumeration<NameClassPair> ne = ctx.list("");
        try {
            for (; ne.hasMore();) {
                final NameClassPair pair = ne.next();
                try {
                    listUnprotected(ctx, parent, pair, depth);
                } catch (final Exception e) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Failed to examine ");
                    sb.append(pair.getName());
                    sb.append(": ");
                    sb.append(e.getClass().getName());
                    sb.append(": ");
                    sb.append(e.toString());
                    parent.insert(new DefaultMutableTreeNode(sb.toString()), parent.getChildCount());
                }
            }
        } finally {
            closeQuietly(ne);
        }
    }

    private static void listUnprotected(final Context ctx, final MutableTreeNode parent, final NameClassPair pair, final int depth)
            throws NamingException, ClassNotFoundException {
        final String name = pair.getName();
        final Class<?> clazz = loadClass(pair, ctx);
        final boolean recursive = Context.class.isAssignableFrom(clazz);
        final boolean isLinkRef = LinkRef.class.isAssignableFrom(clazz);
        final boolean isProxy = Proxy.isProxyClass(clazz) || pair.getClassName().startsWith("$Proxy");
        final StringBuilder sb = new StringBuilder();
        sb.append(name);
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
        } else {
            sb.append(" (class: " + pair.getClassName() + ")");
        }
        final MutableTreeNode node = new DefaultMutableTreeNode(sb.toString());
        parent.insert(node, parent.getChildCount());
        //sometimes there is an StackOverflow exception, we rather check it with primitive condition
        if ((recursive) && (depth < 10)) {
            final Object value = ctx.lookup(name);
            final Context subctx = (Context) value;
            list(subctx, node, depth + 1);
        }
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
            log.log(Level.FINE, "Failed to close an object", ex);
        }
    }
    private final static Logger log = Logger.getLogger(Jndi.class.getName());
}