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

import java.lang.management.MemoryUsage;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;
import sk.baka.webvm.analyzer.utils.MemoryUsage2;

/**
 * Provides the RSS and SWAP values.
 * @author Martin Vysny
 */
public class LinuxProcessMemoryProvider implements IMemoryInfoProvider {
    private final int pid;

    public LinuxProcessMemoryProvider(int pid) {
        this.pid = pid;
    }

    public static boolean isAvailable() {
        return Proc.Stat.isAvailable();
    }

    public MemoryUsage2 getSwap() {
        final Proc.PidStatus status = Proc.PidStatus.now(pid);
        if (status == null) {
            return null;
        }
        final Long swap = status.getVmSwapNull();
        return swap == null ? null : new MemoryUsage2(0, swap, swap, swap);
    }

    public MemoryUsage2 getPhysicalMemory() {
        final Proc.PidStat stat = Proc.PidStat.now(pid);
        if (stat == null) {
            return null;
        }
        final long rss = stat.getRSSAsBytes();
        return new MemoryUsage2(0, rss, rss, rss);
    }
}
