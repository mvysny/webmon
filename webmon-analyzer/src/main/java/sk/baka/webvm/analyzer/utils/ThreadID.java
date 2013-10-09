package sk.baka.webvm.analyzer.utils;

import java.io.Serializable;

/**
 * Represents a thread ID.
 * @author Martin Vysny
 */
public final class ThreadID implements Serializable, Comparable<ThreadID> {
    /**
     * A thread ID. See {@link Thread#getId()} for details.
     */
    public final long id;

    public ThreadID(long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return (int) (this.id ^ (this.id >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ThreadID other = (ThreadID) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(id);
    }
    
    public static ThreadID of(Thread t) {
        return new ThreadID(t.getId());
    }

    public int compareTo(ThreadID o) {
        return id < o.id ? -1 : id == o.id ? 0 : 1;
    }
}
