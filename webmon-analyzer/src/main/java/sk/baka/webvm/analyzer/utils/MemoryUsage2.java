package sk.baka.webvm.analyzer.utils;

import static sk.baka.webvm.analyzer.utils.Constants.HUNDRED_PERCENT;
import static sk.baka.webvm.analyzer.utils.Constants.MEBIBYTES;

import java.io.Serializable;
import java.lang.management.MemoryUsage;

import javax.management.openmbean.CompositeData;

import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import sun.management.MemoryUsageCompositeData;

/**
 * A <tt>MemoryUsage</tt> object represents a snapshot of memory usage.
 * Instances of the <tt>MemoryUsage</tt> class are usually constructed
 * by methods that are used to obtain memory usage
 * information about individual memory pool of the Java virtual machine or
 * the heap or non-heap memory of the Java virtual machine as a whole.
 *
 * <p> A <tt>MemoryUsage</tt> object contains four values:
 * <table summary="Describes the MemoryUsage object content">
 * <tr>
 * <td valign=top> <tt>init</tt> </td>
 * <td valign=top> represents the initial amount of memory (in bytes) that
 *      the Java virtual machine requests from the operating system
 *      for memory management during startup.  The Java virtual machine
 *      may request additional memory from the operating system and
 *      may also release memory to the system over time.
 *      The value of <tt>init</tt> may be undefined.
 * </td>
 * </tr>
 * <tr>
 * <td valign=top> <tt>used</tt> </td>
 * <td valign=top> represents the amount of memory currently used (in bytes).
 * </td>
 * </tr>
 * <tr>
 * <td valign=top> <tt>committed</tt> </td>
 * <td valign=top> represents the amount of memory (in bytes) that is
 *      guaranteed to be available for use by the Java virtual machine.
 *      The amount of committed memory may change over time (increase
 *      or decrease).  The Java virtual machine may release memory to
 *      the system and <tt>committed</tt> could be less than <tt>init</tt>.
 *      <tt>committed</tt> will always be greater than
 *      or equal to <tt>used</tt>.
 * </td>
 * </tr>
 * <tr>
 * <td valign=top> <tt>max</tt> </td>
 * <td valign=top> represents the maximum amount of memory (in bytes)
 *      that can be used for memory management. Its value may be undefined.
 *      The maximum amount of memory may change over time if defined.
 *      The amount of used and committed memory will always be less than
 *      or equal to <tt>max</tt> if <tt>max</tt> is defined.
 *      A memory allocation may fail if it attempts to increase the
 *      used memory such that <tt>used &gt; committed</tt> even
 *      if <tt>used &lt;= max</tt> would still be true (for example,
 *      when the system is low on virtual memory).
 * </td>
 * </tr>
 * </table>
 *
 * Below is a picture showing an example of a memory pool:
 *
 * <pre>
 *        +----------------------------------------------+
 *        +////////////////           |                  +
 *        +////////////////           |                  +
 *        +----------------------------------------------+
 *
 *        |--------|
 *           init
 *        |---------------|
 *               used
 *        |---------------------------|
 *                  committed
 *        |----------------------------------------------|
 *                            max
 * </pre>
 *
 * <h3>MXBean Mapping</h3>
 * <tt>MemoryUsage</tt> is mapped to a {@link javax.management.openmbean.CompositeData CompositeData}
 * with attributes as specified in the {@link #from from} method.
 *
 * @author   Mandy Chung
 * @since   1.5
 */
public final class MemoryUsage2 implements Serializable {
	private final long init;
	private final long used;
	private final long committed;
	private final long max;

	/**
	 * Constructs a <tt>MemoryUsage</tt> object.
	 *
	 * @param init      the initial amount of memory in bytes that
	 *                  the Java virtual machine allocates;
	 *                  or <tt>-1</tt> if undefined.
	 * @param used      the amount of used memory in bytes.
	 * @param committed the amount of committed memory in bytes.
	 * @param max       the maximum amount of memory in bytes that
	 *                  can be used; or <tt>-1</tt> if undefined.
	 *
	 * @throws IllegalArgumentException if
	 * <ul>
	 * <li> the value of <tt>init</tt> or <tt>max</tt> is negative
	 *      but not <tt>-1</tt>; or</li>
	 * <li> the value of <tt>used</tt> or <tt>committed</tt> is negative;
	 *      or</li>
	 * <li> <tt>used</tt> is greater than the value of <tt>committed</tt>;
	 *      or</li>
	 * <li> <tt>committed</tt> is greater than the value of <tt>max</tt>
	 *      <tt>max</tt> if defined.</li>
	 * </ul>
	 */
	public MemoryUsage2(long init,
					   long used,
					   long committed,
					   long max) {
		if (init < -1) {
			throw new IllegalArgumentException( "init parameter = " +
					init + " is negative but not -1.");
		}
		if (max < -1) {
			throw new IllegalArgumentException( "max parameter = " +
					max + " is negative but not -1.");
		}
		if (used < 0) {
			throw new IllegalArgumentException( "used parameter = " +
					used + " is negative.");
		}
		if (committed < 0) {
			throw new IllegalArgumentException( "committed parameter = " +
					committed + " is negative.");
		}
		if (used > committed) {
			throw new IllegalArgumentException( "used = " + used +
					" should be <= committed = " + committed);
		}
		if (max >= 0 && committed > max) {
			throw new IllegalArgumentException( "committed = " + committed +
					" should be < max = " + max);
		}

		this.init = init;
		this.used = used;
		this.committed = committed;
		this.max = max;
	}

	/**
	 * Returns the amount of memory in bytes that the Java virtual machine
	 * initially requests from the operating system for memory management.
	 * This method returns <tt>-1</tt> if the initial memory size is undefined.
	 *
	 * @return the initial size of memory in bytes;
	 * <tt>-1</tt> if undefined.
	 */
	public long getInit() {
		return init;
	}

	/**
	 * Returns the amount of used memory in bytes.
	 *
	 * @return the amount of used memory in bytes.
	 *
	 */
	public long getUsed() {
		return used;
	};

	/**
	 * Returns the amount of memory in bytes that is committed for
	 * the Java virtual machine to use.  This amount of memory is
	 * guaranteed for the Java virtual machine to use.
	 *
	 * @return the amount of committed memory in bytes.
	 *
	 */
	public long getCommitted() {
		return committed;
	};

	/**
	 * Returns the maximum amount of memory in bytes that can be
	 * used for memory management.  This method returns <tt>-1</tt>
	 * if the maximum memory size is undefined.
	 *
	 * <p> This amount of memory is not guaranteed to be available
	 * for memory management if it is greater than the amount of
	 * committed memory.  The Java virtual machine may fail to allocate
	 * memory even if the amount of used memory does not exceed this
	 * maximum size.
	 *
	 * @return the maximum amount of memory in bytes;
	 * <tt>-1</tt> if undefined.
	 */
	public long getMax() {
		return max;
	};

	/**
	 * Returns a descriptive representation of this memory usage.
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("init = " + init + "(" + (init >> 10) + "K) ");
		buf.append("used = " + used + "(" + (used >> 10) + "K) ");
		buf.append("committed = " + committed + "(" +
				(committed >> 10) + "K) " );
		buf.append("max = " + max + "(" + (max >> 10) + "K)");
		return buf.toString();
	}

	/**
	 * Formats a memory usage instance to a compact string. Uses the following format: [used (committed) / max].
	 * @param inMegs if true then given memory usage values are already megabytes. If false then the values are in bytes.
	 * @return [used (committed) / max], or [not available] if null was given
	 */
	@NotNull
	public String toString(final boolean inMegs) {
		final StringBuilder sb = new StringBuilder();
		sb.append('[');
		sb.append(getUsed());
		if (inMegs) {
			sb.append("M");
		}
		sb.append(" (");
		sb.append(getCommitted());
		if (inMegs) {
			sb.append("M");
		}
		sb.append(")");
		if (getMax() >= 0) {
			sb.append(" / ");
			sb.append(getMax());
			if (inMegs) {
				sb.append("M");
			}
			sb.append(" - ");
			if (getMax() > 0) {
				sb.append(getUsed() * HUNDRED_PERCENT / getMax());
			} else {
				sb.append('0');
			}
			sb.append('%');
		} else {
			sb.append(" / ?");
		}
		sb.append(']');
		return sb.toString();
	}

	@Nullable
	public static MemoryUsage2 from(@Nullable MemoryUsage mu) {
		return mu == null ? null : new MemoryUsage2(mu.getInit(), mu.getUsed(), mu.getCommitted(), mu.getMax());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MemoryUsage2 that = (MemoryUsage2) o;

		if (committed != that.committed) return false;
		if (init != that.init) return false;
		if (max != that.max) return false;
		if (used != that.used) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (int) (init ^ (init >>> 32));
		result = 31 * result + (int) (used ^ (used >>> 32));
		result = 31 * result + (int) (committed ^ (committed >>> 32));
		result = 31 * result + (int) (max ^ (max >>> 32));
		return result;
	}

	/**
	 * Sums two memory usages together. Does not allow null values.
	 * @param other other usage, must not be null
	 * @return a summed usage, never null
	 */
	@NotNull
	public MemoryUsage2 add(@NotNull final MemoryUsage2 other) {
		return new MemoryUsage2(addMem(getInit(), other.getInit()), getUsed() + other.getUsed(), getCommitted() + other.getCommitted(),
				addMem(getMax(), other.getMax()));
	}

	private static long addMem(final long l1, final long l2) {
		if (l1 < 0 || l2 < 0) {
			return -1;
		}
		return l1 + l2;
	}

	@NotNull
	public static final MemoryUsage2 ZERO = new MemoryUsage2(0, 0, 0, 0);

	/**
	 * Returns memory usage in the following format: xx%
	 *
	 * @param mu the memory usage object, may be null
	 * @return formatted percent value; "not available" if the object is null or
	 * max is -1; "none" if max is zero
	 */
	@NotNull
	public static String getUsagePerc(@Nullable final MemoryUsage2 mu) {
		return mu == null ? "?" : mu.getUsagePerc();
	}
	/**
	 * Returns memory usage in the following format: xx%
	 * @return formatted percent value; "not available" if the object is null or
	 * max is -1; "none" if max is zero
	 */
	@NotNull
	public String getUsagePerc() {
		if (getMax() < 0) {
			return "?";
		}
		if (getMax() == 0) {
			return "0";
		}
		return "" + (getUsed() * Constants.HUNDRED_PERCENT / getMax());
	}

	/**
	 * Returns a new object with all values divided by 1024*1024 (converted from bytes to mebibytes).
	 * @param mu the memory usage to convert
	 * @return new memory object with values in mebibytes. Returns null if null was supplied.
	 */
	@Nullable
	public static MemoryUsage2 getInMB(@Nullable final MemoryUsage2 mu) {
		return mu == null ? null : mu.getInMB();
	}

	/**
	 * Returns a new object with all values divided by 1024*1024 (converted from bytes to mebibytes).
	 * @return new memory object with values in mebibytes. Returns null if null was supplied.
	 */
	@NotNull
	public MemoryUsage2 getInMB() {
		return new MemoryUsage2(getInit() == -1 ? -1 : getInit() / MEBIBYTES,
				getUsed() / MEBIBYTES,
				getCommitted() / MEBIBYTES,
				getMax() == -1 ? -1 : getMax() / MEBIBYTES);
	}
}
