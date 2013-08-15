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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import org.apache.wicket.markup.html.basic.Label;
import sk.baka.webvm.analyzer.hostos.JavaEEServer;

/**
 * Shows an information about an application server: the connection strings, Hibernate configuration etc.
 * @author Martin Vysny
 */
public class AppServer extends WebVMPage {

    public AppServer(final JavaEEServer server) {
        super();
        border.add(new Label("appServerName", server.getServerName()));
        border.add(new Label("sampleCodeRemoteEjb", getSampleCodeRemoteEjb(server)));
        border.add(new Label("userTransactionJndi", nullable(server.getUserTransactionJndi())));
        String tm = null;
        try {
            tm = nullable(server.getTransactionManagerJndi());
        } catch (Exception e) {
            tm = e.toString();
        }
        border.add(new Label("transactionManagerJndi", tm));
        border.add(new Label("managerLookupClass", nullable(server.getHibernateTransactionManagerFactory())));
    }

    private static String nullable(final String str) {
        return str == null ? "unknown" : str;
    }

    private String getSampleCodeRemoteEjb(final JavaEEServer server) {
        final StringBuilder sb = new StringBuilder();
        sb.append("final Properties p = new Properties();\n");
        final Properties p = server.getJNDIProperties(false);
        p.put(Context.PROVIDER_URL, server.getRemoteURL(getMyIp(), null));
        for (Object key : p.keySet()) {
            sb.append("p.put(\"");
            sb.append(key);
            sb.append("\", \"");
            sb.append(p.get(key));
            sb.append("\");\n");
        }
        sb.append("final Context c = new InitialContext(p);");
        return sb.toString();
    }

    /**
     * Retrieves an IP address of this machine. Returns localhost if the address couldn't be retrieved.
     * @return this machine IP address.
     */
    private static String getMyIp() {
        try {
            for (final NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (final InetAddress ia : Collections.list(iface.getInetAddresses())) {
                    // searching for IPV4 non-localhost address
                    if (ia instanceof Inet4Address && ia.getAddress()[0] != 127) {
                        return HomePage.getIP(ia);
                    }
                }
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, "Failed to detect IP address of this machine", ex);
        }
        return "127.0.0.1";
    }
    private final static Logger log = Logger.getLogger(AppServer.class.getName());
}
