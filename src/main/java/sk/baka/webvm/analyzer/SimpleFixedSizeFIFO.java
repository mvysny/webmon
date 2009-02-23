package sk.baka.webvm.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A simple object that holds a fixed-size history. A fixed-size FIFO implementation.
 * @param <T> type of items contained in this history object.
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
    }
}
