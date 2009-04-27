/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebVM.
 *
 * WebVM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebVM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebVM.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.analyzer;

/**
 * Interface for measuring CPU usage.
 * @author mvy
 */
public interface ICpuUsageMeasure {

    /**
     * Checks if this particular implementation is supported on given host OS.
     * @return true if it is supported, false otherwise.
     */
    boolean supported();

    /**
     * Measures an implementation-dependent CPU usage statistics. Used in {@link #getAvgCpuUsage(java.lang.Object, java.lang.Object)} to compute the real CPU usage.
     * @return the measurement object
     * @throws Exception if something happens.
     */
    Object measure() throws Exception;

    /**
     * Computes an average CPU usage between two measurements. The first measurement was taken before the second one was taken.
     * @param m1 first measurement.
     * @param m2 second measurement
     * @return CPU usage in percent, must be a value between 0 and 100.
     */
    int getAvgCpuUsage(final Object m1, final Object m2);
}
