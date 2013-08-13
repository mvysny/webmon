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

import java.lang.management.MemoryUsage;
import sk.baka.webvm.analyzer.utils.WMIUtils;

/**
 * Provides memory information for a process.
 * @author Martin Vysny
 */
public class Memory {
    /**
     * Provides the Working Set value.
     */
    private static class WindowsProcessMemoryProvider implements IMemoryInfoProvider {
        private final int pid;

        public WindowsProcessMemoryProvider(int pid) {
            this.pid = pid;
        }
        
        public static boolean isAvailable() {
            return WMIUtils.isAvailable();
        }

        public MemoryUsage getSwap() {
            return new MemoryUsage(0, 0, 0, 0);
        }

        public MemoryUsage getPhysicalMemory() {
            return WMIUtils.getWorkingSetSize(pid);
        }
    }
    
    /**
     * Returns a provider which provides memory information about given process.
     * @param pid the process ID.
     * @return the provider, never null.
     */
    public static IMemoryInfoProvider newProcessMemoryInfo(int pid) {
        if (WindowsProcessMemoryProvider.isAvailable()) {
            return new WindowsProcessMemoryProvider(pid);
        }
        return new DummyMemoryStrategy();
    }
}
