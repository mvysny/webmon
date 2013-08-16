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
package sk.baka.webvm.analyzer.hostos.linux;

import java.io.File;
import java.lang.management.MemoryUsage;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;
import sk.baka.webvm.analyzer.hostos.OS;
import sk.baka.webvm.analyzer.utils.Processes;

/**
 * Retrieves host OS memory info using /proc/meminfo (Linux only)
 */
public final class MemoryLinuxStrategy implements IMemoryInfoProvider {

    private static final boolean AVAIL;
    private static final File MEMINFO = new File("/proc/meminfo");

    private static Proc.LinuxProperties parseMeminfo() {
        return Proc.LinuxProperties.parse(MEMINFO);
    }
    private static final Logger log = Logger.getLogger(MemoryLinuxStrategy.class.getName());

    /**
     * The page size, -1 if not running on Linux.
     */
    public static final int PAGE_SIZE;
    static {
        boolean avail = false;
        int pageSize = -1;
        if (OS.isLinuxBased()) {
            try {
                if (!parseMeminfo().isEmpty()) {
                    pageSize = Integer.parseInt(Processes.executeAndWait(null, "getconf", "PAGESIZE").checkSuccess().getOutput().trim());
                    log.info("Linux reports page size of " + pageSize + " bytes long");
                    avail = true;
                }
            } catch (Throwable ex) {
                log.log(Level.INFO, "MemoryLinuxStrategy disabled: a failure occurred retrieving system information", ex);
            }
        }
        AVAIL = avail;
        PAGE_SIZE = pageSize;
    }

    /**
     * Checks that this strategy is available to use.
     * @return true if available, false otherwise.
     */
    public static boolean available() {
        return AVAIL;
    }

    public MemoryUsage getPhysicalMemory() {
        if (!available()) {
            return null;
        }
        final Proc.LinuxProperties memInfo = parseMeminfo();
        if (memInfo.isEmpty()) {
            return null;
        }
        final Long total = memInfo.getValueInBytesNull("MemTotal");
        final Long free = memInfo.getValueInBytesNull("MemFree");
        final Long buffers = memInfo.getValueInBytesNull("Buffers");
        final Long cache = memInfo.getValueInBytesNull("Cached");
        if (total == null || free == null || buffers == null || cache == null) {
            return null;
        }
        final long committed = total - free;
        final long used = committed - buffers - cache;
        return new MemoryUsage(-1, used, committed, total);
    }

    public MemoryUsage getSwap() {
        if (!available()) {
            return null;
        }
        final Proc.LinuxProperties memInfo = parseMeminfo();
        if (memInfo.isEmpty()) {
            return null;
        }
        final Long total = memInfo.getValueInBytesNull("SwapTotal");
        final Long free = memInfo.getValueInBytesNull("SwapFree");
        if (total == null || free == null) {
            return null;
        }
        final long used = total - free;
        return new MemoryUsage(-1, used, used, total);
    }
}
