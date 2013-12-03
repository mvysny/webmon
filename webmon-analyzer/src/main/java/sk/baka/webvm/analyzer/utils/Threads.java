package sk.baka.webvm.analyzer.utils;

import java.lang.management.ThreadInfo;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.baka.webvm.analyzer.ThreadMap;

/**
 *
 * @author Martin Vysny
 */
public class Threads {

    private Threads() {
    }
    private static final Logger log = Logger.getLogger(Threads.class.getName());

    /**
     * Tries to retrieve a list of all threads owned by given executor. If this
     * function is not available for given particular executor implementation,
     * null is returned.
     *
     * @param e the executor, not null.
     * @return a list of threads owned by given executor. May contain
     * unstarted/dead threads. Use {@link #getLive(java.util.Collection)} to
     * filter live threads only.
     */
    public static List<Thread> getThreads(Executor e) {
        Checks.checkNotNull("e", e);
        try {
            if (e instanceof ThreadPoolExecutor) {
                return getThreadsTPE((ThreadPoolExecutor) e);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }

    /**
     * Returns a new list of threads, containing only live threads
     * ({@link Thread#isAlive()} returns {@code true}).
     *
     * @param t the list of threads, not null, may be empty. Not modified.
     * @return a list of live threads, never null, may be empty.
     */
    public static List<Thread> getLive(Collection<Thread> t) {
        final List<Thread> list = new ArrayList<Thread>(t.size());
        for (Thread thread : t) {
            if (thread.isAlive()) {
                list.add(thread);
            }
        }
        return list;
    }
    private static final Field TPE_MAINLOCK;
    private static final Field TPE_WORKERS;
    private static final Field TPE_WORKER_THREAD;
    private static final boolean TPE_AVAILABLE;

    static {
        Field tpeMainlock = null;
        Field tpeWorkers = null;
        Field tpeWorkerThread = null;
        boolean tpeAvailable = false;
        try {
            tpeMainlock = ThreadPoolExecutor.class.getDeclaredField("mainLock");
            tpeMainlock.setAccessible(true);
            tpeMainlock.getType().asSubclass(Lock.class);
            tpeWorkers = ThreadPoolExecutor.class.getDeclaredField("workers");
            tpeWorkers.setAccessible(true);
            tpeWorkers.getType().asSubclass(Set.class);
            tpeWorkerThread = Class.forName(ThreadPoolExecutor.class.getName() + "$Worker").getDeclaredField("thread");
            tpeWorkerThread.setAccessible(true);
            tpeWorkerThread.getType().asSubclass(Thread.class);
            tpeAvailable = true;
        } catch (Throwable t) {
            log.log(Level.INFO, "Failed to retrieve fields necessary to access thread list from ThreadPoolExecutor; Threads.getThreads() will return null", t);
        }
        TPE_MAINLOCK = tpeMainlock;
        TPE_WORKERS = tpeWorkers;
        TPE_WORKER_THREAD = tpeWorkerThread;
        TPE_AVAILABLE = tpeAvailable;
    }

    private static List<Thread> getThreadsTPE(ThreadPoolExecutor e) throws IllegalAccessException, InterruptedException {
        if (!TPE_AVAILABLE) {
            return null;
        }
        final List<Thread> result = new ArrayList<Thread>();
        final Lock mainLock = (Lock) TPE_MAINLOCK.get(e);
        mainLock.lockInterruptibly();
        try {
            for (Object worker : (Set<?>) TPE_WORKERS.get(e)) {
                result.add((Thread) TPE_WORKER_THREAD.get(worker));
            }
        } finally {
            mainLock.unlock();
        }
        return result;
    }

    /**
     * Returns threads for which given thread is waiting. Note that this method
     * only returns threads which are currently involved in invocation of the
     * following methods:
     * <ul>
     * <li>{@link #join(java.lang.Thread, long)}</li>
     * <li>{@link #awaitTermination(java.util.concurrent.ExecutorService, long)}</li>
     * </ul>
     *
     * @param thread the thread, not null.
     * @return a list of threads given thread is blocked on. Never null, may be
     * empty.
     */
    public static List<Thread> getWaitingFor(Thread thread) {
        final CopyOnWriteArrayList<Thread> w = waits.get(new Identity<Thread>(thread));
        return w == null ? Collections.<Thread>emptyList() : Collections.unmodifiableList(w);
    }

    /**
     * Returns a map mapping thread ID to a list of thread IDs this thread is
     * blocked on.
     *
     * @return snapshot of waiting threads, never null, may be empty.
     */
    public static Map<ThreadID, List<ThreadID>> getWaiting() {
        final Map<ThreadID, List<ThreadID>> map = new HashMap<ThreadID, List<ThreadID>>();
        for (Map.Entry<Identity<Thread>, CopyOnWriteArrayList<Thread>> e : waits.entrySet()) {
            final List<ThreadID> list = new ArrayList<ThreadID>();
            for (Thread t : e.getValue()) {
                list.add(ThreadID.of(t));
            }
            map.put(ThreadID.of(e.getKey().ref), Collections.unmodifiableList(list));
        }
        return map;
    }

    /**
     * Calls {@link Thread#join(long)} on target thread. Sets this thread name
     * to the old name plus {@code [join on thread 0xID]} - this will allow you
     * to see in stack dump the thread, which blocks this thread.
     * <p/>
     * Also, marks this thread as waiting for given thread. You can use
     * {@link #getWaitingFor(Thread)} to obtain waiting threads.
     *
     * @param thread the thread to join on, not null.
     * @param duration millis to join. Warning: contrary to the
     * {@link Thread#join(long)}, 0 DOES NOT JOIN forever - instead, a call with
     * zero duration will do nothing! This is to keep this function consistent
     * with
     * {@link #awaitTermination(java.util.concurrent.ExecutorService, sk.baka.webvm.analyzer.utils.Duration)}.
     * @throws InterruptedException if this thread is interrupted during the
     * join operation.
     */
    public static void join(Thread thread, Duration duration) throws InterruptedException {
        Checks.checkNotNull("thread", thread);
        if (duration.isEmpty()) {
            return;
        }
        markCurrentThreadAsWaitingFor("joined", Collections.singletonList(thread));
        try {
            thread.join(duration.millis);
        } finally {
            unmarkCurrentThreadAsWaiting();
        }
    }

    /**
     * Calls
     * {@link ExecutorService#awaitTermination(long, java.util.concurrent.TimeUnit)}
     * on target executor. Sets this thread name to the old name plus
     * {@code [awaits executor threads 0xID, 0xea]} - this will allow you to see
     * in stack dump the thread, which blocks this thread.
     * <p/>
     * Also, marks this thread as waiting for given thread. You can use
     * {@link #getWaitingFor(Thread)} to obtain waiting threads.
     *
     * @param thread the thread to join on, not null.
     * @param duration how long to wait, not null.
     * @throws InterruptedException if this thread is interrupted during the
     * join operation.
     * @return <tt>true</tt> if this executor terminated and
     * <tt>false</tt> if the timeout elapsed before termination
     */
    public static boolean awaitTermination(ExecutorService es, Duration duration) throws InterruptedException {
        Checks.checkNotNull("es", es);
        List<Thread> threads = getThreads(es);
        String operation = "awaits executor";
        if (threads == null) {
            threads = Collections.emptyList();
            operation += " unsupported " + es.getClass().getName();
        }
        markCurrentThreadAsWaitingFor(operation, threads);
        try {
            return es.awaitTermination(duration.millis, TimeUnit.MILLISECONDS);
        } finally {
            unmarkCurrentThreadAsWaiting();
        }
    }

    private static void unmarkCurrentThreadAsWaiting() {
        final Thread currentThread = Thread.currentThread();
        waits.remove(new Identity<Thread>(currentThread));
        if (newThreadName.get().equals(currentThread.getName())) {
            currentThread.setName(oldThreadName.get());
        }
        newThreadName.set(null);
        oldThreadName.set(null);
    }

    private static void markCurrentThreadAsWaitingFor(String operation, List<Thread> threads) {
        final Thread currentThread = Thread.currentThread();
        final String oldName = currentThread.getName();
        oldThreadName.set(oldName);
        final StringBuilder sb = new StringBuilder(oldName);
        sb.append(" [").append(operation).append(" ").append(threads.size()).append(" thread(s)");
        for (Thread t : threads) {
            sb.append(" 0x").append(Long.toHexString(t.getId()));
        }
        sb.append(']');
        final String newName = sb.toString();
        newThreadName.set(newName);
        currentThread.setName(newName);
        waits.put(new Identity<Thread>(currentThread), new CopyOnWriteArrayList<Thread>(threads));
    }
    private static final ThreadLocal<String> oldThreadName = new ThreadLocal<String>();
    private static final ThreadLocal<String> newThreadName = new ThreadLocal<String>();

    private static final class Identity<T> {

        public final T ref;

        public Identity(T ref) {
            Checks.checkNotNull("ref", ref);
            this.ref = ref;
        }

        @Override
        public String toString() {
            return ref.toString();
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(ref);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Identity)) {
                return false;
            }
            return ref == ((Identity<T>) obj).ref;
        }
    }
    /**
     * Maps waiting thread to a list of threads it waits for.
     */
    private static final ConcurrentMap<Identity<Thread>, CopyOnWriteArrayList<Thread>> waits = new ConcurrentHashMap<Identity<Thread>, CopyOnWriteArrayList<Thread>>();

    /**
     * Shows a basic thread info: thread ID, whether the thread is native,
     * suspended, etc.
     *
     * @param info the thread info.
     * @return pretty-printed thread info.
     */
    public static String getThreadMetadata(final ThreadInfo info) {
        final StringBuilder sb = new StringBuilder();
        sb.append("0x");
        sb.append(Long.toHexString(info.getThreadId()));
        sb.append(" named '");
        sb.append(info.getThreadName());
        sb.append("' ");
        sb.append(info.getThreadState().toString());
        if (info.isInNative()) {
            sb.append(", in native");
        }
        if (info.isSuspended()) {
            sb.append(", suspended");
        }
        final String lockName = info.getLockName();
        if (lockName != null) {
            sb.append(", locked on lock named '");
            sb.append(lockName);
            sb.append("'");
            sb.append(" owned by thread: ");
            if (info.getLockOwnerId() < 0) {
                sb.append("none");
            } else {
                sb.append(new ThreadID(info.getLockOwnerId()));
                sb.append(" named '");
                sb.append(info.getLockOwnerName());
                sb.append("'");
            }
        }
        return sb.toString();
    }

    /**
     * Pretty-prints a thread stacktrace, similar to
     * {@link Throwable#printStackTrace()} except that it handles nulls
     * correctly.
     *
     * @param info
     * @return string representation of the stacktrace.
     */
    public static String getThreadStacktrace(final ThreadInfo info) {
        final StringBuilder sb = new StringBuilder();
        final StackTraceElement[] stack = info.getStackTrace();
        if (stack == null) {
            sb.append("  stack trace not available\n");
        } else if (stack.length == 0) {
            sb.append("  stack trace is empty\n");
        } else {
            for (final StackTraceElement ste : stack) {
                sb.append("  at ");
                sb.append(ste.toString());
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    public static final class Info {

        /**
         * This thread ID, not null.
         */
        public final ThreadID id;
        /**
         * Information about this thread, not null.
         */
        public final ThreadInfo info;
        /**
         * This thread is waiting for these threads to finish. May be null if
         * there are no threads.
         */
        public final List<ThreadID> waiting;

        public Info(ThreadID id, ThreadInfo info, List<ThreadID> waiting) {
            this.id = id;
            this.info = info;
            this.waiting = waiting;
        }

        public boolean hasBlockers() {
            return waiting != null && !waiting.isEmpty();
        }

        public String getThreadMetadata() {
            return Threads.getThreadMetadata(info);
        }
    }

    /**
     * Captures a snapshot of threads.
     */
    public static final class Dump {

        /**
         * Unmodifiable map.
         */
        public final SortedMap<ThreadID, Info> threads;

        /**
         * Upon construction, captures a snapshot of threads.
         */
        public Dump() {
            final Map<ThreadID, List<ThreadID>> waiting = Threads.getWaiting();
            final ThreadInfo[] info = ThreadMap.BEAN.getThreadInfo(ThreadMap.BEAN.getAllThreadIds(), Integer.MAX_VALUE);
            final SortedMap<ThreadID, Info> threads = new TreeMap<ThreadID, Info>();
            for (ThreadInfo ti : info) {
                if (ti == null) {
                    continue;
                }
                final ThreadID id = new ThreadID(ti.getThreadId());
                threads.put(id, new Info(id, ti, waiting.get(id)));
            }
            this.threads = Collections.unmodifiableSortedMap(threads);
        }

        /**
         * Returns thread info for given thread. Fails if there is no such
         * thread.
         *
         * @param threadId the thread id, not null.
         * @return info, never null.
         */
        public Info get(ThreadID threadId) {
            final Info ti = threads.get(threadId);
            if (ti == null) {
                throw new IllegalArgumentException("Parameter threadId: invalid value " + threadId + ": not present in the threads map. Available thread IDs: " + threads.keySet());
            }
            return ti;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            for (final Threads.Info i : threads.values()) {
                sb.append(i.getThreadMetadata());
                sb.append('\n');
                sb.append(getThreadStacktrace(i.id, true));
                sb.append('\n');
            }
            return sb.toString();
        }

        /**
         * Formats a thread stack trace. Optionally includes blocker threads
         * stracktraces (similar as caused-by).
         *
         * @param threadId the thread ID, must be present in {@link #threads}.
         * @param iterateBlockerThreads
         * @return formatted stack-trace. For details see
         * {@link Threads#getThreadStacktrace(java.lang.management.ThreadInfo)}.
         */
        public String getThreadStacktrace(ThreadID threadId, boolean iterateBlockerThreads) {
            Info ti = get(threadId);
            if (!iterateBlockerThreads) {
                return Threads.getThreadStacktrace(ti.info);
            }
            final Set<ThreadID> visitedThreads = new HashSet<ThreadID>();
            final StringBuilder sb = new StringBuilder();
            sb.append(Threads.getThreadStacktrace(ti.info));
            visitedThreads.add(ti.id);
            while (ti.hasBlockers()) {
                sb.append("Blocked by ").append(ti.waiting);
                final ThreadID blocker = ti.waiting.get(0);
                Info ti2 = threads.get(blocker);
                if (ti2 == null) {
                    sb.append("; WARNING: thread ").append(blocker).append(" is reportedly blocking this thread but is not available in thread dump - perhaps it is no longer alive?\n");
                    break;
                }
                if (!visitedThreads.add(ti2.id)) {
                    sb.append(", but thread ").append(ti2.id).append(" stack already printed - a deadlock?\n");
                    break;
                }
                sb.append(", stacktrace of thread ").append(ti2.id).append(" follows\n");
                sb.append(Threads.getThreadStacktrace(ti2.info));
                ti = ti2;
            }
            return sb.toString();
        }
    }

    public static class ExceptionLoggingRunnableDecorator implements Runnable {

        public final Runnable original;

        public ExceptionLoggingRunnableDecorator(Runnable original) {
            this.original = Objects.requireNonNull(original);
        }

        @Override
        public void run() {
            try {
                original.run();
            } catch (Throwable t) {
                log.log(Level.SEVERE, "A failure occurred", t);
                throw propagate(t);
            }
        }
    }

    private static <X extends Throwable> void propagateIfInstanceOf(
            Throwable throwable, Class<X> declaredType) throws X {
        // Check for null is needed to avoid frequent JNI calls to isInstance().
        if (throwable != null && declaredType.isInstance(throwable)) {
            throw declaredType.cast(throwable);
        }
    }

    private static void propagateIfPossible(Throwable throwable) {
        propagateIfInstanceOf(throwable, Error.class);
        propagateIfInstanceOf(throwable, RuntimeException.class);
    }

    private static RuntimeException propagate(Throwable throwable) {
        propagateIfPossible(Checks.checkNotNull("throwable", throwable));
        throw new RuntimeException(throwable);
    }

    private static <X extends Throwable> void propagateIfPossible(
            Throwable throwable, Class<X> declaredType) throws X {
        propagateIfInstanceOf(throwable, declaredType);
        propagateIfPossible(throwable);
    }

    public static class ExceptionLoggingCallableDecorator<T> implements Callable<T> {

        public final Callable<T> original;

        public ExceptionLoggingCallableDecorator(Callable<T> original) {
            this.original = Objects.requireNonNull(original);
        }

        @Override
        public T call() throws Exception {
            try {
                return original.call();
            } catch (Throwable t) {
                log.log(Level.SEVERE, "A failure occurred", t);
                propagateIfPossible(t, Exception.class);
                throw propagate(t); // never reached
            }
        }
    }

    /**
     * Wraps given executor service with a service which decorates all
     * Runnables/Callables to logs all Throwables.
     *
     * @param original the original, not null.
     * @return decorated service, never null.
     */
    public static ExecutorService logging(final ExecutorService original) {
        Objects.requireNonNull(original);
        return new ExecutorService() {
            @Override
            public void shutdown() {
                original.shutdown();
            }

            @Override
            public List<Runnable> shutdownNow() {
                return original.shutdownNow();
            }

            @Override
            public boolean isShutdown() {
                return original.isShutdown();
            }

            @Override
            public boolean isTerminated() {
                return original.isTerminated();
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return original.awaitTermination(timeout, unit);
            }

            @Override
            public <T> Future<T> submit(Callable<T> task) {
                return original.submit(new ExceptionLoggingCallableDecorator<T>(task));
            }

            @Override
            public <T> Future<T> submit(Runnable task, T result) {
                return original.submit(new ExceptionLoggingRunnableDecorator(task), result);
            }

            @Override
            public Future<?> submit(Runnable task) {
                return original.submit(new ExceptionLoggingRunnableDecorator(task));
            }

            private <T> List<Callable<T>> wrap(Collection<? extends Callable<T>> tasks) {
                final List<Callable<T>> result = new ArrayList<Callable<T>>();
                for (Callable<T> task : tasks) {
                    result.add(new ExceptionLoggingCallableDecorator<T>(task));
                }
                return result;
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
                return original.invokeAll(wrap(tasks));
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
                return original.invokeAll(wrap(tasks), timeout, unit);
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
                return original.invokeAny(wrap(tasks));
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return original.invokeAny(wrap(tasks), timeout, unit);
            }

            @Override
            public void execute(Runnable command) {
                original.execute(new ExceptionLoggingRunnableDecorator(command));
            }
        };
    }

    /**
     * Decorates given executor with a service which logs all throwables.
     *
     * @param original the original, not null.
     * @return decorated service, never null.
     */
    public static Executor logging(final Executor original) {
        Objects.requireNonNull(original);
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                original.execute(new ExceptionLoggingRunnableDecorator(command));
            }
        };
    }

    /**
     * Wraps given executor so that the original cannot be shot down.
     *
     * @param original the original, not null.
     * @return wrapped executor, never null.
     */
    public static ExecutorService disableShutdown(final ExecutorService original) {
        Objects.requireNonNull(original);
        return new ExecutorService() {
            @Override
            public void shutdown() {
                throw new UnsupportedOperationException("Cannot shutdown");
            }

            @Override
            public List<Runnable> shutdownNow() {
                throw new UnsupportedOperationException("Cannot shutdown");
            }

            @Override
            public boolean isShutdown() {
                return original.isShutdown();
            }

            @Override
            public boolean isTerminated() {
                return original.isTerminated();
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return original.awaitTermination(timeout, unit);
            }

            @Override
            public <T> Future<T> submit(Callable<T> task) {
                return original.submit(task);
            }

            @Override
            public <T> Future<T> submit(Runnable task, T result) {
                return original.submit(task, result);
            }

            @Override
            public Future<?> submit(Runnable task) {
                return original.submit(task);
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
                return original.invokeAll(tasks);
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
                return original.invokeAll(tasks, timeout, unit);
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
                return original.invokeAny(tasks);
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return original.invokeAny(tasks, timeout, unit);
            }

            @Override
            public void execute(Runnable command) {
                original.execute(command);
            }
        };
    }

    /**
     * Creates a new thread factory.
     * @param owner the owning service which owns the threads. not null.
     * @param daemon true if the threads are to be daemon threads, false otherwise.
     * @return the thread factory, not null.
     */
    public static ThreadFactory newThreadFactory(final ServiceName owner, final boolean daemon) {
        Checks.checkNotNull("owner", owner);
        // nie getSimpleName(), inner classy nevracaju asi meno outer triedy
        return new ThreadFactory() {
            private final AtomicInteger id = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                final Thread t = new Thread(r);
                t.setName(owner.getThreadName(id.incrementAndGet()));
                t.setDaemon(daemon);
                return t;
            }
        };
    }
}
