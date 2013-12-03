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
package sk.baka.webvm.misc;

import java.io.Serializable;

/**
 * Describes a style of graph to be drawn.
 * @author Martin Vysny
 */
public final class GraphStyle implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * The caption of the graph.
     */
    public String caption;

    /**
     * Creates new object instance.
     */
    public GraphStyle() {
    }

    /**
     * Copy-constructor.
     * @param other copy this instance, must not be null
     */
    public GraphStyle(final GraphStyle other) {
        vertical = other.vertical;
        width = other.width;
        height = other.height;
        colors = other.colors;
        showValues = other.showValues;
        showPercentage = other.showPercentage;
        yLegend = other.yLegend;
        border = other.border;
    }
    /**
     * Draw a legend for the Y axis.
     */
    public boolean yLegend = false;
    /**
     * Draw a thin border around the table if not null. Use a regular HTML color specification.
     */
    public String border = null;
    /**
     * Vertical draws vertical (zero is in the left side) or horizontal bar (zero is on the bottom).
     */
    public boolean vertical = false;
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
     * Display data captions in given colors. Use CSS-style notation, like "red" or "#00ffee". null color is a transparent color, i.e. a div without a background. Optional.
     */
    public String[] fontColors = null;
    /**
     * Draw values to the graph.
     */
    public boolean showValues = false;
    /**
     * Draw percentage values to the graph.
     */
    public boolean showPercentage = false;
    /**
     * The graph legend.
     */
    public GraphStyleEnum style = GraphStyleEnum.StackedBar;

    /**
     * A type of graph to draw.
     */
    public static enum GraphStyleEnum {

        /**
         * A stacked bar graph.
         */
        StackedBar,
        /**
         * A line graph.
         */
        Line;
    }

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
        if (isCaptionColorDifferToBarColor() && (colors.length != fontColors.length)) {
            throw new IllegalArgumentException("fontColors must have the same length as colors");
        }
        if (style == null) {
            throw new IllegalArgumentException("style is null");
        }
    }

    private boolean isCaptionColorDifferToBarColor() {
        return (showValues || showPercentage) && (fontColors != null);
    }
}
