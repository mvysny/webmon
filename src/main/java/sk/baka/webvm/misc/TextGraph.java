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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple class which paints text graphs.
 * @author Martin Vysny
 */
public final class TextGraph {

    /**
     * Minimum value.
     */
    private int min = Integer.MAX_VALUE;
    private boolean autodetect = true;

    /**
     * Sets the min/max range, disabling autodetection mechanism.
     * @param min the minimum
     * @param max the maximum
     */
    public void setRange(final int min, final int max) {
        if (min >= max) {
            throw new IllegalArgumentException("min<max must be true");
        }
        this.min = min;
        this.max = max;
        autodetect = false;
    }
    private int max = Integer.MIN_VALUE;
    private static final char[] GRAPH_DRAWABLES = new char[]{' ', '_', 'x', '#'};
    private final List<Integer> values = new ArrayList<Integer>();

    /**
     * Appends a value.
     * @param value the value to append.
     */
    public void addValue(final int value) {
        if (autodetect) {
            if (min > value) {
                min = value;
            }
            if (max < value) {
                max = value;
            }
        }
        values.add(value);
    }

    /**
     * Draws the graph. Width of the graph is determined by the number of values.
     * @param w draw the graph to this writer.
     * @param height a height in characters of the graph.
     * @throws IOException if write failure occurs.
     */
    public void draw(final int height, final Writer w) throws IOException {
        if (height <= 0) {
            throw new IllegalArgumentException("height must be >0");
        }
        if (values.isEmpty()) {
            return;
        }
        final int totalValueRange = max - min;
        int yLegendLength = -1;
        for (int row = height; row > 0; row--) {
            int rowValueRangeMax = totalValueRange * row / height + min;
            int rowValueRangeMin = rowValueRangeMax - totalValueRange / height;
            final String yLegend = Integer.toString((rowValueRangeMax + rowValueRangeMin) / 2);
            if (yLegendLength == -1) {
                yLegendLength = yLegend.length();
            }
            int currentYLegendLength = yLegend.length();
            while (currentYLegendLength < yLegendLength) {
                currentYLegendLength++;
                w.append(' ');
            }
            w.append(yLegend);
            w.append('|');
            drawLine(rowValueRangeMin, rowValueRangeMax, w);
            w.append('\n');
        }
        // draw bottom line
        for (int i = 0; i < yLegendLength; i++) {
            w.append(' ');
        }
        w.append('+');
        for (int i = 0; i < values.size(); i++) {
            w.append('-');
        }
        w.append('\n');
    }

    private void drawLine(int rowValueRangeMin, int rowValueRangeMax, Writer w) throws IOException {
        for (final Integer value : values) {
            if (value < rowValueRangeMin) {
                w.append(GRAPH_DRAWABLES[0]);
            } else if (value >= rowValueRangeMax) {
                w.append(GRAPH_DRAWABLES[GRAPH_DRAWABLES.length - 1]);
            } else {
                int charIndex = (value - rowValueRangeMin) * 4 / (rowValueRangeMax - rowValueRangeMin);
                if (charIndex < 0) {
                    charIndex = -charIndex;
                }
                w.append(GRAPH_DRAWABLES[charIndex]);
            }
        }
    }

    /**
     * Draws the graph and returns it as a string. Use {@link #draw(int, java.io.Writer) } if you want to be memory-effective.
     * @param height a height in characters of the graph.
     * @return drawn graph.
     */
    public String draw(final int height) {
        final StringWriter result = new StringWriter();
        try {
            draw(height, result);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return result.toString();
    }
}
