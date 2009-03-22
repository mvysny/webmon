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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Draws a graph using div elements.
 * @author Martin Vysny
 */
public final class DivGraph {

    /**
     * The maximum value.
     */
    private final int max;
    /**
     * The graph values.
     */
    private final List<int[]> values = new ArrayList<int[]>();
    /**
     * The graph style.
     */
    private final GraphStyle style;

    /**
     * Constructs a new graph instance.
     * @param max the maximum value.
     * @param style the graph style, must be valid.
     */
    public DivGraph(final int max, final GraphStyle style) {
        style.validate();
        this.max = max;
        this.style = style;
    }

    /**
     * Adds given value array to the graph.
     * @param values the values to add. There must be exactly one value for each {@link GraphStyle#colors graph column}. The array is modified by sorting the array.
     */
    public void add(final int[] values) {
        if (values.length != style.colors.length) {
            throw new IllegalArgumentException("Expected " + style.colors.length + " columns but got " + values.length);
        }
        Arrays.sort(values);
        this.values.add(values);
    }

    /**
     * Draws the graph and returns the html code.
     * @return the html code of the graph.
     */
    public String draw() {
        final GraphStyle gs = new GraphStyle(style);
        gs.vertical = !style.vertical;
        final StringBuilder sb = new StringBuilder();
        for (final int[] vals : values) {
            sb.append(drawStackedBar(gs, vals, max));
        }
        return sb.toString();
    }

    /**
     * Draws a stacked bar graph.
     * @param style the graph style, must be valid.
     * @param values the values to draw. The array must be sorted from least to highest. Negative values are not permitted.
     * @param max the maximum value. A transparent div will be added at the end if necessary.
     * @return a html source
     */
    public static String drawStackedBar(final GraphStyle style, final int[] values, final int max) {
        style.validate();
        final int[] pixels = toPixels(values, max, style.vertical ? style.height : style.width);
        final StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"");
        if (style.vertical) {
            // if multiple stacked bars are drawn, make sure they are positioned horizontally.
            sb.append("float: left; width: ");
        } else {
            sb.append("height: ");
        }
        sb.append(style.vertical ? style.width : style.height);
        sb.append("px;\">");
        for (int i = pixels.length - 1; i >= 0; i--) {
            final boolean isEmpty = pixels[i] == 0;
            if (isEmpty) {
                continue;
            }
            sb.append("<div style=\"");
            final boolean isMax = i == pixels.length - 1;
            if (!isMax) {
                sb.append("background-color: ");
                sb.append(style.colors[i]);
                sb.append("; ");
            }
            sb.append(style.vertical ? "height: " : "width: ");
            sb.append(pixels[i]);
            sb.append("px;\">");
            if (!isMax) {
                if (style.showValues) {
                    sb.append(values[i]);
                    if (style.showPercentage) {
                        sb.append(" (");
                    }
                }
                if (style.showPercentage) {
                    sb.append(values[i] * 100 / max);
                    sb.append('%');
                    if (style.showValues) {
                        sb.append(')');
                    }
                }
            }
            if (!style.showValues && !style.showPercentage) {
                sb.append("<!-- -->");
            }
            sb.append("</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Converts given value array into pixel dimensions for each value.
     * @param values the values to convert. The array must be sorted from least to highest. Negative values are not permitted.
     * @param max the maximum value to draw.
     * @param maxPixels the desired pixel dimension.
     * @return a sorted list of pixel dimensions. Contains one additional value for the max value.
     * @throws IllegalArgumentException if the values is empty, max is not a positive number or maxPixels is not a positive number
     * @throws NullPointerException if the values array is null
     */
    static int[] toPixels(final int[] values, final int max, final int maxPixels) {
        if (values.length == 0) {
            throw new IllegalArgumentException("values array is empty");
        }
        final int[] result = new int[values.length + 1];
        int i = 0;
        // when a value reaches maximum there is no need to draw other (greater) values.
        boolean isMaximumReached = false;
        for (int value : values) {
            if (value >= max && !isMaximumReached) {
                isMaximumReached = true;
            }
            if (isMaximumReached) {
                result[i++] = maxPixels;
                continue;
            }
            final int pixelSize = value * maxPixels / max;
            int prevPixelSize = 0;
            if (i > 0) {
                prevPixelSize = result[i - 1];
            }
            result[i++] = pixelSize - prevPixelSize;
        }
        result[values.length] = maxPixels - result[values.length - 1];
        return result;
    }
}
