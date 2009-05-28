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
package sk.baka.webvm.analyzer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simulates a deadlock.
 * @author Martin Vysny
 */
public final class Deadlock {

    private final Lock lock1 = new ReentrantLock();
    private final Lock lock2 = new ReentrantLock();
    private Thread t1 = null;
    private Thread t2 = null;

    /**
     * Simulates a deadlock. The deadlock should be already simulated when the method finishes.
     */
    public void simulate() {
        if (t1 != null || t2 != null) {
            return;
        }
        t1 = new Thread("deadlock1") {

            @Override
            public void run() {
                lock1.tryLock();
                try {
                    Thread.sleep(100);
                    lock2.tryLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                    lock2.unlock();
                } catch (InterruptedException ex) {
                    // okay
                } finally {
                    lock1.unlock();
                }
            }
        };
        t1.setDaemon(true);
        t2 = new Thread("deadlock2") {

            @Override
            public void run() {
                lock2.tryLock();
                try {
                    Thread.sleep(100);
                    lock1.tryLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                    lock1.unlock();
                } catch (InterruptedException ex) {
                    // okay
                } finally {
                    lock2.unlock();
                }
            }
        };
        t2.setDaemon(true);
        t1.start();
        t2.start();
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Deadlock.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Cancels the deadlock, disposing of the threads.
     */
    public void cancel() {
        if (t1 != null && t2 != null) {
            t1.interrupt();
            t2.interrupt();
            try {
                t1.join();
                t1 = null;
            } catch (InterruptedException ex) {
                Logger.getLogger(Deadlock.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                t2.join();
                t2 = null;
            } catch (InterruptedException ex) {
                Logger.getLogger(Deadlock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
