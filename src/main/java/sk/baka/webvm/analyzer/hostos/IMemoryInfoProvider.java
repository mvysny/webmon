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
import sk.baka.tools.concurrent.ThreadSafe;

/**
 * Provides memory information. Implementors must be thread-safe.
 * @author Martin Vysny
 */
@ThreadSafe
public interface IMemoryInfoProvider extends Serializable {

    /**
     * Returns swap memory information for host OS. The method should not throw an exception - null should be returned instead.
     * @return memory usage or null if the information is unavailable.
     */
    MemoryUsage getSwap();

    /**
     * Returns physical memory information for host OS. The method should not throw an exception - null should be returned instead.
     * @return memory usage or null if the information is unavailable.
     */
    MemoryUsage getPhysicalMemory();
}
