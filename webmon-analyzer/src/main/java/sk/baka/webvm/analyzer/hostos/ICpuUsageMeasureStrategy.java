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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for measuring CPU usage.
 * @author mvy
 */
public interface ICpuUsageMeasureStrategy {

    /**
     * Measures an implementation-dependent CPU usage statistics. Used in {@link #getAvgCpuUsage(java.lang.Object, java.lang.Object)} to compute the real CPU usage.
     * @return the measurement object. May return null if the measurement is not available.
     * @throws Exception if something happens.
     */
    @Nullable
    Object measure() throws Exception;

    /**
     * Computes an average CPU usage between two measurements. The first measurement was taken before the second one was taken.
     * <p/>
     * The method must not fail if m1 is same as m2 - in such case return 0.
     * @param m1 first measurement, must not be null.
     * @param m2 second measurement, must not be null.
     * @return CPU usage in percent, must be a value between 0 and 100.
     */
    @NotNull
    CPUUsage getAvgCpuUsage(@NotNull final Object m1, @NotNull final Object m2);
}
