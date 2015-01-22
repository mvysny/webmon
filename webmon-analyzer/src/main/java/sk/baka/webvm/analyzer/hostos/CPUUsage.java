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
		this.cpuAvgUsage = cpuAvgUsage;
		this.cpuMaxCoreUsage = cpuMaxCoreUsage;
	}

    @Override
    public String toString() {
        return "cpuAvgUsage=" + cpuAvgUsage + ", cpuMaxCoreUsage=" + cpuMaxCoreUsage;
    }

    @NotNull
	public static final CPUUsage ZERO = new CPUUsage(0, 0);
}
