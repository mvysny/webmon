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

import org.apache.wicket.markup.html.basic.Label;

/**
 * Performs a search and posts search results.
 * @author Martin Vysny
 */
public final class SearchResults extends WebVMPage {

    public SearchResults(final String searchQuery) {
        super();
        border.add(new Label("searchQuery", searchQuery));
    }

    public SearchResults() {
        this("");
    }
}
