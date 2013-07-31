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
package sk.baka.webvm;

import com.google.inject.Inject;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import sk.baka.webvm.analyzer.HistorySample;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.analyzer.HistorySampler;
import sk.baka.webvm.analyzer.IHistorySampler;
import sk.baka.webvm.analyzer.hostos.Cpu;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;
import sk.baka.webvm.misc.AbstractGraph;
import sk.baka.webvm.misc.BluffGraph;
import sk.baka.webvm.misc.GraphStyle;
import sk.baka.webvm.analyzer.utils.MgmtUtils;

/**
 * Shows the JVM memory usage and GC CPU usage graphs.
 *
 * @author Martin Vysny
 */
public class Graphs extends WebVMPage {

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
    @Inject
    private IHistorySampler historySampler;
    private final IModel<List<HistorySample>> history;

    /**
     * Creates the page instance.
     */
    public Graphs() {
        history = register(new LoadableDetachableModel<List<HistorySample>>() {

            @Override
            protected List<HistorySample> load() {
                return historySampler.getVmstatHistory();
            }
        });
        drawGcCpuUsage();
        drawHeap();
        drawNonHeap();
        drawClassesGraph();
        drawThreadsGraph();
        drawPhysMem();
        drawSwap();
        drawHostCpuUsage();
    }

    /**
     * Creates the default telemetry graph style.
     *
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

    private void drawClassesGraph() {
        unescaped("classesGraph", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                final GraphStyle gs = newDefaultStyle();
                gs.colors = new String[]{COLOR_BROWN};
                int maxClasses = 0;
                for (final HistorySample hs : history.getObject()) {
                    if (maxClasses < hs.classesLoaded) {
                        maxClasses = hs.classesLoaded;
                    }
                }
                maxClasses = maxClasses * 5 / 4;
                final BluffGraph dg = new BluffGraph(maxClasses, gs);
                for (final HistorySample hs : history.getObject()) {
                    dg.add(new int[]{hs.classesLoaded});
                }
                dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength(), false);
                return dg.draw();
            }
        });
        border.add(new Label("classesCurrentlyLoaded", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                final ClassLoadingMXBean bean = ManagementFactory.getClassLoadingMXBean();
                return "" + bean.getLoadedClassCount();
            }
        }));
        border.add(new Label("classesLoadedTotal", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                final ClassLoadingMXBean bean = ManagementFactory.getClassLoadingMXBean();
                return "" + bean.getTotalLoadedClassCount();
            }
        }));
        border.add(new Label("classesUnloadedTotal", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                final ClassLoadingMXBean bean = ManagementFactory.getClassLoadingMXBean();
                return "" + bean.getUnloadedClassCount();
            }
        }));
    }
    @Inject
    private IMemoryInfoProvider meminfo;

    private void drawHostCpuUsage() {
        final boolean hostCpu = Cpu.isHostCpuSupported();
        final boolean javaCpu = Cpu.isJavaCpuSupported();
        final boolean hostIOCpu = Cpu.isHostIOCpuSupported();
        if (hostCpu || javaCpu || hostIOCpu) {
            unescaped("cpuUsageGraph", new LoadableDetachableModel<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected String load() {
                    final GraphStyle gs = newDefaultStyle();
                    gs.colors = new String[]{COLOR_BLUE, COLOR_BROWN, COLOR_DARKGREY};
                    final AbstractGraph dg = new BluffGraph(100, gs);
                    dg.makeAscending = true;
                    for (final HistorySample hs : history.getObject()) {
                        dg.add(new int[]{hs.cpuJavaUsage, hs.cpuUsage, hs.cpuIOUsage});
                    }
                    dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength(), false);
                    return dg.draw();
                }
            });
            border.add(new Label("cpuUsagePerc", new LoadableDetachableModel<String>() {

                @Override
                protected String load() {
                    return printValue(hostCpu, getNonEmptyLastSample().cpuUsage);
                }
            }));
            border.add(new Label("javaCpuUsagePerc", new LoadableDetachableModel<String>() {

                @Override
                protected String load() {
                    return printValue(javaCpu, getNonEmptyLastSample().cpuJavaUsage);
                }
            }));
            border.add(new Label("iowait", new LoadableDetachableModel<String>() {

                @Override
                protected String load() {
                    return printValue(hostIOCpu, getNonEmptyLastSample().cpuIOUsage);
                }
            }));
        } else {
            border.add(new Label("cpuUsageGraph", "Both HostOS CPU measurement and Java CPU usage measurement are unsupported on this OS/JavaVM"));
            border.add(new Label("cpuUsagePerc", "-"));
            border.add(new Label("javaCpuUsagePerc", "-"));
            border.add(new Label("iowait", "-"));
        }
    }

    private HistorySample getNonEmptyLastSample() {
        return history.getObject().isEmpty() ? new HistorySample.Builder().autodetectMemClassesThreads(meminfo).build() : history.getObject().get(history.getObject().size() - 1);
    }

    private static String printValue(final boolean enabled, final int value) {
        return enabled ? Integer.toString(value) : "?";
    }

    private void drawGcCpuUsage() {
        unescaped("gcCPUUsageGraph", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                final GraphStyle gs = newDefaultStyle();
                gs.colors = new String[]{COLOR_BLUE};
                final AbstractGraph dg = new BluffGraph(100, gs);
                for (final HistorySample hs : history.getObject()) {
                    dg.add(new int[]{hs.gcCpuUsage});
                }
                dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength(), false);
                return dg.draw();
            }
        });
        border.add(new Label("gcCPUUsagePerc", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                return "" + getNonEmptyLastSample().gcCpuUsage;
            }
        }));
    }

    private void drawHeap() {
        drawMemoryUsageGraph("heapUsageGraph", HistorySample.POOL_HEAP);
        final IModel<MemoryUsage> heap = register(new LoadableDetachableModel<MemoryUsage>() {

            @Override
            protected MemoryUsage load() {
                return MgmtUtils.getInMB(MgmtUtils.getHeapFromRuntime());
            }
        });
        border.add(new Label("heapUsage", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                return "" + heap.getObject().getUsed();
            }
        }));
        border.add(new Label("heapSize", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                return "" + heap.getObject().getCommitted();
            }
        }));
    }

    private void drawNonHeap() {
        if (MgmtUtils.isNonHeapPool()) {
            drawMemoryUsageGraph("nonHeapUsageGraph", HistorySample.POOL_NON_HEAP);
            final IModel<MemoryUsage> nonHeap = register(new LoadableDetachableModel<MemoryUsage>() {

                @Override
                protected MemoryUsage load() {
                    return MgmtUtils.getInMB(MgmtUtils.getNonHeapSummary());
                }
            });
            border.add(new Label("nonHeapUsage", new LoadableDetachableModel<String>() {

                @Override
                protected String load() {
                    return "" + nonHeap.getObject().getUsed();
                }
            }));
            border.add(new Label("nonHeapSize", new LoadableDetachableModel<String>() {

                @Override
                protected String load() {
                    return "" + nonHeap.getObject().getCommitted();
                }
            }));
        } else {
            border.add(new Label("nonHeapUsageGraph", "No information available"));
            border.add(new Label("nonHeapUsage", "-"));
            border.add(new Label("nonHeapSize", "-"));
        }
    }

    private void drawPhysMem() {
        final IModel<MemoryUsage> physMem = register(new LoadableDetachableModel<MemoryUsage>() {

            @Override
            protected MemoryUsage load() {
                return MgmtUtils.getInMB(meminfo.getPhysicalMemory());
            }
        });
        if (physMem.getObject() != null) {
            drawMemoryUsageGraph("physUsageGraph", HistorySample.POOL_PHYS_MEM);
        } else {
            border.add(new Label("physUsageGraph", "No information available"));
        }
        border.add(new Label("physCommitted", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                return physMem.getObject() == null ? "-" : "" + physMem.getObject().getCommitted();
            }
        }));
        border.add(new Label("physUsed", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                return physMem.getObject() == null ? "-" : "" + physMem.getObject().getUsed();
            }
        }));
    }

    private void drawSwap() {
        final IModel<MemoryUsage> swap = register(new LoadableDetachableModel<MemoryUsage>() {

            @Override
            protected MemoryUsage load() {
                return MgmtUtils.getInMB(meminfo.getSwap());
            }
        });
        if (swap.getObject() != null) {
            drawMemoryUsageGraph("swapUsageGraph", HistorySample.POOL_SWAP);
            border.add(new Label("swapUsed", new LoadableDetachableModel<String>() {

                @Override
                protected String load() {
                    return "" + swap.getObject().getUsed();
                }
            }));
        } else {
            border.add(new Label("swapUsageGraph", "No information available"));
            border.add(new Label("swapUsed", "-"));
        }
    }

    private void drawThreadsGraph() {
        unescaped("threadsGraph", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                final GraphStyle gs = newDefaultStyle();
                gs.colors = new String[]{COLOR_BLUE, COLOR_BROWN};
                int maxThreads = 0;
                for (final HistorySample hs : history.getObject()) {
                    if (maxThreads < hs.threads.threadCount) {
                        maxThreads = hs.threads.threadCount;
                    }
                }
                maxThreads = maxThreads * 5 / 4;
                final AbstractGraph dg = new BluffGraph(maxThreads, gs);
                for (final HistorySample hs : history.getObject()) {
                    dg.add(new int[]{hs.threads.daemonThreadCount, hs.threads.threadCount});
                }
                dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength(), false);
                return dg.draw();
            }
        });
        border.add(new Label("liveThreads", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                return "" + bean.getThreadCount();
            }
        }));
        border.add(new Label("daemonThreads", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                return "" + bean.getDaemonThreadCount();
            }
        }));
        border.add(new Label("threadsStartedTotal", new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                return "" + bean.getTotalStartedThreadCount();
            }
        }));
    }

    /**
     * Draws details for given memory usage object line.
     *
     * @param wid chain result with this wicket id
     * @param index the memory usage index to the {@link HistorySample#memUsage}
     * array.
     */
    private void drawMemoryUsageGraph(final String wid, final int index) {
        unescaped(wid, new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                final GraphStyle gs = Graphs.newDefaultStyle();
                gs.colors = new String[]{Graphs.COLOR_BLUE, Graphs.COLOR_BROWN};
                long maxMem = history.getObject().get(0).memPoolUsage[index].getMax();
                if (maxMem == -1) {
                    maxMem = 0;
                    for (final HistorySample hs : history.getObject()) {
                        final MemoryUsage usage = hs.memPoolUsage[index];
                        if (maxMem < usage.getCommitted()) {
                            maxMem = usage.getCommitted();
                        }
                    }
                    maxMem = maxMem * 5 / 4;
                }
                final BluffGraph dg = new BluffGraph((int) maxMem, gs);
                for (final HistorySample hs : history.getObject()) {
                    final MemoryUsage usage = hs.memPoolUsage[index];
                    dg.add(new int[]{(int) usage.getUsed(), (int) usage.getCommitted()});
                }
                dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength(), false);
                return dg.draw();
            }
        });
    }
}
