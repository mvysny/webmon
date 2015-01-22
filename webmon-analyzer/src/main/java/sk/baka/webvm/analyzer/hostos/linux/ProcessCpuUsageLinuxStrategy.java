/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * WebMon is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * WebMon. If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.analyzer.hostos.linux;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.baka.webvm.analyzer.hostos.CPUUsage;
import sk.baka.webvm.analyzer.hostos.ICpuUsageMeasureStrategy;

/**
 *
 * @author Martin Vysny
 */
public class ProcessCpuUsageLinuxStrategy implements ICpuUsageMeasureStrategy {
    public final int pid;

    public ProcessCpuUsageLinuxStrategy(int pid) {
        this.pid = pid;
    }

    public static boolean isAvailable() {
        return Proc.Stat.isAvailable();
    }

    public Object measure() throws Exception {
        return StatWithTime.now(pid);
    }

    public CPUUsage getAvgCpuUsage(Object m1, Object m2) {
        final int u = ((StatWithTime) m2).getCpuUsage((StatWithTime) m1);
        return CPUUsage.of(u);
    }
    private static final class StatWithTime {
        /**
         * may be null.
         */
        @Nullable
        public final Proc.PidStat stat;
        /**
         * may be null.
         */
        @Nullable
        public final Proc.Stats procStat;
        public StatWithTime(@Nullable Proc.PidStat stat, @Nullable Proc.Stats procStat) {
            this.stat = stat;
            this.procStat = procStat;
        }
        @NotNull
        public static StatWithTime now(int pid) {
            return new StatWithTime(Proc.PidStat.now(pid), Proc.Stat.now());
        }
        public int getCpuUsage(@NotNull StatWithTime prev) {
            if (prev == null || stat == null || procStat == null) {
                return 0;
            }
            final long du = stat.utimeJiffies - prev.stat.utimeJiffies;
            final long ds = stat.stimeJiffies - prev.stat.stimeJiffies;
            final long dt = procStat.overall.getTotal() - prev.procStat.overall.getTotal();
            if (du < 0 || ds < 0 || dt < 0) {
                throw new IllegalArgumentException("Parameter prev: invalid value " + prev + ": does not precede this: " + this);
            }
            if (dt == 0) {
                return 0;
            }
            final int user = (int) (100L * Runtime.getRuntime().availableProcessors() * du / dt);
            final int sys = (int) (100L * Runtime.getRuntime().availableProcessors() * ds / dt);
            final int sum = user + sys;
            return sum;
        }

        @Override
        public String toString() {
            return "StatWithTime{" + "stat=" + stat + ", procStat=" + procStat + '}';
        }
    }
}
