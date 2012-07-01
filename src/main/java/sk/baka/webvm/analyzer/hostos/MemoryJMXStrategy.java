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
package sk.baka.webvm.analyzer.hostos;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Retrieves host OS memory info using com.sun.management.OperatingSystemMXBean
 */
public final class MemoryJMXStrategy implements IMemoryInfoProvider {

    private static final OperatingSystemMXBean BEAN;
    private static final Class<?> BEAN_CLASS;
    private static final Logger LOG = Logger.getLogger(MemoryJMXStrategy.class.getName());

    static {
        OperatingSystemMXBean b = null;
        Class<?> clazz = null;
        try {
            clazz = Class.forName("com.sun.management.OperatingSystemMXBean");
            b = OperatingSystemMXBean.class.cast(clazz.cast(ManagementFactory.getOperatingSystemMXBean()));
        } catch (ClassNotFoundException ex) {
            LOG.log(Level.INFO, "MemoryJMXStrategy disabled: com.sun.management.OperatingSystemMXBean unavailable", ex);
        }
        BEAN = b;
        BEAN_CLASS = clazz;
    }

    /**
     * Checks that this strategy is available to use.
     * @return true if available, false otherwise.
     */
    public static boolean available() {
        return BEAN != null;
    }

    public MemoryUsage getPhysicalMemory() {
        if (!available()) {
            return null;
        }
        try {
            final long total = (Long) BEAN_CLASS.getMethod("getTotalPhysicalMemorySize").invoke(BEAN);
            final long free = (Long) BEAN_CLASS.getMethod("getFreePhysicalMemorySize").invoke(BEAN);
            final long used = total - free;
            return new MemoryUsage(-1, used, used, total);
        } catch (Exception e) {
            LOG.log(Level.INFO, "Failed to obtain JMX memory", e);
            return null;
        }
    }

    public MemoryUsage getSwap() {
        if (!available()) {
            return null;
        }
        try {
            final long total = (Long) BEAN_CLASS.getMethod("getTotalSwapSpaceSize").invoke(BEAN);
            final long free = (Long) BEAN_CLASS.getMethod("getFreeSwapSpaceSize").invoke(BEAN);
            final long used = total - free;
            return new MemoryUsage(-1, used, used, total);
        } catch (Exception e) {
            LOG.log(Level.INFO, "Failed to obtain JMX memory", e);
            return null;
        }
    }
}
