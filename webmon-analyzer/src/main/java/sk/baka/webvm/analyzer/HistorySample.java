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
package sk.baka.webvm.analyzer;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import sk.baka.webvm.analyzer.utils.MgmtUtils;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;

/**
 * Holds history data for a single time unit.
 *
 * @author Martin Vysny
 */
public final class HistorySample {

    /**
     * Returns GC CPU Usage.
     *
     * @return average CPU usage of GC for this time slice in percent, 0-100.
     */
    public final int gcCpuUsage;
    /**
     * The time this sample was taken.
     */
    public final long sampleTime;
    /**
     * The {@link MgmtUtils#getHeapFromRuntime() heap usage}.
     */
    public static final int POOL_HEAP = 0;
    /**
     * The {@link MgmtUtils#getNonHeapSummary() non-heap usage}, may be null.
     */
    public static final int POOL_NON_HEAP = 1;
    /**
     * The
     * {@link IMemoryInfoProvider#getPhysicalMemory() host OS physical memory},
     * may be null.
     */
    public static final int POOL_PHYS_MEM = 2;
    /**
     * The {@link IMemoryInfoProvider#getSwap() swap}, may be null.
     */
    public static final int POOL_SWAP = 3;
    /**
     * The memory usage list, indexed according to the value of the
     * <code>POOL_*</code> constants. The values are in MB.
     */
    public final MemoryUsage[] memPoolUsage;
    /**
     * A thread dump. Does not contain any stacktraces.
     */
    public final ThreadMap threads;
    /**
     * Count of classes currently loaded in the VM.
     */
    public final int classesLoaded;
    /**
     * Shows the host OS CPU usage. A value of 0..100, 0 when not supported. If
     * the computer contains more than one CPU, this value is the average of all
     * CPU usages of all CPUs.
     */
    public final int cpuUsage;
    /**
     * Shows the owner java process CPU usage. A value of 0..100, 0 when not
     * supported.
     */
    public final int cpuJavaUsage;
    /**
     * Shows the host OS CPU IO usage. A value of 0..100, 0 when not supported.
     */
    public final int cpuIOUsage;

    private HistorySample(int gcCpuUsage, MemoryUsage[] memPoolUsage, ThreadMap threads, int classesLoaded, int cpuUsage, int cpuJavaUsage, int cpuIOUsage, long sampleTime) {
        this.sampleTime = sampleTime;
        this.gcCpuUsage = gcCpuUsage;
        this.memPoolUsage = memPoolUsage;
        this.threads = threads;
        this.classesLoaded = classesLoaded;
        this.cpuUsage = cpuUsage;
        this.cpuJavaUsage = cpuJavaUsage;
        this.cpuIOUsage = cpuIOUsage;
    }

    /**
     * A mutable builder for the {@link HistorySample}. Serializable, with the exception of {@link #threads} field - the field is not serialized and is null upon deserialization.
     */
    public static class Builder implements Externalizable {
        public Builder copy(HistorySample hs) {
            this.classesLoaded = hs.classesLoaded;
            this.cpuIOUsage = hs.cpuIOUsage;
            this.cpuJavaUsage = hs.cpuJavaUsage;
            this.cpuUsage = hs.cpuUsage;
            this.gcCpuUsage = hs.gcCpuUsage;
            this.memPoolUsage = hs.memPoolUsage;
            this.sampleTime = hs.sampleTime;
            this.threads = hs.threads;
            return this;
        }
        /**
         * The time this sample was taken.
         */
        public long sampleTime = System.currentTimeMillis();
        /**
         * Returns GC CPU Usage.
         *
         * @return average CPU usage of GC for this time slice in percent,
         * 0-100.
         */
        public int gcCpuUsage = 0;
        /**
         * The memory usage list, indexed according to the value of the
         * <code>POOL_*</code> constants. The values are in MB.
         */
        public MemoryUsage[] memPoolUsage = new MemoryUsage[4];
        /**
         * A thread dump. Does not contain any stacktraces. Never null.
         */
        public transient ThreadMap threads = null;
        /**
         * Count of classes currently loaded in the VM.
         */
        public int classesLoaded = 0;
        /**
         * Shows the host OS CPU usage. A value of 0..100, 0 when not supported.
         * If the computer contains more than one CPU, this value is the average
         * of all CPU usages of all CPUs.
         */
        public int cpuUsage = 0;
        /**
         * Shows the owner java process CPU usage. A value of 0..100, 0 when not
         * supported.
         */
        public int cpuJavaUsage = 0;
        /**
         * Shows the host OS CPU IO usage. A value of 0..100, 0 when not
         * supported.
         */
        public int cpuIOUsage = 0;

        public Builder setGcCpuUsage(int gcCpuUsage) {
            this.gcCpuUsage = gcCpuUsage;
            return this;
        }

        public Builder setClassesLoaded(int classesLoaded) {
            this.classesLoaded = classesLoaded;
            return this;
        }

        public Builder setCpuUsage(int cpuUsage) {
            this.cpuUsage = cpuUsage;
            return this;
        }

        public Builder setCpuJavaUsage(int cpuJavaUsage) {
            this.cpuJavaUsage = cpuJavaUsage;
            return this;
        }

        public Builder setCpuIOUsage(int cpuIOUsage) {
            this.cpuIOUsage = cpuIOUsage;
            return this;
        }

        public Builder autodetectMeminfo(IMemoryInfoProvider meminfo) {
            memPoolUsage[POOL_HEAP] = MgmtUtils.getInMB(MgmtUtils.getHeapFromRuntime());
            memPoolUsage[POOL_NON_HEAP] = MgmtUtils.getInMB(MgmtUtils.getNonHeapSummary());
            memPoolUsage[POOL_PHYS_MEM] = MgmtUtils.getInMB(meminfo.getPhysicalMemory());
            memPoolUsage[POOL_SWAP] = MgmtUtils.getInMB(meminfo.getSwap());
            return this;
        }

        public Builder autodetectClassesLoaded() {
            classesLoaded = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
            return this;
        }

        public Builder autodetectMemClassesThreads(IMemoryInfoProvider meminfo) {
            autodetectMeminfo(meminfo);
            autodetectClassesLoaded();
            threads = ThreadMap.takeSnapshot();
            return this;
        }

        public HistorySample build() {
            return new HistorySample(gcCpuUsage, memPoolUsage, threads, classesLoaded, cpuUsage, cpuJavaUsage, cpuIOUsage, sampleTime);
        }

        private static void write(MemoryUsage mu, ObjectOutput out) throws IOException {
            out.writeLong(mu.getInit());
            out.writeLong(mu.getCommitted());
            out.writeLong(mu.getUsed());
            out.writeLong(mu.getMax());
        }
        private static MemoryUsage read(ObjectInput in) throws IOException {
            final long init = in.readLong();
            final long committed = in.readLong();
            final long used = in.readLong();
            final long max = in.readLong();
            return new MemoryUsage(init, used, committed, max);
        }
        
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeLong(sampleTime);
            out.write(gcCpuUsage);
            if (memPoolUsage == null) {
                out.write(0);
            } else {
                out.write(memPoolUsage.length);
                for (MemoryUsage mu: memPoolUsage) {
                    write(mu, out);
                }
            }
            out.writeInt(classesLoaded);
            out.write(cpuUsage);
            out.write(cpuIOUsage);
            out.write(cpuJavaUsage);
        }

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            sampleTime = in.readLong();
            gcCpuUsage = in.read();
            final int mempoolCount = in.read();
            memPoolUsage = new MemoryUsage[mempoolCount];
            for (int i=0;i<mempoolCount;i++){
                memPoolUsage[i] = read(in);
            }
            classesLoaded = in.readInt();
            cpuUsage = in.read();
            cpuIOUsage = in.read();
            cpuJavaUsage = in.read();
        }
    }
}
