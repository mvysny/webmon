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
import java.lang.management.MemoryUsage;
import sk.baka.webvm.analyzer.utils.MgmtUtils;

/**
 * Provides memory information. Implementors must be thread-safe.
 * <p/>
 * Use {@link MgmtUtils#getMemoryInfoProvider()} to obtain correct memory info provider for your platform.
 * @author Martin Vysny
 */
public interface IMemoryInfoProvider extends Serializable {

    /**
     * Returns swap memory information for host OS. The method should not throw an exception - null should be returned instead.
     * <p/>
     * The meaning of the values being returned:
     * <ul>
     * <li>{@link MemoryUsage#getInit()}: not used, -1</li>
     * <li>{@link MemoryUsage#getUsed()}: Used swap space, equal to the <code>committed</code> value.</li>
     * <li>{@link MemoryUsage#getCommitted()}: Used swap space, equal to the <code>used</code> value.</li>
     * <li>{@link MemoryUsage#getMax()}: Total available swap space.</li>
     * </ul>
     * The following holds true: <code>init &lt;= used == committed &lt;= max</code>.
     * @return memory usage or null if the information is unavailable.
     */
    MemoryUsage getSwap();

    /**
     * Returns physical memory information for host OS. The method should not throw an exception - null should be returned instead.
     * <p/>
     * The meaning of the values being returned:
     * <ul>
     * <li>{@link MemoryUsage#getInit()}: not used, -1</li>
     * <li>{@link MemoryUsage#getUsed()}: Used memory without buffers/cache</li>
     * <li>{@link MemoryUsage#getCommitted()}: Used memory total, including buffers and cache</li>
     * <li>{@link MemoryUsage#getMax()}: Total memory usable to the OS. This may be lower than the amount of memory installed in the system, in case when 32bit system cannot see more than 4GB of RAM.</li>
     * </ul>
     * The following holds true: <code>init &lt;= used &lt;= committed &lt;= max</code>.
     * @return memory usage or null if the information is unavailable.
     */
    MemoryUsage getPhysicalMemory();
}
