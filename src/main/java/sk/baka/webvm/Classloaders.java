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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import org.apache.commons.io.IOUtils;
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
                final MutableTreeNode root = analyzeClassloader(1, Thread.currentThread().getContextClassLoader());
                return new DefaultTreeModel(root);
            }
        });
        tree.getTreeState().expandAll();
        return tree;
    }
    private static final int MAX_CLASSLOADER_NAME_LENGTH = 40;

    private static MutableTreeNode analyzeClassloader(final int clNumber, final ClassLoader cl) {
        final DefaultMutableTreeNode result = new DefaultMutableTreeNode();
        String name = cl.toString();
        if (name.length() > MAX_CLASSLOADER_NAME_LENGTH) {
            name = name.substring(0, MAX_CLASSLOADER_NAME_LENGTH);
        }
        result.setUserObject("[" + clNumber + " " + CLEnum.getType(cl) + " ]: " + name);
        final ClassLoader parent = cl.getParent();
        if (parent != null) {
            result.add(analyzeClassloader(clNumber + 1, parent));
        }
        return result;
    }

    private static enum CLEnum {

        WAR, EJBJAR, EAR, SYSTEM, AS;

        /**
         * Detects the type of classes this class loader manages.
         * @param cl the classloader, not null.
         * @return class loader type
         */
        public static CLEnum getType(final ClassLoader cl) {
            if (cl == ClassLoader.getSystemClassLoader()) {
                return SYSTEM;
            }
            if (cl.getResource("WEB-INF/web.xml") != null) {
                try {
                    System.out.println(IOUtils.toString(cl.getResource("WEB-INF/web.xml").openStream()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                return WAR;
            }
            if (cl.getResource("META-INF/ejb-jar.xml") != null) {
                return EJBJAR;
            }
            if (cl.getResource("META-INF/application.xml") != null) {
                return EAR;
            }
            return AS;
        }
    }
}

