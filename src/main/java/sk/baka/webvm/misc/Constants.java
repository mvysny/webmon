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
package sk.baka.webvm.misc;

/**
 * Contains several useful constants.
 * @author Martin Vysny
 */
public final class Constants {

    private Constants() {
        throw new AssertionError();
    }
    /**
     * Number of millis in a second.
     */
    public static final int MILLIS_IN_SECOND = 1000;
    /**
     * Hundred (full) percent.
     */
    public static final int HUNDRED_PERCENT = 100;
    /**
     * Number of bytes in a binary kilobyte.
     */
    public static final long KIBIBYTES = 1024;
    /**
     * Number of bytes in a binary megabyte.
     */
    public static final long MEBIBYTES = KIBIBYTES * KIBIBYTES;
}
