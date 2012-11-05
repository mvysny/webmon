package sk.baka.webvm.analyzer;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * A snapshot of thread state. Immutable, thread-safe.
 * @author Martin Vysny
 */
public final class ThreadMap {

    private static final Logger log = Logger.getLogger(ThreadMap.class.getName());
    public static final ThreadMXBean BEAN;

    static {
        BEAN = ManagementFactory.getThreadMXBean();
        if (!BEAN.isThreadCpuTimeSupported()) {
            log.warning("ThreadMXBean claims Thread CPU times are not supported");
        } else {
            if (!BEAN.isThreadCpuTimeEnabled()) {
                BEAN.setThreadCpuTimeEnabled(true);
                if (!BEAN.isThreadCpuTimeEnabled()) {
                    log.warning("ThreadMXBean claims Thread CPU times are supported, but could not be enabled for unknown reason");
                }
            }
        }
    }

    public static final class Item {

        public final long threadId;
        public final ThreadInfo info;
        /**
         * Total CPU time eaten by a thread, in nanoseconds. -1 if the
         * measurement is not supported.
         */
        public final long totalCpuTimeNanos;
        /**
         * CPU usage in percent since last measure. null if not known.
         */
        public final Integer lastCpuUsagePerc;

        public Item(long threadId, ThreadInfo info, long totalCpuTimeNanos, Integer lastCpuUsagePerc) {
            this.threadId = threadId;
            this.info = info;
            this.totalCpuTimeNanos = totalCpuTimeNanos;
            this.lastCpuUsagePerc = lastCpuUsagePerc;
        }

        public Item setLastCpuUsagePerc(int lastCpuUsagePerc) {
            return new Item(threadId, info, totalCpuTimeNanos, lastCpuUsagePerc);
        }
    }
    private final CopyOnWriteArrayList<Item> items = new CopyOnWriteArrayList<ThreadMap.Item>();
    public final int threadCount;
    public final int daemonThreadCount;
    public final long takenAt;

    public static ThreadMap takeSnapshot() {
        return new ThreadMap();
    }

    public long[] getThreadIDs() {
        final long[] result = new long[items.size()];
        int index = 0;
        for (Item item : items) {
            result[index++] = item.threadId;
        }
        return result;
    }

    public Item get(long threadId) {
        for (Item item : items) {
            if (item.threadId == threadId) {
                return item;
            }
        }
        return null;
    }

    private ThreadMap() {
        takenAt = System.currentTimeMillis();
        final ThreadInfo[] threadInfos = BEAN.getThreadInfo(BEAN.getAllThreadIds());
        daemonThreadCount = BEAN.getDaemonThreadCount();
        final List<Item> items = new ArrayList<Item>(threadInfos.length);
        for (ThreadInfo info : threadInfos) {
            final long threadId = info.getThreadId();
            items.add(new Item(threadId, info, BEAN.getThreadCpuTime(threadId), null));
        }
        this.items.addAll(items);
        threadCount = threadInfos.length;
    }

    public static SortedMap<Long, List<Item>> historyToTable(List<HistorySample> samples) {
        final SortedMap<Long, List<Item>> history = new TreeMap<Long, List<Item>>();
        // compute the map
        int i = 0;
        for (final HistorySample sample : samples) {
            for (Item info : sample.threads.items) {
                if (info == null) {
                    continue;
                }
                List<Item> list = history.get(info.threadId);
                if (list == null) {
                    list = new ArrayList<Item>(samples.size());
                    history.put(info.threadId, list);
                }
                ensureSize(list, i);
                if (!list.isEmpty()) {
                    final Item last = list.get(list.size() - 1);
                    final long newLastMeasurementTimeMillis = sample.threads.takenAt;
                    final long lastMeasurementTimeMillis = samples.get(i - 1).threads.takenAt;
                    if (newLastMeasurementTimeMillis != lastMeasurementTimeMillis) {
                        final long newTotalCpuTimeNanos = info.totalCpuTimeNanos;
                        final long totalCpuTimeNanos = last.totalCpuTimeNanos;
                        final int lastCpuUsagePerc = (int) (((newTotalCpuTimeNanos - totalCpuTimeNanos) / 10000L) / (newLastMeasurementTimeMillis - lastMeasurementTimeMillis));
                        info = info.setLastCpuUsagePerc(lastCpuUsagePerc);
                    }
                }
                list.add(info);
            }
            i++;
        }
        // align dead threads' list for a proper length.
        for (final List<Item> infos : history.values()) {
            ensureSize(infos, i);
        }
        return history;
    }

    /**
     * Ensures that the given list is of given size, appending nulls as
     * necessary.
     *
     * @param list the list to enlarge. Will be modified.
     * @param size the desired size
     * @return the list itself.
     */
    private static <T> void ensureSize(List<? super T> list, final int size) {
        while (list.size() < size) {
            list.add(null);
        }
    }
}
