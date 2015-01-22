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

import com.google.inject.Provider;
import java.io.Serializable;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;
import sk.baka.webvm.misc.DivGraph;
import sk.baka.webvm.analyzer.utils.MemoryUsages;

/**
 * Shows detailed memory information.
 *
 * @author Martin Vysny
 */
public final class Memory extends WebVMPage {

    private static final long serialVersionUID = 1L;
    private static final int GRAPH_WIDTH_PIXELS = 300;

    /**
     * Creates new object instance
     */
    public Memory() {
        final IModel<MemoryUsage> heap = new LoadableDetachableModel<MemoryUsage>() {

            @Override
            protected MemoryUsage load() {
                return MemoryUsages.getInMB(sk.baka.webvm.analyzer.hostos.Memory.getHeapFromRuntime());
            }
        };
        drawMemoryStatus(heap, "heapStatusBar", GRAPH_WIDTH_PIXELS);
        addStatusBar("heapStatusText", heap);
        final IMemoryInfoProvider meminfo = WicketApplication.getInjector().getInstance(IMemoryInfoProvider.class);
        final IModel<MemoryUsage> physical = new LoadableDetachableModel<MemoryUsage>() {

            @Override
            protected MemoryUsage load() {
                return MemoryUsages.getInMB(meminfo.getPhysicalMemory());
            }
        };
        drawMemoryStatus(physical, "physicalMemoryStatusBar", GRAPH_WIDTH_PIXELS);
        addStatusBar("physicalMemoryStatusText", physical);
        final IModel<MemoryUsage> swap = new LoadableDetachableModel<MemoryUsage>() {

            @Override
            protected MemoryUsage load() {
                return MemoryUsages.getInMB(meminfo.getSwap());
            }
        };
        drawMemoryStatus(swap, "swapStatusBar", GRAPH_WIDTH_PIXELS);
        addStatusBar("swapStatusText", swap);
        addMemoryPoolInfo(border, new MemoryBeansProducer(false), "memoryManagers", "memManName", "memManValid", "memManProperties");
        addMemoryPoolInfo(border, new MemoryBeansProducer(true), "gc", "gcName", "gcValid", "gcProperties");
        addDetailedMemoryPoolInfo(border);
        addGCStats();
    }

    private void addStatusBar(String id, final IModel<MemoryUsage> model) {
        register(model);
        border.add(new Label(id, new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                return MemoryUsages.toString(model.getObject(), true);
            }
        }));
    }

    private void addGCStats() {
        // garbage collections
        int collectors = 0;
        long collections = 0;
        long collectTime = 0;
        final List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        if (beans != null) {
            for (final GarbageCollectorMXBean bean : beans) {
                if (bean.isValid()) {
                    collectors++;
                }
                if (bean.getCollectionCount() > 0) {
                    collections += bean.getCollectionCount();
                }
                if (bean.getCollectionTime() > 0) {
                    collectTime += bean.getCollectionTime();
                }
            }
        }
        border.add(new Label("gcCount", Long.toString(collectors)));
        border.add(new Label("gcAmount", Long.toString(collections)));
        border.add(new Label("gcTime", Long.toString(collectTime)));
    }

    private static void addDetailedMemoryPoolInfo(final AppBorder border) {
        final IModel<List<MemoryPoolMXBean>> model = new LoadableDetachableModel<List<MemoryPoolMXBean>>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<MemoryPoolMXBean> load() {
                return ManagementFactory.getMemoryPoolMXBeans();
            }
        };
        border.add(new MemoryPoolDetailListView("memoryPool", model));
    }

    private static void addMemoryPoolInfo(final AppBorder border, final Provider<? extends List<? extends MemoryManagerMXBean>> memoryBeansProducer, final String listId, final String nameId, final String validId, final String propsId) {
        final IModel<List<MemoryManagerMXBean>> model = new LoadableDetachableModel<List<MemoryManagerMXBean>>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<MemoryManagerMXBean> load() {
                final List<? extends MemoryManagerMXBean> beans = memoryBeansProducer.get();
                return beans != null ? new ArrayList<MemoryManagerMXBean>(beans) : Collections.<MemoryManagerMXBean>emptyList();
            }
        };
        border.add(new MemoryPoolListView(listId, model, nameId, validId, propsId));
    }

    /**
     * Shows a list of detailed information about memory pools.
     */
    private static class MemoryPoolDetailListView extends ListView<MemoryPoolMXBean> {

        public MemoryPoolDetailListView(String id, IModel<? extends List<? extends MemoryPoolMXBean>> model) {
            super(id, model);
        }
        private static final int MEMSTAT_GRAPH_WIDTH = 200;
        private static final long serialVersionUID = 1L;

        @Override
        protected void populateItem(ListItem<MemoryPoolMXBean> item) {
            final MemoryPoolMXBean bean = item.getModelObject();
            item.add(new Label("poolName", bean.getName()));
            item.add(new Label("poolType", "" + bean.getType()));
            item.add(new Label("poolValid", bean.isValid() ? "Y" : "N"));
            MemoryUsage usage = MemoryUsages.getInMB(bean.getCollectionUsage());
            add(item, "poolCollects", usage, true);
            item.add(new Label("poolCollectsPerc", MemoryUsages.getUsagePerc(usage)));
            usage = MemoryUsages.getInMB(bean.getPeakUsage());
            add(item, "poolPeak", usage, false);
            item.add(new Label("poolPeakPerc", MemoryUsages.getUsagePerc(usage)));
            usage = MemoryUsages.getInMB(bean.getUsage());
            add(item, "poolUsage", usage, false);
            item.add(new Label("poolUsagePerc", MemoryUsages.getUsagePerc(usage)));
        }

        private void add(final ListItem<?> item, final String wid, MemoryUsage usage, final boolean gc) {
            if (usage == null && gc) {
                item.add(new Label(wid, "Not collectable"));
                return;
            }
            final StringBuilder sb = new StringBuilder();
            if (usage != null) {
                sb.append(DivGraph.drawMemoryStatus(usage, MEMSTAT_GRAPH_WIDTH));
            }
            sb.append(MemoryUsages.toString(usage, true));
            final Label label = new Label(wid, sb.toString());
            label.setEscapeModelStrings(false);
            item.add(label);
        }
    }

    private static class MemoryBeansProducer implements Provider<List<? extends MemoryManagerMXBean>>, Serializable {

        private final boolean isGcOnly;

        public MemoryBeansProducer(boolean isGcOnly) {
            this.isGcOnly = isGcOnly;
        }
        private static final long serialVersionUID = 1L;

        public List<? extends MemoryManagerMXBean> get() {
            return isGcOnly ? ManagementFactory.getGarbageCollectorMXBeans() : ManagementFactory.getMemoryManagerMXBeans();
        }
    }

    /**
     * Shows a quick memory pool overview.
     */
    private static class MemoryPoolListView extends ListView<MemoryManagerMXBean> {

        private final String nameId;
        private final String validId;
        private final String propsId;

        public MemoryPoolListView(String id, IModel<? extends List<? extends MemoryManagerMXBean>> model, String nameId, String validId, String propsId) {
            super(id, model);
            this.nameId = nameId;
            this.validId = validId;
            this.propsId = propsId;
        }
        private static final long serialVersionUID = 1L;

        @Override
        protected void populateItem(ListItem<MemoryManagerMXBean> item) {
            final MemoryManagerMXBean bean = item.getModelObject();
            item.add(new Label(nameId, bean.getName()));
            item.add(new Label(validId, bean.isValid() ? "Y" : "N"));
            final StringBuilder sb = new StringBuilder();
            sb.append("Memory Pools: ");
            sb.append(Arrays.toString(bean.getMemoryPoolNames()));
            if (bean instanceof GarbageCollectorMXBean) {
                final GarbageCollectorMXBean gb = (GarbageCollectorMXBean) bean;
                sb.append("; Collections: ");
                sb.append(gb.getCollectionCount());
                sb.append("; Time: ");
                sb.append(gb.getCollectionTime());
                sb.append("ms");
            }
            item.add(new Label(propsId, sb.toString()));
        }
    }

    /**
     * Draws a memory usage status for given memory usage object
     *
     * @param usage the memory usage object, must be in megabytes as int
     * arithmetics is used.
     * @param wid the wicket component
     * @param width the width of the bar in pixels.
     */
    public final void drawMemoryStatus(final IModel<MemoryUsage> usage, final String wid, final int width) {
        register(usage);
        unescaped(wid, new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                try {
                    return DivGraph.drawMemoryStatus(usage.getObject(), width);
                } catch (Throwable t) {
                    log.log(Level.CONFIG, "Failed to obtain memory status", t);
                    return "Failed to obtain: " + t;
                }
            }
        });
    }
    private static final Logger log = Logger.getLogger(Memory.class.getName());
}
