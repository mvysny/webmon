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
package sk.baka.webvm.analyzer.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds values from a {@link Properties} map.
 * @author Martin Vysny
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
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

    /**
     * Makes the value a part of a group.
     */
    int group() default 0;

    /**
     * true if this field denotes a password field.
     */
    boolean password() default false;

    /**
     * Set to false if this field is not required.
     */
    boolean required() default true;
}
