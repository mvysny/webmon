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
import sk.baka.webvm.Graphs;
import static sk.baka.webvm.misc.Constants.*;

/**
 * Draws stacked bars.
 * @author Martin Vysny
 */
public final class DivGraph {

    /**
     * The height of a memory status bar.
     */
    public static final int MEMSTAT_BAR_HEIGHT = 20;

    /**
     * Draws a HTML DIV element with required properties and style.
     * @param pixels the desired pixel width/height.
     * @param i the value index, used to determine the div color.
     * @param style the style
     * @param values the values
     * @param max maximum value
     * @return a HTML DIV element.
     */
    private static String drawBarValueAsDiv(final int[] pixels, int i, final GraphStyle style, final int[] values, final int max) {
        final StringBuilder sb = new StringBuilder();
        final boolean isEmpty = pixels[i] == 0;
        if (isEmpty) {
            return "";
        }
        final boolean isLastValue = i == pixels.length - 1;
        drawDivOpening(sb, isLastValue, style, i, pixels[i]);
        drawDivContents(isLastValue, style, sb, values, i, max);
        sb.append("</div>");
        return sb.toString();
    }

    private static void drawDivContents(final boolean isLastValue, final GraphStyle style, final StringBuilder sb, final int[] values, final int i, final int max) {
        // here a value in percents should be drawn (if desired)
        if (!isLastValue) {
            if (style.showValues) {
                sb.append(values[i]);
                if (style.showPercentage) {
                    sb.append(" (");
                }
            }
            if (style.showPercentage) {
                if (max <= 0) {
                    sb.append('?');
                } else {
                    sb.append(values[i] * HUNDRED_PERCENT / max);
                }
                sb.append('%');
                if (style.showValues) {
                    sb.append(')');
                }
            }
        }
        if (isLastValue || (!style.showValues && !style.showPercentage)) {
            // nothing should be shown in the div itself. Implement an IE hack
            sb.append("<!-- -->");
        }
    }

    /**
     * Draws an opening div element for given value.
     * @param sb add the element to this builder
     * @param isLastValue if true then this value is the last one shown in the graph. Last value is always drawn with transparent background. Use a custom background color only for non-last value.
     * @param style the style to use
     * @param i the value index
     * @param pixel the desired pixel width/heigth.
     */
    private static void drawDivOpening(final StringBuilder sb, final boolean isLastValue, final GraphStyle style, int i, final int pixel) {
        sb.append("<div style=\"");
        // last value is always drawn with transparent background. Use a custom background color only for non-last value.
        if (!isLastValue) {
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
        if (!isLastValue && !style.vertical) {
            sb.append("float: left; ");
        }
        sb.append(style.vertical ? "height: " : "width: ");
        sb.append(pixel);
        sb.append("px;\">");
    }

    /**
     * Retrieves a HTML div element style, based on given graph style.
     * @param style the style
     * @param floatLeft if true then the <code>float:left</code> will be used.
     * @return the HTML style
     */
    private static String getDivStyle(final GraphStyle style, final boolean floatLeft) {
        final StringBuilder sb = new StringBuilder();
        sb.append("style=\"");
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
        // if multiple stacked bars are drawn, make sure they are positioned horizontally.
        if (style.vertical && floatLeft) {
            sb.append("float:left; ");
        }
        sb.append("width: ");
        sb.append(style.width);
        sb.append("px; height: ");
        sb.append(style.height);
        sb.append("px;\"");
        return sb.toString();
    }

    private DivGraph() {
        throw new AssertionError();
    }

    /**
     * Draws a stacked bar graph.
     * @param style the graph style, must be valid.
     * @param values the values to draw. The array must be sorted from least to highest. Negative values are not permitted.
     * @param max the maximum value. A transparent div will be added at the end if necessary.
     * @param floatLeft if true then a float:left style is added to ensure that next HTML content is positioned to the right of this bar.
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
     * @param floatLeft if true then a float:left style is added to ensure that next HTML content is positioned to the right of this bar.
     * @param sb puts the HTML code here.
     */
    public static void drawStackedBar(final GraphStyle style, final int[] values, final int max, final boolean floatLeft, final StringBuilder sb) {
        style.validate();
        if (max < 0) {
            throw new IllegalArgumentException("Invalid max value: " + max);
        }
        final int[] pixels = toPixels(values, max, style.vertical ? style.height : style.width);
        sb.append("<div ");
        sb.append(getDivStyle(style, floatLeft));
        sb.append(">");
        // go from pixels.length-1 to 0 when vertical
        // go from 0 to pixels.length-1 when horizontal
        if (style.vertical) {
            for (int i = pixels.length - 1; i >= 0; i--) {
                sb.append(drawBarValueAsDiv(pixels, i, style, values, max));
            }
        } else {
            for (int i = 0; i < pixels.length; i++) {
                sb.append(drawBarValueAsDiv(pixels, i, style, values, max));
            }
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
    public static String drawMemoryStatus(final MemoryUsage usage, final int width) {
        final GraphStyle gs = new GraphStyle();
        gs.vertical = false;
        gs.width = width;
        gs.height = MEMSTAT_BAR_HEIGHT;
        gs.showValues = true;
        gs.colors = new String[]{Graphs.COLOR_BLUE, Graphs.COLOR_BROWN};
        gs.border = Graphs.COLOR_GREY;
        gs.fontColors = new String[]{Graphs.COLOR_WHITE, null};
        int max;
        if (usage.getMax() >= 0) {
            max = (int) usage.getMax();
        } else {
            max = (int) (usage.getCommitted() * 7 / 5);
        }
        if (max <= 0) {
            // if "0 out of 0" is about to be displayed, show the graph as empty.
            max = 1;
        }
        return DivGraph.drawStackedBar(gs, new int[]{(int) usage.getUsed(), (int) usage.getCommitted()}, max, false);
    }
}
