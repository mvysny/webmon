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
import java.util.List;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import sk.baka.webvm.analyzer.classloader.ClassLoaderUtils;
import sk.baka.webvm.analyzer.classloader.ResourceLink;
import sk.baka.webvm.wicket.WicketUtils;

/**
 * Performs a search and posts search results.
 * @author Martin Vysny
 */
public final class SearchResults extends WebVMPage {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the search page and performs the search.
     * @param searchQuery the query to use. Used as substring in classloader resources names search.
     */
    public SearchResults(final String searchQuery) {
        super();
        border.add(new Label("searchQuery", searchQuery));
        final List<CLResult> results = performSearch(searchQuery);
        border.add(new ResultsListView("classpathItems", results));
    }

    /**
     * Performs a search in URLs of all known class loaders.
     * @param searchQuery a substring to search for, must not be null
     * @return a mixed list of {@link ResourceLink}s (if search went OKay) and {@link String} (if exception occured in the search process).
     */
    private List<CLResult> performSearch(String searchQuery) {
        final List<CLResult> result = new ArrayList<CLResult>();
        int clIndex = 0;
        for (ClassLoader loader = Thread.currentThread().getContextClassLoader(); loader != null; loader = loader.getParent()) {
            clIndex++;
            final URL[] urls = ClassLoaderUtils.getURLs(loader);
            for (final URL url : urls) {
                final File file = toFile(url);
                if (file == null) {
                    continue;
                }
                final ResourceLink root = ResourceLink.newFor(file);
                try {
                    final List<ResourceLink> search = root.search(searchQuery);
                    // if the root file name itself matches the search query, add it to the result list
                    if (root.getContainer().getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                        search.add(0, root);
                    }
                    result.addAll(CLResult.from(search, loader, clIndex));
                } catch (Exception ex) {
                    result.add(CLResult.from(ex, loader, clIndex));
                }
            }
        }
        // sort the list
        Collections.sort(result);
        return result;
    }

    /**
     * The classloader search result item, contains either a resource link or an error message.
     */
    private static class CLResult implements Comparable<CLResult>, Serializable {

        private static final long serialVersionUID = 1L;
        /**
         * A link to the resource item.
         */
        public ResourceLink res;
        /**
         * If we were unable to obtain a resource link then this is the error message.
         */
        public String error;
        /**
         * Classloader number, 1 is the context classloader, 2 is its parent etc.
         */
        public int clIndex;

        /**
         * Converts a list of resource links.
         * @param source the source list, must not be null
         * @param cl originating class laoder.
         * @param clIndex Classloader number, 1 is the context classloader, 2 is its parent etc.
         * @return non-null converted list.
         */
        public static List<CLResult> from(final List<? extends ResourceLink> source, final ClassLoader cl, final int clIndex) {
            final List<CLResult> result = new ArrayList<CLResult>(source.size());
            for (final ResourceLink res : source) {
                final CLResult cresult = new CLResult();
                cresult.res = res;
                cresult.clIndex = clIndex;
                result.add(cresult);
            }
            return result;
        }

        /**
         * Converts a throwable.
         * @param t the throwable.
         * @param cl originating class laoder.
         * @param clIndex Classloader number, 1 is the context classloader, 2 is its parent etc.
         * @return non-null result.
         */
        public static CLResult from(final Throwable t, final ClassLoader cl, final int clIndex) {
            final CLResult result = new CLResult();
            result.error = t.toString();
            result.clIndex = clIndex;
            return result;
        }

        public int compareTo(CLResult o) {
            final String s1 = toString();
            final String s2 = o.toString();
            return s1.compareToIgnoreCase(s2);
        }

        @Override
        public String toString() {
            return res != null ? "[" + clIndex + "] " + res.getFullName() : error;
        }
    }

    /**
     * Shows search result list.
     */
    private static class ResultsListView extends ListView<CLResult> {

        public ResultsListView(String id, List<? extends CLResult> list) {
            super(id, list);
        }
        private static final long serialVersionUID = 1L;

        @Override
        protected void populateItem(ListItem<CLResult> item) {
            final CLResult resLink = item.getModelObject();
            item.add(new DownloadableResultLink("classpathItem", item.getModel(), resLink));
        }

        private static class DownloadableResultLink extends Link<CLResult> {

            private final CLResult resLink;

            public DownloadableResultLink(String id, IModel<CLResult> model, CLResult resLink) {
                super(id, model);
                this.resLink = resLink;
            }
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                final CLResult model = getModelObject();
                final String caption = model.toString();
                replaceComponentTagBody(markupStream, openTag, caption);
            }

            @Override
            public void onClick() {
                if (resLink.res == null) {
                    return;
                }
                WicketUtils.redirectTo(resLink.res);
            }
        }
    }
}
