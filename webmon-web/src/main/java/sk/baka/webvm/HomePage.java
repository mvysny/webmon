/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebMon is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebMon.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm;

import com.google.inject.Provider;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.analyzer.hostos.JavaEEServer;

/**
 * Homepage
 * @author Martin Vysny
 */
public class HomePage extends WebVMPage {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor that is invoked when page is invoked without a session.
     */
    public HomePage() {
        // system info
        border.add(new Label("os", System.getProperty("os.name") + " " + System.getProperty("os.version")));
        border.add(new Label("hw", System.getProperty("os.arch") + "; CPU#: " + Runtime.getRuntime().availableProcessors()));
        border.add(new Label("java", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + " by " + System.getProperty("java.vm.vendor")));
        border.add(new Label("netInterfaces", getAllInterfaces()));
        final JavaEEServer server = JavaEEServer.getRuntimeNull();
        // java properties
        listMap(border, new SystemPropertiesProducer(), "systemProperties", "sysPropName", "sysPropValue");
        // environment properties
        listMap(border, new EnvPropertiesProducer(), "env", "envName", "envValue");
        final Link<Void> link = new Link<Void>("asLink") {

            @Override
            public void onClick() {
                if (JavaEEServer.getRuntimeNull() != null) {
                    setResponsePage(new AppServer(JavaEEServer.getRuntimeNull()));
                }
            }
        };
        link.add(new Label("as", server == null ? "Unknown" : server.getServerName()));
        border.add(link);
    }

    private static String getAllInterfaces() {
        try {
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (final NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (first) {
                    first = false;
                } else {
                    sb.append("; ");
                }
                sb.append(iface.getName()).append(": ");
                for (final InetAddress ia : Collections.list(iface.getInetAddresses())) {
                    if (ia instanceof Inet4Address) {
                        sb.append(getIP(ia));
                    }
                }
            }
            return sb.toString();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to enumerate interfaces", ex);
            return "Failed to enumerate interfaces: " + ex;
        }
    }
    private final static Logger LOG = Logger.getLogger(HomePage.class.getName());

    /**
     * Returns formatted IP address.
     * @param adr the address object
     * @return the formatted IP address.
     */
    public static String getIP(InetAddress adr) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (byte b : adr.getAddress()) {
            if (first) {
                first = false;
            } else {
                sb.append('.');
            }
            int num = b;
            if (num < 0) {
                num += 256;
            }
            sb.append(num);
        }
        return sb.toString();
    }

    /**
     * Uses {@link ListView} to list contents of given map.
     * @param border the application border instance.
     * @param producer produces the map
     * @param listId connect the ListView to this wicket ID
     * @param keyId this wicket ID will display the map key
     * @param valueId this wicket ID will display the map value
     */
    private static void listMap(final AppBorder border, final Provider<Map<String, String>> producer, final String listId, final String keyId, final String valueId) {
        final IModel<List<Map.Entry<String, String>>> model = new LoadableDetachableModel<List<Map.Entry<String, String>>>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<Map.Entry<String, String>> load() {
                final Map<String, String> map = producer.get();
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

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<Map.Entry<String, String>> item) {
                final Map.Entry<String, String> property = item.getModelObject();
                item.add(new Label(keyId, property.getKey()));
                item.add(new Label(valueId, property.getValue()));
            }
        });
    }

    public static class SystemPropertiesProducer implements Provider<Map<String, String>>, Serializable {

        private static final long serialVersionUID = 1L;

        @SuppressWarnings(value = "unchecked")
        public Map<String, String> get() {
            return (Map<String, String>) (Map) System.getProperties();
        }
    }

    public static class EnvPropertiesProducer implements Provider<Map<String, String>>, Serializable {

        private static final long serialVersionUID = 1L;

        public Map<String, String> get() {
            return System.getenv();
        }
    }
}
