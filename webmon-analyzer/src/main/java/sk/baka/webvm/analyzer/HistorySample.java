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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import sk.baka.webvm.analyzer.utils.MemoryUsages;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;
import sk.baka.webvm.analyzer.hostos.Memory;

/**
 * Holds history data for a single time unit.
 *
 * @author Martin Vysny
 */
public final class HistorySample {

    public static enum MemoryPools {
        /**
         * The {@link MgmtUtils#getHeapFromRuntime() heap usage}.
         */
        Heap("Java Heap"),
        /**
         * The {@link MgmtUtils#getNonHeapSummary() non-heap usage}, may be
         * null.
         */
        NonHeap("Java Non-Heap"),
        /**
         * The
         * {@link IMemoryInfoProvider#getPhysicalMemory() host OS physical memory},
         * may be null.
         */
        PhysMem("OS Memory"),
        /**
         * The {@link IMemoryInfoProvider#getSwap() swap}, may be null.
         */
        Swap("OS Swap");
        public final String displayable;
        private MemoryPools(String displayable) {
            this.displayable = displayable;
        }
    }
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
     * The memory usage list, never null, may be empty. The values are in MB.
     */
    public final Map<MemoryPools, MemoryUsage> memPoolUsage;
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

    private HistorySample(int gcCpuUsage, EnumMap<MemoryPools, MemoryUsage> memPoolUsage, ThreadMap threads, int classesLoaded, int cpuUsage, int cpuJavaUsage, int cpuIOUsage, long sampleTime) {
        this.sampleTime = sampleTime;
        this.gcCpuUsage = gcCpuUsage;
        this.memPoolUsage = Collections.unmodifiableMap(memPoolUsage);
        this.threads = threads;
        this.classesLoaded = classesLoaded;
        this.cpuUsage = cpuUsage;
        this.cpuJavaUsage = cpuJavaUsage;
        this.cpuIOUsage = cpuIOUsage;
    }

    /**
     * A mutable builder for the {@link HistorySample}. Serializable, with the
     * exception of {@link #threads} field - the field is not serialized and is
     * null upon deserialization.
     */
    public static class Builder implements Externalizable {

        public Builder copy(HistorySample hs) {
            this.classesLoaded = hs.classesLoaded;
            this.cpuIOUsage = hs.cpuIOUsage;
            this.cpuJavaUsage = hs.cpuJavaUsage;
            this.cpuUsage = hs.cpuUsage;
            this.gcCpuUsage = hs.gcCpuUsage;
            this.memPoolUsage.clear();
            this.memPoolUsage.putAll(hs.memPoolUsage);
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
         * The memory usage list. The values are in MB.
         */
        public final EnumMap<MemoryPools, MemoryUsage> memPoolUsage = new EnumMap<MemoryPools, MemoryUsage>(MemoryPools.class);
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
            memPoolUsage.put(MemoryPools.Heap, MemoryUsages.getInMB(Memory.getHeapFromRuntime()));
            memPoolUsage.put(MemoryPools.NonHeap, MemoryUsages.getInMB(Memory.getNonHeapSummary()));
            memPoolUsage.put(MemoryPools.PhysMem, MemoryUsages.getInMB(meminfo.getPhysicalMemory()));
            memPoolUsage.put(MemoryPools.Swap, MemoryUsages.getInMB(meminfo.getSwap()));
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

        private static void write(MemoryUsage mu, DataOutput out) throws IOException {
            out.writeLong(mu.getInit());
            out.writeLong(mu.getCommitted());
            out.writeLong(mu.getUsed());
            out.writeLong(mu.getMax());
        }

        private static MemoryUsage read(DataInput in) throws IOException {
            final long init = in.readLong();
            final long committed = in.readLong();
            final long used = in.readLong();
            final long max = in.readLong();
            return new MemoryUsage(init, used, committed, max);
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            writeTo(out);
        }

        public void writeTo(DataOutput out) throws IOException {
            out.writeLong(sampleTime);
            out.writeByte(gcCpuUsage);
            if (memPoolUsage == null) {
                out.writeByte(0);
            } else {
                out.writeByte(memPoolUsage.size());
                for (Map.Entry<MemoryPools, MemoryUsage> mu : memPoolUsage.entrySet()) {
                    out.writeByte(mu.getKey().ordinal());
                    write(mu.getValue(), out);
                }
            }
            out.writeInt(classesLoaded);
            out.writeByte(cpuUsage);
            out.writeByte(cpuIOUsage);
            out.writeByte(cpuJavaUsage);
        }

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            readFrom(in);
        }

        public void readFrom(DataInput in) throws IOException {
            sampleTime = in.readLong();
            gcCpuUsage = in.readByte();
            final int mempoolCount = in.readByte();
            memPoolUsage.clear();
            for (int i = 0; i < mempoolCount; i++) {
                memPoolUsage.put(MemoryPools.values()[in.readByte()], read(in));
            }
            classesLoaded = in.readInt();
            cpuUsage = in.readByte();
            cpuIOUsage = in.readByte();
            cpuJavaUsage = in.readByte();
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HistorySample other = (HistorySample) obj;
        if (this.gcCpuUsage != other.gcCpuUsage) {
            return false;
        }
        if (!this.memPoolUsage.keySet().equals(other.memPoolUsage.keySet())) {
            return false;
        }
        for (MemoryPools pool: memPoolUsage.keySet()) {
            if (!equals(memPoolUsage.get(pool), other.memPoolUsage.get(pool))) {
                return false;
            }
        }
        if (this.classesLoaded != other.classesLoaded) {
            return false;
        }
        if (this.cpuUsage != other.cpuUsage) {
            return false;
        }
        if (this.cpuJavaUsage != other.cpuJavaUsage) {
            return false;
        }
        if (this.cpuIOUsage != other.cpuIOUsage) {
            return false;
        }
        return true;
    }
    
    private static boolean equals(MemoryUsage m1, MemoryUsage m2) {
        return m1.getCommitted() == m2.getCommitted() &&
                m1.getInit() == m2.getInit() &&
                m1.getMax() == m2.getMax() &&
                m1.getUsed() == m2.getUsed();
    }

    @Override
    public String toString() {
        return "HistorySample{" + "gcCpuUsage=" + gcCpuUsage + ", sampleTime=" + sampleTime + ", memPoolUsage=" + memPoolUsage + ", classesLoaded=" + classesLoaded + ", cpuUsage=" + cpuUsage + ", cpuJavaUsage=" + cpuJavaUsage + ", cpuIOUsage=" + cpuIOUsage + '}';
    }
}
