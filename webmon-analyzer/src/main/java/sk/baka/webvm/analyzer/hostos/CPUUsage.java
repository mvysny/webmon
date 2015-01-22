package sk.baka.webvm.analyzer.hostos;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

/**
* @author mvy
*/
public final class CPUUsage implements Serializable {
	/**
	 * Shows the host OS CPU usage. A value of 0..100, 0 when not supported.
	 * If the computer contains more than one CPU, this value is the average
	 * of all CPU usages of all CPUs.
	 */
	public final int cpuAvgUsage;
	/**
	 * Shows the host OS CPU usage. A value of 0..100, 0 when not supported. If
	 * the computer contains more than one CPU/core, this value is the max usage of all
	 * CPU usages of all CPUs/cores.
	 */
	public final int cpuMaxCoreUsage;

	public CPUUsage(int cpuAvgUsage, int cpuMaxCoreUsage) {
        if (cpuAvgUsage < 0 || cpuAvgUsage > 100) {
            throw new IllegalArgumentException("Parameter cpuAvgUsage: invalid value " + cpuAvgUsage + ": must be 0..100");
        }
        if (cpuMaxCoreUsage < 0 || cpuMaxCoreUsage > 100) {
            throw new IllegalArgumentException("Parameter cpuMaxCoreUsage: invalid value " + cpuMaxCoreUsage + ": must be 0..100");
        }
		this.cpuAvgUsage = cpuAvgUsage;
		this.cpuMaxCoreUsage = cpuMaxCoreUsage;
	}

    @Override
    public String toString() {
        return "avg=" + cpuAvgUsage + ", core_max=" + cpuMaxCoreUsage;
    }

    @NotNull
	public static final CPUUsage ZERO = new CPUUsage(0, 0);

    @NotNull
    public static CPUUsage of(int cpuUsage) {
        return CACHE[cpuUsage];
    }

    private static final CPUUsage[] CACHE = new CPUUsage[100];
    static {
        CACHE[0] = ZERO;
        for (int i = 1; i < 100; i++) {
            CACHE[i] = new CPUUsage(i, i);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CPUUsage cpuUsage = (CPUUsage) o;

        if (cpuAvgUsage != cpuUsage.cpuAvgUsage) return false;
        if (cpuMaxCoreUsage != cpuUsage.cpuMaxCoreUsage) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = cpuAvgUsage;
        result = 31 * result + cpuMaxCoreUsage;
        return result;
    }
}
