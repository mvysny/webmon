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
package sk.baka.webvm.misc;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes various tasks in background thread.
 * @author Martin Vysny
 */
public abstract class BackgroundService {
    /**
     * A short name of the service - used in thread names.
     */
    protected final String name;
    /**
     * Maximum thread pool size.
     */
    protected final int maxThreads;

    /**
     * Creates new service instance.
     * @param name A short name of the service - used in thread names.
     * @param maxThreads Maximum thread pool size.
     */
    protected BackgroundService(final String name, final int maxThreads) {
        this.name = name;
        this.maxThreads = maxThreads;
    }

    /**
     * Starts the sampling process in a background thread.
     */
    public synchronized void start() {
        if (executor != null) {
            throw new IllegalStateException("Already started.");
        }
        executor = new ScheduledThreadPoolExecutor(1, newDaemonFactory(name));
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
    protected synchronized final ScheduledExecutorService getExecutor() {
        return executor;
    }

    /**
     * Disposes of this sampler. This instance is no longer usable and cannot be started again.
     */
    public final synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
            stopped();
        }
    }

    /**
     * Invoked after the service has been stopped.
     */
    protected abstract void stopped();

    /**
     * Creates a new factory which creates daemon threads using the {@link Executors#defaultThreadFactory()}.
     * @param name create threads marked with this name.
     * @return a daemon thread factory.
     */
    public static ThreadFactory newDaemonFactory(final String name) {
        return new ThreadFactory() {

            private final ThreadFactory def = Executors.defaultThreadFactory();
            private final AtomicInteger threadNum = new AtomicInteger(0);

            public Thread newThread(Runnable r) {
                final Thread result = def.newThread(r);
                result.setName("WebMon: " + name + "-" + threadNum.incrementAndGet());
                result.setDaemon(true);
                return result;
            }
        };
    }
}
