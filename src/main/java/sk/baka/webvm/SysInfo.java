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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.misc.Producer;

/**
 * Provides list of system properties and environment properties.
 * @author Martin Vysny
 */
public final class SysInfo extends WebVMPage {

    /**
     * Creates new object
     * @param params page parameters
     */
    public SysInfo() {
        listMap(border, new Producer<Map<String, String>>() {

            public Map<String, String> produce() {
                return (Map<String, String>) (Map) System.getProperties();
            }
        }, "systemProperties", "sysPropName", "sysPropValue");
        listMap(border, new Producer<Map<String, String>>() {

            public Map<String, String> produce() {
                return System.getenv();
            }
        }, "env", "envName", "envValue");
    }

    private void listMap(final AppBorder border, final Producer<Map<String, String>> producer, final String listId, final String keyId, final String valueId) {
        final IModel<List<Map.Entry<String, String>>> model = new LoadableDetachableModel<List<Map.Entry<String, String>>>() {

            @Override
            protected List<Map.Entry<String, String>> load() {
                final Map<String, String> map = producer.produce();
                final List<Map.Entry<String, String>> result = new ArrayList<Map.Entry<String, String>>(map.entrySet());
                Collections.sort(result, new Comparator<Map.Entry<String, String>>() {

                    public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                        return o1.getKey().compareToIgnoreCase(o2.getKey());
                    }
                });
                return result;
            }
        };
        border.add(new ListView<Map.Entry<String, String>>(listId, model) {

            @Override
            protected void populateItem(ListItem<Map.Entry<String, String>> item) {
                final Map.Entry<String, String> property = item.getModelObject();
                item.add(new Label(keyId, property.getKey()));
                item.add(new Label(valueId, property.getValue()));
            }
        });
    }
}
