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
package sk.baka.webvm.config;

/**
 * Binds values from a {@link Properties} map.
 * @author Martin Vysny
 */
public @interface Bind {
	/**
	 * A key to the {@link Properties} map.
	 */
	String key();
	/**
	 * (Optional) minimum value if number is specified.
	 */
	int min() default Integer.MIN_VALUE;

	/**
	 * (Optional) maximum value if number is specified.
	 */
	int max() default Integer.MAX_VALUE;
}
