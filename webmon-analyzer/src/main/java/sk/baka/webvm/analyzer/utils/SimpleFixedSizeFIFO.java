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
package sk.baka.webvm.analyzer.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A simple object that holds a fixed-size history. A fixed-size FIFO implementation. The object is generally thread-unsafe,
 * with the exception of the {@link #toList()} method which can safely be invoked from any thread.
 * @param <T> type of items contained in this history object.
 * @author Martin Vysny
 */
public final class SimpleFixedSizeFIFO<T> {

    private final int maxLength;
    private int historyLength = 0;
    private final Queue<T> history = new ConcurrentLinkedQueue<T>();

    /**
     * Creates new instance.
     * @param maxLength maximum length of the FIFO queue. Old items are dropped automatically.
     */
    public SimpleFixedSizeFIFO(final int maxLength) {
        super();
        this.maxLength = maxLength;
    }

    /**
     * Adds an item to the history, discarding oldest items when the maximum length has been reached. Not thread-safe - may be called from one thread instance only.
     * @param item the item to add
     */
    public void add(T item) {
        while (historyLength >= maxLength) {
            history.poll();
            historyLength--;
        }
        history.add(item);
        historyLength++;
        newest = item;
    }

    /**
     * Returns a snapshot of the FIFO. Thread-safe.
     * @return a snapshot, first item is the oldest one.
     */
    public List<T> toList() {
        return new ArrayList<T>(history);
    }

    /**
     * Clears the history.
     */
    public void clear() {
        history.clear();
        historyLength =0;
        newest = null;
    }
    private T newest = null;

    /**
     * Returns the newest item put into this queue. Not thread-safe - may be called from the same thread which is {@link #add(java.lang.Object) adding} items only.
     * @return newest item or null if the FIFO is empty.
     */
    public T getNewest() {
        return newest;
    }
}
