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
package sk.baka.webvm.analyzer.classloader;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import sk.baka.tools.IOUtils;

/**
 * Auto-detection of the class loader type.
 * @author Martin Vysny
 */
public enum CLEnum {

    /**
     * The WebMon WAR classloader.
     */
    WAR {

        @Override
        protected boolean matches(final ClassLoader cl) {
            try {
                final Enumeration<URL> e = cl.getResources("WEB-INF/web.xml");
                for (final URL url : Collections.list(e)) {
                    final InputStream in = url.openStream();
                    try {
                        final String webXml = IOUtils.toString(in);
                        if (webXml.contains("WebMon")) {
                            return true;
                        }
                    } finally {
                        IOUtils.closeQuietly(in);
                    }
                }
            } catch (Exception ex) {
                return false;
            }
            return false;
        }
    },
    /**
     * A classloader which loads ejb.jar
     */
    EJBJAR {

        @Override
        protected boolean matches(final ClassLoader cl) {
            return cl.getResource("META-INF/ejb-jar.xml") != null;
        }
    },
    /**
     * Classloader for the EAR application.
     */
    EAR {

        protected boolean matches(final ClassLoader cl) {
            return cl.getResource("META-INF/application.xml") != null;
        }
    },
    /**
     * A {@link ClassLoader#getSystemClassLoader() system classloader}.
     */
    SYSTEM {

        protected boolean matches(final ClassLoader cl) {
            return cl == ClassLoader.getSystemClassLoader();
        }
    },
    /**
     * Root classloader (its parent is null).
     */
    ROOT {

        protected boolean matches(final ClassLoader cl) {
            return cl.getParent() == null;
        }
    },
    /**
     * An unknown classloader type.
     */
    SERVER {

        protected boolean matches(final ClassLoader cl) {
            return false;
        }
    };

    /**
     * Checks if this enum constant matches given classloader.
     * @param cl the classloader to match, not null
     * @return true if given classloader is of this type.
     */
    protected abstract boolean matches(final ClassLoader cl);

    /**
     * Detects the type of classes this class loader manages.
     * @param cl the classloader, not null.
     * @return class loader type
     */
    public static EnumSet<CLEnum> getTypes(final ClassLoader cl) {
        final EnumSet<CLEnum> result = EnumSet.noneOf(CLEnum.class);
        for (final CLEnum e : values()) {
            if (e.matches(cl)) {
                result.add(e);
            }
        }
        if (result.isEmpty()) {
            result.add(SERVER);
        }
        return result;
    }
}
