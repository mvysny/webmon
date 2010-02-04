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
package sk.baka.webvm;

import com.google.inject.Inject;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import sk.baka.webvm.analyzer.HistorySample;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import sk.baka.webvm.analyzer.HistorySampler;
import sk.baka.webvm.analyzer.hostos.Cpu;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;
import sk.baka.webvm.misc.AbstractGraph;
import sk.baka.webvm.misc.BluffGraph;
import sk.baka.webvm.misc.GraphStyle;
import sk.baka.webvm.misc.MgmtUtils;

/**
 * Shows the JVM memory usage and GC CPU usage graphs.
 * @author Martin Vysny
 */
public final class Graphs extends WebVMPage {

    public static final String GRAPH_BORDER = "black";
    public static final int GRAPH_HEIGHT_PX = 120;
    public static final int GRAPH_WIDTH_PX = 300;
    private static final long serialVersionUID = 1L;
    /**
     * The blue color used in graphs.
     */
    public static final String COLOR_BLUE = "#7e43b2";
    /**
     * The brown color used in graphs.
     */
    public static final String COLOR_BROWN = "#ff7f7f";
    /**
     * The darkgrey color used in graphs.
     */
    public static final String COLOR_DARKGREY = "#888888";
    public static final String COLOR_WHITE = "#ffffff";
    public static final String COLOR_GREY = "#999999";

    /**
     * Creates the page instance.
     */
    public Graphs() {
        final List<HistorySample> history = WicketApplication.getInjector().getInstance(HistorySampler.class).getVmstatHistory();
        drawGcCpuUsage(history);
        drawHeap(history);
        drawNonHeap(history);
        drawClassesGraph(history);
        drawThreadsGraph(history);
        drawPhysMem(history);
        drawSwap(history);
        drawHostCpuUsage(history);
    }

    /**
     * Creates the default telemetry graph style.
     * @return the graph style.
     */
    public static GraphStyle newDefaultStyle() {
        final GraphStyle gs = new GraphStyle();
        gs.height = GRAPH_HEIGHT_PX;
        gs.width = GRAPH_WIDTH_PX;
        gs.border = GRAPH_BORDER;
        gs.yLegend = true;
        return gs;
    }

    private void drawClassesGraph(List<HistorySample> history) {
        final GraphStyle gs = newDefaultStyle();
        gs.colors = new String[]{COLOR_BROWN};
        int maxClasses = 0;
        for (final HistorySample hs : history) {
            if (maxClasses < hs.classesLoaded) {
                maxClasses = hs.classesLoaded;
            }
        }
        maxClasses = maxClasses * 5 / 4;
        final BluffGraph dg = new BluffGraph(maxClasses, gs);
        for (final HistorySample hs : history) {
            dg.add(new int[]{hs.classesLoaded});
        }
        dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength(), false);
        unescaped("classesGraph", dg.draw());
        final ClassLoadingMXBean bean = ManagementFactory.getClassLoadingMXBean();
        border.add(new Label("classesCurrentlyLoaded", Integer.toString(bean.getLoadedClassCount())));
        border.add(new Label("classesLoadedTotal", Long.toString(bean.getTotalLoadedClassCount())));
        border.add(new Label("classesUnloadedTotal", Long.toString(bean.getUnloadedClassCount())));
    }
    @Inject
    private IMemoryInfoProvider meminfo;

    private void drawHostCpuUsage(List<HistorySample> history) {
        final boolean hostCpu = Cpu.isHostCpuSupported();
        final boolean javaCpu = Cpu.isJavaCpuSupported();
        final boolean hostIOCpu = Cpu.isHostIOCpuSupported();
        if (hostCpu || javaCpu || hostIOCpu) {
            final GraphStyle gs = newDefaultStyle();
            gs.colors = new String[]{COLOR_BLUE, COLOR_BROWN, COLOR_DARKGREY};
            final AbstractGraph dg = new BluffGraph(100, gs);
            dg.makeAscending = true;
            for (final HistorySample hs : history) {
                dg.add(new int[]{hs.cpuJavaUsage, hs.cpuUsage, hs.cpuIOUsage});
            }
            dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength(), false);
            unescaped("cpuUsageGraph", dg.draw());
            final HistorySample last = history.isEmpty() ? new HistorySample(0, 0, 0, 0, meminfo) : history.get(history.size() - 1);
            border.add(new Label("cpuUsagePerc", printValue(hostCpu, last.cpuUsage)));
            border.add(new Label("javaCpuUsagePerc", printValue(javaCpu, last.cpuJavaUsage)));
            border.add(new Label("iowait", printValue(hostIOCpu, last.cpuIOUsage)));
        } else {
            add(new Label("cpuUsageGraph", "Both HostOS CPU measurement and Java CPU usage measurement are unsupported on this OS/JavaVM"));
            add(new Label("cpuUsagePerc", "-"));
            add(new Label("javaCpuUsagePerc", "-"));
            add(new Label("iowait", "-"));
        }
    }

    private static String printValue(final boolean enabled, final int value) {
        return enabled ? Integer.toString(value) : "?";
    }

    private void drawGcCpuUsage(final List<HistorySample> history) {
        final GraphStyle gs = newDefaultStyle();
        gs.colors = new String[]{COLOR_BLUE};
        final AbstractGraph dg = new BluffGraph(100, gs);
        for (final HistorySample hs : history) {
            dg.add(new int[]{hs.gcCpuUsage});
        }
        dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength(), false);
        unescaped("gcCPUUsageGraph", dg.draw());
        final HistorySample last = history.isEmpty() ? null : history.get(history.size() - 1);
        border.add(new Label("gcCPUUsagePerc", last == null ? "?" : Integer.toString(last.gcCpuUsage)));
    }

    private void drawHeap(final List<HistorySample> history) {
        drawMemoryUsageGraph(history, "heapUsageGraph", HistorySample.POOL_HEAP);
        final MemoryUsage heap = MgmtUtils.getInMB(MgmtUtils.getHeapFromRuntime());
        border.add(new Label("heapUsage", Long.toString(heap.getUsed())));
        border.add(new Label("heapSize", Long.toString(heap.getCommitted())));
    }

    private void drawNonHeap(final List<HistorySample> history) {
        if (MgmtUtils.isNonHeapPool()) {
            drawMemoryUsageGraph(history, "nonHeapUsageGraph", HistorySample.POOL_NON_HEAP);
            final MemoryUsage nonHeap = MgmtUtils.getInMB(MgmtUtils.getNonHeapSummary());
            border.add(new Label("nonHeapUsage", Long.toString(nonHeap.getUsed())));
            border.add(new Label("nonHeapSize", Long.toString(nonHeap.getCommitted())));
        } else {
            border.add(new Label("nonHeapUsageGraph", "No information available"));
            border.add(new Label("nonHeapUsage", "-"));
            border.add(new Label("nonHeapSize", "-"));
        }
    }

    private void drawPhysMem(final List<HistorySample> history) {
        final MemoryUsage physMem = MgmtUtils.getInMB(meminfo.getPhysicalMemory());
        if (physMem != null) {
            drawMemoryUsageGraph(history, "physUsageGraph", HistorySample.POOL_PHYS_MEM);
            border.add(new Label("physCommitted", Long.toString(physMem.getCommitted())));
            border.add(new Label("physUsed", Long.toString(physMem.getUsed())));
        } else {
            border.add(new Label("physUsageGraph", "No information available"));
            border.add(new Label("physCommitted", "-"));
            border.add(new Label("physUsed", "-"));
        }
    }

    private void drawSwap(final List<HistorySample> history) {
        final MemoryUsage swap = MgmtUtils.getInMB(meminfo.getSwap());
        if (swap != null) {
            drawMemoryUsageGraph(history, "swapUsageGraph", HistorySample.POOL_SWAP);
            border.add(new Label("swapUsed", Long.toString(swap.getUsed())));
        } else {
            border.add(new Label("swapUsageGraph", "No information available"));
            border.add(new Label("swapUsed", "-"));
        }
    }

    private void drawThreadsGraph(List<HistorySample> history) {
        final GraphStyle gs = newDefaultStyle();
        gs.colors = new String[]{COLOR_BLUE, COLOR_BROWN};
        int maxThreads = 0;
        for (final HistorySample hs : history) {
            if (maxThreads < hs.threadCount) {
                maxThreads = hs.threadCount;
            }
        }
        maxThreads = maxThreads * 5 / 4;
        final AbstractGraph dg = new BluffGraph(maxThreads, gs);
        for (final HistorySample hs : history) {
            dg.add(new int[]{hs.daemonThreadCount, hs.threadCount});
        }
        dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength(), false);
        unescaped("threadsGraph", dg.draw());
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        border.add(new Label("liveThreads", Integer.toString(bean.getThreadCount())));
        border.add(new Label("daemonThreads", Long.toString(bean.getDaemonThreadCount())));
        border.add(new Label("threadsStartedTotal", Long.toString(bean.getTotalStartedThreadCount())));
    }
}
