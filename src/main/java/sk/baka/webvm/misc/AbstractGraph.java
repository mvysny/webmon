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
import java.util.Collections;
import java.util.List;

/**
 * An abstract graph, intended to be extended by classes drawing graphs.
 * @author Martin Vysny
 */
public abstract class AbstractGraph {

    /**
     * The maximum value.
     */
    protected final int max;
    /**
     * The graph values.
     */
    protected final List<int[]> values = new ArrayList<int[]>();
    /**
     * The graph style.
     */
    protected final GraphStyle style;

    /**
     * Constructs a new graph instance.
     * @param max the maximum value.
     * @param style the graph style, must be valid.
     */
    public AbstractGraph(final int max, final GraphStyle style) {
        style.validate();
        this.max = max;
        this.style = style;
    }
    /**
     * Automatically make incoming values ascending.
     */
    public boolean makeAscending = false;

    /**
     * Adds given value array to the graph. The array must be ascending.
     * @param values the values to add. There must be exactly one value for each {@link GraphStyle#colors graph column}.
     */
    public final void add(final int[] values) {
        if (values.length != style.colors.length) {
            throw new IllegalArgumentException("Expected " + style.colors.length + " columns but got " + values.length);
        }
        for (int i = 0; i < values.length - 1; i++) {
            if (values[i] > values[i + 1]) {
                if (!makeAscending) {
                    throw new IllegalArgumentException("Array " + Arrays.toString(values) + " is not ascending");
                } else {
                    values[i + 1] = values[i];
                }
            }
        }
        this.values.add(values);
    }

    /**
     * Append several zero values until a desired data length is reached.
     * @param desiredLength desired length of X axis.
     * @param fillToRight if true then the zero values are appended to the value list. If false then the zero values are prepended before all list values.
     */
    public final void fillWithZero(final int desiredLength, final boolean fillToRight) {
        final int dif = desiredLength - values.size();
        if (dif <= 0) {
            return;
        }
        final List<int[]> listOfZeroes = Collections.nCopies(dif, new int[style.colors.length]);
        if (fillToRight) {
            values.addAll(listOfZeroes);
        } else {
            values.addAll(0, listOfZeroes);
        }
    }

    /**
     * Draws the graph and returns the html code.
     * @return the html code of the graph.
     */
    public final String draw() {
        final StringBuilder sb = new StringBuilder();
        draw(sb);
        return sb.toString();
    }

    /**
     * Draws the graph and returns the html code.
     * @param sb draw the graph here
     */
    public abstract void draw(final StringBuilder sb);
}
