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

import sk.baka.webvm.analyzer.hostos.ICpuUsageMeasure;
import sk.baka.webvm.analyzer.hostos.windows.WMIUtils;

/**
 *
 * @author Martin Vysny
 */
public class OneProcessCpuUsageWindowsStrategy implements ICpuUsageMeasure {

    public static boolean isAvailable() {
        return WMIUtils.isAvailable();
    }
    private final int pid;

    public OneProcessCpuUsageWindowsStrategy(int pid) {
        this.pid = pid;
    }

    public Object measure() throws Exception {
        return WMIUtils.getProcessPerfRawData(pid);
    }

    public int getAvgCpuUsage(Object m1, Object m2) {
        return ((WMIUtils.Win32_PerfRawData_PerfProc_Process) m2).getCPUUsage((WMIUtils.Win32_PerfRawData_PerfProc_Process) m1);
    }
    
}
