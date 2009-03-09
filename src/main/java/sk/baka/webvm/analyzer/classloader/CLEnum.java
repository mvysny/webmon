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
package sk.baka.webvm.analyzer.classloader;

import java.util.EnumSet;

/**
 * Auto-detection of the class loader type.
 * @author Martin Vysny
 */
public enum CLEnum {

    WAR {
        @Override
        protected boolean matches(final ClassLoader cl) {
            return cl.getResource("WEB-INF/web.xml") != null;
        }
    }, EJBJAR {
        @Override
        protected boolean matches(final ClassLoader cl) {
            return cl.getResource("META-INF/ejb-jar.xml") != null;
        }
    }, EAR {
        protected boolean matches(final ClassLoader cl) {
            return cl.getResource("META-INF/application.xml") != null;
        }
    }, SYSTEM {
        protected boolean matches(final ClassLoader cl) {
            return cl == ClassLoader.getSystemClassLoader();
        }
    }, ROOT {
        protected boolean matches(final ClassLoader cl) {
            return cl.getParent() == null;
        }
    };

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
        return result;
    }
}
