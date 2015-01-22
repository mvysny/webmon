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

import sk.baka.webvm.analyzer.hostos.ICpuUsageMeasure;

/**
 * Returns a Host OS CPU time waiting for IO.
 *
 * @author Martin Vysny
 */
public class IOCpuUsageLinuxStrategy implements ICpuUsageMeasure {

    @Override
    public Object measure() throws Exception {
        return Proc.Diskstats.now();
    }

    @Override
    public int getAvgCpuUsage(Object m1, Object m2) {
        return ((Proc.Diskstats) m2).getCpuIOUsage((Proc.Diskstats) m1);
    }
}
