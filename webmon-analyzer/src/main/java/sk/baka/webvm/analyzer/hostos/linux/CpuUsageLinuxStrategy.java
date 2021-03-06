/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * WebMon is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * WebMon. If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.analyzer.hostos.linux;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.baka.webvm.analyzer.hostos.CPUUsage;
import sk.baka.webvm.analyzer.hostos.ICpuUsageMeasureStrategy;

/**
 * Returns a Host OS CPU usage information.
 *
 * @author Martin Vysny
 */
public class CpuUsageLinuxStrategy implements ICpuUsageMeasureStrategy {

    @Nullable
    public Object measure() throws Exception {
        return Proc.Stat.now();
    }

    @NotNull
    public CPUUsage getAvgCpuUsage(@NotNull Object m1, @NotNull Object m2) {
        return ((Proc.Stats) m2).getCPUUsage((Proc.Stats) m1);
    }
}
