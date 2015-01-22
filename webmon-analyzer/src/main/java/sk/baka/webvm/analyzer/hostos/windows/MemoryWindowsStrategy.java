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
package sk.baka.webvm.analyzer.hostos.windows;

import java.lang.management.MemoryUsage;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;
import sk.baka.webvm.analyzer.hostos.MemoryJMXStrategy;

/**
 * Provides Windows memory data from the WMI.
 * @author Martin Vysny
 */
public class MemoryWindowsStrategy implements IMemoryInfoProvider {

    public static boolean isAvailable() {
        return WMIUtils.isAvailable();
    }
    
    @Override
    public MemoryUsage getSwap() {
        if (WMIUtils.isAvailable()) {
            return WMIUtils.getSwapUsage();
        }
        return null;
    }

    @Override
    public MemoryUsage getPhysicalMemory() {
        // fallback to JMX
        return new MemoryJMXStrategy().getPhysicalMemory();
    }
}
