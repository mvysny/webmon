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

import java.lang.management.MemoryUsage;
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
     * Append several zero values until a desired data length is reached.
     * @param desiredLength desired length of X axis.
     */
    public void fillWithZero(final int desiredLength) {
        int[] empty = new int[style.colors.length];
        while (values.size() < desiredLength) {
            values.add(empty);
        }
    }

    /**
     * Draws the graph and returns the html code.
     * @return the html code of the graph.
     * @deprecated use the more memory-friendly {@link #draw(java.lang.StringBuilder)}.
     */
    public String draw() {
        final StringBuilder sb = new StringBuilder();
        draw(sb);
        return sb.toString();
    }

    /**
     * Draws the graph and returns the html code.
     * @param sb draw the graph here
     */
    public void draw(final StringBuilder sb) {
        // draw border if necessary
        sb.append("<table");
        if (style.border != null) {
            sb.append(" style=\"border: 1px solid ");
            sb.append(style.border);
            sb.append("; \"");
        }
        sb.append("><tr>");
        // draw legend if necessary
        if (style.yLegend) {
            final int quarterHeight = style.height / 2;
            sb.append("<td style=\"text-align: right; ");
            sb.append("\">");
            sb.append("<div style=\"border-top: 1px solid black; vertical-align: top; height: ");
            sb.append(quarterHeight);
            sb.append("px; \">");
            sb.append(max);
            sb.append("</div>");
            sb.append("<div style=\"border-top: 1px solid black; vertical-align: top; height: ");
            sb.append(quarterHeight);
            sb.append("px; \">");
            sb.append(max / 2);
            sb.append("</div>");
            sb.append("</td>");
        }
        sb.append("<td>");
        // draw the graph itself
        final GraphStyle gs = new GraphStyle(style);
        gs.vertical = !style.vertical;
        gs.border = null;
        for (int i = 0; i < values.size(); i++) {
            drawStackedBar(gs, values.get(i), max, i < values.size() - 1, sb);
        }
        sb.append("</td></tr></table>");
    }

    /**
     * Draws a stacked bar graph.
     * @param style the graph style, must be valid.
     * @param values the values to draw. The array must be sorted from least to highest. Negative values are not permitted.
     * @param max the maximum value. A transparent div will be added at the end if necessary.
     * @return a html source
     */
    public static String drawStackedBar(final GraphStyle style, final int[] values, final int max, final boolean floatLeft) {
        final StringBuilder sb = new StringBuilder();
        drawStackedBar(style, values, max, floatLeft, sb);
        return sb.toString();
    }

    /**
     * Draws a stacked bar graph.
     * @param style the graph style, must be valid.
     * @param values the values to draw. The array must be sorted from least to highest. Negative values are not permitted.
     * @param max the maximum value. A transparent div will be added at the end if necessary.
     * @return a html source
     */
    public static void drawStackedBar(final GraphStyle style, final int[] values, final int max, final boolean floatLeft, final StringBuilder sb) {
        style.validate();
        final int[] pixels = toPixels(values, max, style.vertical ? style.height : style.width);
        sb.append("<div style=\"");
        if (style.border != null) {
            sb.append("border: 1px solid ");
            sb.append(style.border);
            sb.append("; ");
        }
        if (style.showPercentage || style.showValues) {
            sb.append("text-align: ");
            sb.append(style.vertical ? "center" : "right");
            sb.append("; ");
        }
        if (style.vertical) {
            // if multiple stacked bars are drawn, make sure they are positioned horizontally.
            if (floatLeft) {
                sb.append("float:left; ");
            }
            sb.append("width: ");
        } else {
            sb.append("height: ");
        }
        sb.append(style.vertical ? style.width : style.height);
        sb.append("px;\">");
        // go from pixels.length-1 to 0 when vertical
        // go from 0 to pixels.length-1 when horizontal
        for (int i = style.vertical ? pixels.length - 1 : 0; style.vertical ? i >= 0 : i < pixels.length; i += style.vertical ? -1 : 1) {
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
                if (style.fontColors != null) {
                    final String fontColor = style.fontColors[i];
                    if (fontColor != null) {
                        sb.append("color: ");
                        sb.append(fontColor);
                        sb.append("; ");
                    }
                }
            }
            if (!isMax && !style.vertical) {
                sb.append("float: left; ");
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
        int pixelSize = 0;
        int prevPixelSize = 0;
        for (int value : values) {
            prevPixelSize = pixelSize;
            if (value >= max) {
                isMaximumReached = true;
            }
            if (isMaximumReached) {
                pixelSize = maxPixels;
            } else {
                pixelSize = value * maxPixels / max;
            }
            result[i++] = pixelSize - prevPixelSize;
        }
        result[values.length] = maxPixels - pixelSize;
        return result;
    }

    /**
     * Converts a memory usage to a horizontal bar graph.
     * @param usage the memory usage. Should be converted to megabytes as longs will be converted to integers.
     * @param width width of result in pixels.
     * @return a string representation of a horizontal bar line.
     */
    public static String getMemoryStatus(final MemoryUsage usage, final int width) {
        final GraphStyle gs = new GraphStyle();
        gs.vertical = false;
        gs.width = width;
        gs.height = 20;
        gs.showPercentage = true;
        gs.colors = new String[]{"#7e43b2", "#ff7f7f"};
        gs.border = "#999999";
        gs.fontColors = new String[]{"#ffffff", null};
        final int max;
        if (usage.getMax() != Long.MAX_VALUE) {
            max = (int) usage.getMax();
        } else {
            max = (int) (usage.getCommitted() * 7 / 5);
        }
        final String bar = DivGraph.drawStackedBar(gs, new int[]{(int) usage.getUsed(), (int) usage.getCommitted()}, max, false);
        return bar;
    }
}
