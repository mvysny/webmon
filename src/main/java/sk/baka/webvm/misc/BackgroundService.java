/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.baka.webvm.misc;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import sk.baka.webvm.misc.NotificationDelivery;

/**
 * Executes various tasks in background thread.
 * @author Martin Vysny
 */
public abstract class BackgroundService {

    protected final int maxThreads;

    protected BackgroundService(final int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * Starts the sampling process in a background thread.
     */
    public synchronized void start() {
        if (executor != null) {
            throw new IllegalStateException("Already started.");
        }
        executor = new ScheduledThreadPoolExecutor(1, newDaemonFactory());
        started(executor);
    }
    private ScheduledExecutorService executor = null;

    /**
     * Invoked when the service is started.
     * @param executor executes tasks.
     */
    protected abstract void started(final ScheduledExecutorService executor);

    /**
     * Returns the executor.
     * @return the executor. null if not started.
     */
    protected final ScheduledExecutorService getExecutor() {
        return executor;
    }

    /**
     * Disposes of this sampler. This instance is no longer usable and cannot be started again.
     */
    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
            stopped();
        }
    }

    /**
     * The service is stopped.
     */
    protected abstract void stopped();

    /**
     * Creates a new factory which creates daemon threads using the {@link Executors#defaultThreadFactory()}.
     * @return a daemon thread factory.
     */
    public static ThreadFactory newDaemonFactory() {
        return new ThreadFactory() {

            private final ThreadFactory def = Executors.defaultThreadFactory();

            public Thread newThread(Runnable r) {
                final Thread result = def.newThread(r);
                result.setDaemon(true);
                return result;
            }
        };
    }
}