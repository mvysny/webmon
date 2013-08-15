/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * WebMon is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * WebMon. If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.analyzer.hostos;

import java.util.Properties;
import javax.naming.Context;

/**
 * Enumerates JavaEE servers and provides several information such as remote EJB endpoint name, transaction manager location etc.
 * Provides running JavaEE application server auto-detection mechanism.
 * @author Martin Vysny
 */
public enum JavaEEServer {

    /**
     * The JBoss server.
     */
    JBoss {

        @Override
        public String getRemoteURL(final String host, final Integer port) {
            return host + ":" + (port == null ? "1099" : port);
        }

        @Override
        protected boolean isRunningOn() {
            return existsClass("org.jboss.management.j2ee.factory.ManagedObjectFactory");
        }

        @Override
        public String getHibernateTransactionManagerFactory() {
            return "org.hibernate.transaction.JBossTransactionManagerLookup";
        }

        @Override
        public String getServerName() {
            return "JBoss";
        }

        @Override
        public Properties getJNDIProperties(final boolean local) {
            if (local) {
                return null;
            }
            final Properties result = new Properties();
            result.put(Context.INITIAL_CONTEXT_FACTORY,
                    "org.jnp.interfaces.NamingContextFactory");
            result.put(Context.URL_PKG_PREFIXES, "org.jboss.naming");
            return result;
        }

        @Override
        public String getTransactionManagerJndi() {
            return "java:/TransactionManager";
        }

        @Override
        public String getUserTransactionJndi() {
            return "UserTransaction";
        }
    },
    /**
     * WebSphere.
     */
    WebSphere {

        @Override
        public String getRemoteURL(final String host, final Integer port) {
            return "corbaloc:iiop:" + host + ":" + (port == null ? "2809" : port);
        }

        @Override
        protected boolean isRunningOn() {
            return existsClass("com.ibm.websphere.management.exception.DescriptorParseException");
        }

        @Override
        public String getHibernateTransactionManagerFactory() {
            return "org.hibernate.transaction.WebSphereExtendedJTATransactionLookup";
        }

        @Override
        public String getServerName() {
            return "WebSphere";
        }

        @Override
        public Properties getJNDIProperties(final boolean local) {
            if (local) {
                return null;
            }
            final Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY,
                    "com.ibm.websphere.naming.WsnInitialContextFactory");
            return props;
        }

        @Override
        public String getTransactionManagerJndi() {
            throw new UnsupportedOperationException("WAS, unlike every other app server on the planet, does not allow"
                    + " direct access to the JTS TransactionManager.  Instead, for common transaction-related tasks users must "
                    + "utilize a proprietary API known as ExtendedJTATransaction. For details please see the org.hibernate.transaction.WebSphereExtendedJTATransactionLookup class.");
        }

        @Override
        public String getUserTransactionJndi() {
            return "java:comp/UserTransaction";
        }
    },
    /**
     * WebLogic.
     */
    WebLogic {

        @Override
        public String getRemoteURL(final String host, final Integer port) {
            return "t3://" + host + ":" + (port == null ? "7001" : port);
        }

        @Override
        protected boolean isRunningOn() {
            return existsClass("weblogic.management.CompatibilityException");
        }

        @Override
        public String getHibernateTransactionManagerFactory() {
            return "org.hibernate.transaction.WeblogicTransactionManagerLookup";
        }

        @Override
        public String getServerName() {
            return "WebLogic";
        }

        @Override
        public Properties getJNDIProperties(final boolean local) {
            if (local) {
                return null;
            }
            final Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY,
                    "weblogic.jndi.WLInitialContextFactory");
            return props;
        }

        @Override
        public String getTransactionManagerJndi() {
            return "javax.transaction.TransactionManager";
        }

        @Override
        public String getUserTransactionJndi() {
            return "javax.transaction.UserTransaction";
        }
    },
    /**
     * OpenEJB.
     */
    OpenEJB {

        @Override
        public String getRemoteURL(final String host, final Integer port) {
            return "ejbd://" + host + ":" + (port == null ? "4201" : port);
        }

        @Override
        protected boolean isRunningOn() {
            return existsClass("org.apache.openejb.jee.EjbJar");
        }

        @Override
        public String getHibernateTransactionManagerFactory() {
            return "sk.baka.tools.javaee.OpenEJBTransactionManagerLookup";
        }

        @Override
        public String getServerName() {
            return "OpenEJB";
        }

        @Override
        public Properties getJNDIProperties(final boolean local) {
            final Properties result = new Properties();
            if (local) {
                result.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                        "org.apache.openejb.client.LocalInitialContextFactory");
            } else {
                result.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                        "org.apache.openejb.client.RemoteInitialContextFactory");
            }
            return result;
        }

        @Override
        public String getTransactionManagerJndi() {
            return "java:comp/TransactionManager";
        }

        @Override
        public String getUserTransactionJndi() {
            return "java:comp/UserTransaction";
        }
    },
    /**
     * The Glassfish application server.
     */
    Glassfish {

        @Override
        public Properties getJNDIProperties(
                boolean local) {
            if (local) {
                return null;
            }
            final Properties p = new Properties();
            p.put(Context.INITIAL_CONTEXT_FACTORY,
                    "com.sun.enterprise.naming.SerialInitContextFactory");
            p.put(Context.URL_PKG_PREFIXES, "com.sun.enterprise.naming");
            p.setProperty(Context.STATE_FACTORIES,
                    "com.sun.corba.ee.impl.presentation.rmi.JNDIStateFactoryImpl");
            return p;
        }

        @Override
        public String getRemoteURL(String host, Integer port) {
            return "corbaloc:iiop:" + host + ":" + (port == null ? "3700" : port);
        }

        @Override
        public String getServerName() {
            return "Glassfish";
        }

        @Override
        public String getHibernateTransactionManagerFactory() {
            return "org.hibernate.transaction.SunONETransactionManagerLookup";
        }

        @Override
        protected boolean isRunningOn() {
            return existsClass("com.sun.enterprise.deployment.ConnectorDescriptor");
        }

        @Override
        public String getTransactionManagerJndi() {
            return "java:appserver/TransactionManager";
        }

        @Override
        public String getUserTransactionJndi() {
            return "java:comp/UserTransaction";
        }
    },
    /**
     * The OC4J application server.
     */
    Oc4j {

        @Override
        public Properties getJNDIProperties(
                boolean local) {
            if (local) {
                return null;
            }
            final Properties p = new Properties();
            p.put(Context.INITIAL_CONTEXT_FACTORY, "com.evermind.server.ApplicationClientInitialContextFactory");
            return p;
        }

        @Override
        public String getRemoteURL(String host, Integer port) {
            return "corbaname:" + host + ":" + (port == null ? "4444" : port);
        }

        @Override
        public String getServerName() {
            return "OC4J";
        }

        @Override
        public String getHibernateTransactionManagerFactory() {
            return "org.hibernate.transaction.OC4JTransactionManagerLookup";
        }

        @Override
        protected boolean isRunningOn() {
            return existsClass("oracle.oc4j.sql.ManagedDataSource");
        }

        @Override
        public String getTransactionManagerJndi() {
            return "java:comp/pm/TransactionManager";
        }

        @Override
        public String getUserTransactionJndi() {
            return "java:comp/UserTransaction";
        }
    };

    /**
     * Checks if we are running on a particular kind of a server.
     *
     * @return <code>true</code> if we are running on this server,
     *         <code>false</code> otherwise.
     */
    protected abstract boolean isRunningOn();
    /**
     * The server we are running on.
     */
    private static JavaEEServer runtime = null;

    /**
     * Returns the server we are running on.
     *
     * @return the server environment. If <code>null</code> then no known server is running in this VM.
     */
    public static JavaEEServer getRuntimeNull() {
        // this is actually thread-safe, even without synchronization: at worst case the {@link #RUNTIME} field will be recomputed multiple times.
        if (runtime == null) {
            for (JavaEEServer server : JavaEEServer.values()) {
                if (server.isRunningOn()) {
                    runtime = server;
                    break;
                }
            }
        }
        return runtime;
    }

    protected static boolean existsClass(final String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch(NoClassDefFoundError e){
            // this may happen when the class itself exists but its dependencies do not. This is the case of OC4J where
            // the com.sun.enterprise.deployment.ConnectorDescriptor class exists but its loading fails because
            // NoClassDefFoundError: com/sun/enterprise/deployment/ConnectorArchivist
            return false;
        }
    }

    /**
     * Returns the server we are running on.
     *
     * @return the server environment.
     */
    public static JavaEEServer getRuntime() {
        runtime = getRuntimeNull();
        if (runtime == null) {
            throw new IllegalStateException("Running on unknown/unsupported JEE server");
        }
        return runtime;
    }

    /**
     * Returns a correct Hibernate
     * <code>org.hibernate.transaction.TransactionManagerLookup</code> instance class name.
     *
     * @return the classname.
     */
    public abstract String getHibernateTransactionManagerFactory();

    /**
     * Returns a JNDI name of the TransactionManager implementation.
     * @return non-null TransactionManager JNDI name.
     * @throws UnsupportedOperationException if the TransactionManager cannot be simply looked up in JNDI.
     */
    public abstract String getTransactionManagerJndi();

    /**
     * Returns a JNDI name of the UserTransaction implementation.
     * @return non-null UserTransaction JNDI name.
     */
    public abstract String getUserTransactionJndi();

    /**
     * Returns the displayable name of the application server.
     *
     * @return the name.
     */
    public abstract String getServerName();

    /**
     * A localhost sample of how the connection URL should look like when
     * connecting to {@link #getRemoteURL(java.lang.String, java.lang.Integer) remote EJBs}.
     *
     * @return non-<code>null</code> sample URL.
     */
    public final String getLocalhostRemoteURL() {
        return getRemoteURL("localhost", null);
    }

    /**
     * Returns the connection URL for connecting to remote EJBs. Use the returned value as a value for {@link Context#PROVIDER_URL}.
     *
     * @param host
     *            the host name, must not be <code>null</code>.
     * @param port
     *            optional port. If <code>null</code> then use the default
     *            port.
     * @return the URL to connect to, never null.
     */
    public abstract String getRemoteURL(final String host, final Integer port);

    /**
     * Returns JNDI properties for instantiating correct instance of
     * {@link InitialContext}. Note that in properly configured JavaEE environment there should be no need to set JNDI parameters to InitialContext, therefore
     * this method should be used in special environments only:
     * <ul><li>You can use local context to lookup beans in mixed environment, in which the webcontainer does not setup the context properly, e.g. Jetty + OpenEJB.</li>
     * <li>Remote context can be used when JavaEE client libraries are not available</li></ul>
     *
     * @param local
     *            if <code>true</code> then JNDI is configured to invoke local
     *            beans; if <code>false</code> then remote beans are invoked.
     *
     * @return properties. May be <code>null</code> if the server requires a JavaEE environment and thus a correct Context instance can be obtained simply by
     * invoking <code>new InitialContext()</code>.
     * @throws UnsupportedOperationException
     *             if this type of connectivity is not supported.
     */
    public abstract Properties getJNDIProperties(final boolean local);
}
