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

import sk.baka.webvm.analyzer.hostos.CPUUsage;
import sk.baka.webvm.analyzer.hostos.ICpuUsageMeasureStrategy;

/**
 *
 * @author Martin Vysny
 */
public class IOCpuUsageWindowsStrategyStrategy implements ICpuUsageMeasureStrategy {

    @Override
    public Object measure() throws Exception {
        return CPUUsage.of(WMIUtils.getIOCPUUsage());
    }

    @Override
    public CPUUsage getAvgCpuUsage(Object m1, Object m2) {
        return (CPUUsage) m2;
    }
    
}
