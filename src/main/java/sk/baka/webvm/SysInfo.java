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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Provides list of system properties and environment properties.
 * @author Martin Vysny
 */
public final class SysInfo extends WebPage {

    /**
     * Creates new object
     * @param params page parameters
     */
    public SysInfo(PageParameters params) {
        final AppBorder border = new AppBorder("appBorder");
        add(border);
        listMap(border, (Map<String, String>) (Map) System.getProperties(), "systemProperties", "sysPropName", "sysPropValue");
        listMap(border, System.getenv(), "env", "envName", "envValue");
    }

    private void listMap(final AppBorder border, final Map<String, String> map, final String listId, final String keyId, final String valueId) {
        final Map<String, String> clonedMap = new HashMap<String, String>(map);
        final List<String> keys = new ArrayList<String>(clonedMap.keySet());
        Collections.sort(keys);
        final IModel<List<String>> model = new LoadableDetachableModel<List<String>>() {

            @Override
            protected List<String> load() {
                return keys;
            }
        };
        border.add(new ListView<String>(listId, model) {

            @Override
            protected void populateItem(ListItem<String> item) {
                final String propertyName = item.getModelObject();
                item.add(new Label(keyId, propertyName));
                item.add(new Label(valueId, clonedMap.get(propertyName)));
            }
        });
    }
}

