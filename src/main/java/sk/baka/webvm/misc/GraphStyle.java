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

import java.io.Serializable;

/**
 * Describes a style of graph to be drawn.
 * @author Martin Vysny
 */
public final class GraphStyle implements Serializable {

    /**
     * Vertical draws vertical (zero is in the left side) or horizontal bar (zero is on the bottom).
     */
    public boolean vertical = true;
    /**
     * The width of the graph in pixels. Must be a positive integer.
     */
    public int width = 0;
    /**
     * The height of the graph in pixels. Must be a positive integer.
     */
    public int height = 0;
    /**
     * Display data in given colors. Use CSS-style notation, like "red" or "#00ffee". null color is a transparent color, i.e. a div without a background.
     */
    public String[] colors = null;
    /**
     * Draw values to the graph.
     */
    public boolean showValues = false;
    /**
     * Draw percentage values to the graph.
     */
    public boolean showPercentage = false;

    /**
     * Validates this value object.
     */
    public void validate() {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be a positive integer");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be a positive integer");
        }
        if (colors.length == 0) {
            throw new IllegalArgumentException("colors must contain at least one color");
        }
    }
}