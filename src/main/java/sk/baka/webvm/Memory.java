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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.misc.DivGraph;
import sk.baka.webvm.misc.MgmtUtils;
import sk.baka.webvm.misc.Producer;

/**
 * Shows detailed memory information.
 * @author Martin Vysny
 */
public final class Memory extends WebVMPage {

    private static final long serialVersionUID = 1L;
    private static final int GRAPH_WIDTH_PIXELS = 300;

    /**
     * Creates new object instance
     */
    public Memory() {
        MemoryUsage usage = MgmtUtils.getInMB(MgmtUtils.getHeapFromRuntime());
        drawMemoryStatus(usage, "heapStatusBar", GRAPH_WIDTH_PIXELS);
        border.add(new Label("heapStatusText", MgmtUtils.toString(usage, true)));
        usage = MgmtUtils.getInMB(sk.baka.webvm.analyzer.hostos.Memory.getPhysicalMemory());
        drawMemoryStatus(usage, "physicalMemoryStatusBar", GRAPH_WIDTH_PIXELS);
        border.add(new Label("physicalMemoryStatusText", MgmtUtils.toString(usage, true)));
        usage = MgmtUtils.getInMB(sk.baka.webvm.analyzer.hostos.Memory.getSwap());
        drawMemoryStatus(usage, "swapStatusBar", GRAPH_WIDTH_PIXELS);
        border.add(new Label("swapStatusText", MgmtUtils.toString(usage, true)));
        addMemoryPoolInfo(border, new MemoryBeansProducer(false), "memoryManagers", "memManName", "memManValid", "memManProperties");
        addMemoryPoolInfo(border, new MemoryBeansProducer(true), "gc", "gcName", "gcValid", "gcProperties");
        addDetailedMemoryPoolInfo(border);
        addGCStats();
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

    private static void addMemoryPoolInfo(final AppBorder border, final Producer<? extends List<? extends MemoryManagerMXBean>> memoryBeansProducer, final String listId, final String nameId, final String validId, final String propsId) {
        final IModel<List<MemoryManagerMXBean>> model = new LoadableDetachableModel<List<MemoryManagerMXBean>>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<MemoryManagerMXBean> load() {
                final List<? extends MemoryManagerMXBean> beans = memoryBeansProducer.produce();
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
            MemoryUsage usage = MgmtUtils.getInMB(bean.getCollectionUsage());
            add(item, "poolCollects", usage, true);
            item.add(new Label("poolCollectsPerc", MgmtUtils.getUsagePerc(usage)));
            usage = MgmtUtils.getInMB(bean.getPeakUsage());
            add(item, "poolPeak", usage, false);
            item.add(new Label("poolPeakPerc", MgmtUtils.getUsagePerc(usage)));
            usage = MgmtUtils.getInMB(bean.getUsage());
            add(item, "poolUsage", usage, false);
            item.add(new Label("poolUsagePerc", MgmtUtils.getUsagePerc(usage)));
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
            sb.append(MgmtUtils.toString(usage, true));
            final Label label = new Label(wid, sb.toString());
            label.setEscapeModelStrings(false);
            item.add(label);
        }
    }

    private static class MemoryBeansProducer implements Producer<List<? extends MemoryManagerMXBean>> {

        private final boolean isGcOnly;

        public MemoryBeansProducer(boolean isGcOnly) {
            this.isGcOnly = isGcOnly;
        }
        private static final long serialVersionUID = 1L;

        public List<? extends MemoryManagerMXBean> produce() {
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
}

