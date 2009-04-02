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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
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

                    @Override
                    protected void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                        final Serializable model = getModelObject();
                        final String caption = SearchResults.toString(model);
                        replaceComponentTagBody(markupStream, openTag, caption);
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

    private static String toString(final Serializable s) {
        return s instanceof ResourceLink ? ((ResourceLink) s).getFullName() : (String) s;
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
        // sort the list
        Collections.sort(result, new Comparator<Serializable>() {

            public int compare(Serializable o1, Serializable o2) {
                final String s1 = SearchResults.toString(o1);
                final String s2 = SearchResults.toString(o2);
                return s1.compareToIgnoreCase(s2);
            }
        });
        return result;
    }
}

