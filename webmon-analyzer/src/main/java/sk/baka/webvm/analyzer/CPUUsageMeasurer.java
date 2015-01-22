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
package sk.baka.webvm.analyzer;

import sk.baka.webvm.analyzer.hostos.ICpuUsageMeasureStrategy;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.baka.webvm.analyzer.hostos.Cpu;
import sk.baka.webvm.analyzer.utils.Checks;

/**
 * Provides a CPU measurement support.
 * @author Martin Vysny
 */
public final class CPUUsageMeasurer {

    private static final Logger LOG = Logger.getLogger(Cpu.class.getName());
    /**
     * The CPU usage measurer.
     */
    public final ICpuUsageMeasureStrategy cpuUsage;

    /**
     * Creates a new CPU usage measurer.
     * @param usage retrieve usage data from this object.
     */
    public CPUUsageMeasurer(final ICpuUsageMeasureStrategy usage) {
        Checks.checkNotNull("usage", usage);
        cpuUsage = usage;
    }

    /**
     * Returns an average CPU usage in a time slice starting at the previous call of this method.
     * @return average overall CPU usage or -1 if CPU sampling is unsupported or error occurred.
     */
    public synchronized int getCpuUsage() {
        if (cpuUsage == null) {
            return -1;
        }
        if (cpuMeasurement == null) {
            try {
                cpuMeasurement = cpuUsage.measure();
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Failed to measure a CPU usage", ex);
                return -1;
            }
            return 0;
        }
        final Object newMeasurement;
        try {
            newMeasurement = cpuUsage.measure();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to measure a CPU usage", ex);
            return -1;
        }
        final int result = cpuMeasurement == null || newMeasurement == null ? 0 : cpuUsage.getAvgCpuUsage(cpuMeasurement, newMeasurement);
        cpuMeasurement = newMeasurement;
        return result;
    }
    private Object cpuMeasurement = null;
}