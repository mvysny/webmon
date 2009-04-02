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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.target.resource.ResourceStreamRequestTarget;
import sk.baka.webvm.analyzer.classloader.ClassLoaderUtils;
import sk.baka.webvm.analyzer.classloader.ResourceLink;

/**
 * Performs a search and posts search results.
 * @author Martin Vysny
 */
public final class SearchResults extends WebVMPage {

    public SearchResults(final String searchQuery) {
        super();
        border.add(new Label("searchQuery", searchQuery));
        final List<Serializable> results = performSearch(searchQuery);
        border.add(new ListView<Serializable>("classpathItems", results) {

            @Override
            protected void populateItem(ListItem<Serializable> item) {
                final Serializable resLink = item.getModelObject();
                item.add(new Link<Serializable>("classpathItem", item.getModel()) {

                    {
                        final Serializable model = getModelObject();
                        final String caption = model instanceof ResourceLink ? ((ResourceLink) model).getFullName() : (String) model;
                        add(new Label("linkLabel", caption));
                    }

                    @Override
                    public void onClick() {
                        if (!(resLink instanceof ResourceLink)) {
                            return;
                        }
                        final ResourceLink rl = (ResourceLink) resLink;
                        if (rl.isPackage()) {
                            return;
                        }
                        getRequestCycle().setRequestTarget(new ResourceStreamRequestTarget(Classloaders.toStream(rl), rl.getName()));
                        setRedirect(true);
                    }
                });
            }
        });
    }

    public SearchResults() {
        this("");
    }

    /**
     * Performs a search in URLs of all known class loaders.
     * @param searchQuery a substring to search for, must not be null
     * @return a mixed list of {@link ResourceLink}s (if search went OKay) and {@link String} (if exception occured in the search process).
     */
    private List<Serializable> performSearch(String searchQuery) {
        final List<Serializable> result = new ArrayList<Serializable>();
        for (ClassLoader loader = Thread.currentThread().getContextClassLoader(); loader != null; loader = loader.getParent()) {
            final URL[] urls = ClassLoaderUtils.getURLs(loader);
            for (final URL url : urls) {
                final File file = FileUtils.toFile(url);
                if (file == null) {
                    continue;
                }
                final ResourceLink root = ResourceLink.newFor(file);
                try {
                    result.addAll(root.search(searchQuery));
                } catch (Exception ex) {
                    result.add("Failed to search in " + root + ": " + ex.toString());
                }
            }
        }
        return result;
    }
}

