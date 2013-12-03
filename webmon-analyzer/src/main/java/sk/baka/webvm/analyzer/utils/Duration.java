/**
 * Copyright (c) 2011 Martin Vysny, All rights reserved
 */
package sk.baka.webvm.analyzer.utils;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Represents a duration in milliseconds.
 *
 * @author Martin Vysny
 */
public final class Duration implements Serializable, Comparable<Duration> {

    private static final long serialVersionUID = 1L;
    /**
     * Duration, in milliseconds.
     */
    public final long millis;

    public int compareTo(Duration o) {
        return Long.valueOf(millis).compareTo(Long.valueOf(o.millis));
    }

    public static Duration millis(long millis) {
        return new Duration(millis);
    }
    
    private Duration(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Parameter millis: invalid value " + millis + ": must not be negative");
        }
        this.millis = millis;
    }

    public Duration(long duration, TimeUnit unit) {
        this(unit.toMillis(duration));
    }

    public static Duration days(long days) {
        return Duration.hours(days * 24);
    }

    public static Duration weeks(long days) {
        return Duration.days(days * 7);
    }

    public static Duration of(long t1, long t2) {
        return new Duration(Math.abs(t1 - t2));
    }

    public static Duration fromNow(long t) {
        return of(System.currentTimeMillis(), t);
    }

    public static Duration fromNow(Date t) {
        return fromNow(t.getTime());
    }

    /**
     * Checks if this duration is longer or equal than given duration.
     *
     * @param duration duration, in milliseconds.
     * @return true if this duration is longer or equal.
     */
    public boolean isLongerThan(long duration) {
        return this.millis >= duration;
    }

    public boolean isLongerThan(Duration duration) {
        return isLongerThan(duration.millis);
    }

    /**
     * Checks if this duration is shorter than given duration.
     *
     * @param duration duration, in milliseconds.
     * @return true if this duration is shorter.
     */
    public boolean isShorterThan(long duration) {
        return this.millis < duration;
    }

    public boolean isShorterThan(Duration duration) {
        return isShorterThan(duration.millis);
    }

    public int getDurationInt() {
        return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Duration other = (Duration) obj;
        if (this.millis != other.millis) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (int) (this.millis ^ (this.millis >>> 32));
        return hash;
    }

    @Override
    public String toString() {
        return millis + "ms";
    }
    public static final Duration FOREVER = new Duration(Long.MAX_VALUE);

    public long toMinutes() {
        return toSeconds() / 60;
    }

    public long toSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(millis);
    }

    public boolean isEmpty() {
        return millis == 0;
    }

    public static Duration minutes(long minutes) {
        return Duration.seconds(minutes * 60L);
    }

    public static Duration hours(long hours) {
        return Duration.minutes(hours * 60L);
    }

    public static Duration seconds(long i) {
        return new Duration(i, TimeUnit.SECONDS);
    }
}
