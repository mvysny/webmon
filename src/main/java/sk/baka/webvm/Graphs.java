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
package sk.baka.webvm;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import sk.baka.webvm.analyzer.HistorySample;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import sk.baka.webvm.analyzer.HistorySampler;
import sk.baka.webvm.misc.DivGraph;
import sk.baka.webvm.misc.GraphStyle;
import sk.baka.webvm.misc.MgmtUtils;

/**
 * Shows the JVM memory usage and GC CPU usage graphs.
 * @author Martin Vysny
 */
public final class Graphs extends WebVMPage {

    /**
     * Creates the page instance.
     */
    public Graphs() {
        final List<HistorySample> history = WicketApplication.getHistory().getVmstatHistory();
        drawGcCpuUsage(history);
        drawMemoryUsageGraph(history, "heapUsageGraph", 0);
        final MemoryUsage heap = MgmtUtils.getInMB(MgmtUtils.getHeapFromRuntime());
        border.add(new Label("heapUsage", Long.toString(heap.getUsed()) + "M"));
        border.add(new Label("heapSize", Long.toString(heap.getCommitted()) + "M"));
        if (MgmtUtils.isNonHeapPool()) {
            drawMemoryUsageGraph(history, "nonHeapUsageGraph", 1);
            final MemoryUsage nonHeap = MgmtUtils.getInMB(MgmtUtils.getNonHeapSummary());
            border.add(new Label("nonHeapUsage", Long.toString(nonHeap.getUsed()) + "M"));
            border.add(new Label("nonHeapSize", Long.toString(nonHeap.getCommitted()) + "M"));
        } else {
            border.add(new Label("nonHeapUsageGraph", "No information available"));
            border.add(new Label("nonHeapUsage", "-"));
            border.add(new Label("nonHeapSize", "-"));
        }
        drawClassesGraph(history);
        drawThreadsGraph(history);
    }

    private void drawClassesGraph(List<HistorySample> history) {
        final GraphStyle gs = new GraphStyle();
        gs.colors = new String[]{"#ff7f7f"};
        gs.height = 100;
        gs.width = 2;
        gs.border = "black";
        gs.yLegend = true;
        int maxClasses = 0;
        for (final HistorySample hs : history) {
            if (maxClasses < hs.classesLoaded) {
                maxClasses = hs.classesLoaded;
            }
        }
        maxClasses = maxClasses * 5 / 4;
        final DivGraph dg = new DivGraph(maxClasses, gs);
        for (final HistorySample hs : history) {
            dg.add(new int[]{hs.classesLoaded});
        }
        dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength());
        // TODO draw the graph directly to a writer
        unescaped("classesGraph", dg.draw());
        final ClassLoadingMXBean bean = ManagementFactory.getClassLoadingMXBean();
        border.add(new Label("classesCurrentlyLoaded", Integer.toString(bean.getLoadedClassCount())));
        border.add(new Label("classesLoadedTotal", Long.toString(bean.getTotalLoadedClassCount())));
        border.add(new Label("classesUnloadedTotal", Long.toString(bean.getUnloadedClassCount())));
    }

    private void drawGcCpuUsage(final List<HistorySample> history) {
        final GraphStyle gs = new GraphStyle();
        gs.colors = new String[]{"#7e43b2"};
        gs.height = 100;
        gs.width = 2;
        gs.border = "black";
        gs.yLegend = true;
        final DivGraph dg = new DivGraph(100, gs);
        for (final HistorySample hs : history) {
            dg.add(new int[]{hs.gcCpuUsage});
        }
        dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength());
        // TODO draw the graph directly to a writer
        unescaped("gcCPUUsageGraph", dg.draw());
        final HistorySample last = history.isEmpty() ? null : history.get(history.size() - 1);
        border.add(new Label("gcCPUUsagePerc", last == null ? "-" : Integer.toString(last.gcCpuUsage)));
    }

    private void drawThreadsGraph(List<HistorySample> history) {
        final GraphStyle gs = new GraphStyle();
        gs.colors = new String[]{"#7e43b2", "#ff7f7f"};
        gs.height = 100;
        gs.width = 2;
        gs.border = "black";
        gs.yLegend = true;
        int maxThreads = 0;
        for (final HistorySample hs : history) {
            if (maxThreads < hs.threadCount) {
                maxThreads = hs.threadCount;
            }
        }
        maxThreads = maxThreads * 5 / 4;
        final DivGraph dg = new DivGraph(maxThreads, gs);
        for (final HistorySample hs : history) {
            dg.add(new int[]{hs.daemonThreadCount, hs.threadCount});
        }
        dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength());
        // TODO draw the graph directly to a writer
        unescaped("threadsGraph", dg.draw());
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        border.add(new Label("liveThreads", Integer.toString(bean.getThreadCount())));
        border.add(new Label("daemonThreads", Long.toString(bean.getDaemonThreadCount())));
    }
}
