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
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.misc.MgmtUtils;

/**
 * Shows detailed memory information.
 * @author Martin Vysny
 */
public final class Memory extends WebVMPage {

    /**
     * Creates new object instance
     * @param params page parameters
     */
    public Memory(PageParameters params) {
        MemoryUsage usage = MgmtUtils.getInMB(MgmtUtils.getHeapFromRuntime());
        drawMemoryStatus(usage, "heapStatusBar", 300);
        border.add(new Label("heapStatusText", MgmtUtils.toString(usage, true)));
        usage = MgmtUtils.getInMB(sk.baka.webvm.analyzer.hostos.Memory.getPhysicalMemory());
        drawMemoryStatus(usage, "physicalMemoryStatusBar", 300);
        border.add(new Label("physicalMemoryStatusText", MgmtUtils.toString(usage, true)));
        usage = MgmtUtils.getInMB(sk.baka.webvm.analyzer.hostos.Memory.getSwap());
        drawMemoryStatus(usage, "swapStatusBar", 300);
        border.add(new Label("swapStatusText", MgmtUtils.toString(usage, true)));
        displayMemInfo(border, ManagementFactory.getMemoryManagerMXBeans(), "memoryManagers", "memManName", "memManValid", "memManProperties");
        displayMemInfo(border, ManagementFactory.getGarbageCollectorMXBeans(), "gc", "gcName", "gcValid", "gcProperties");
        addMemoryPoolInfo();
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

    private void addMemoryPoolInfo() {
        final IModel<List<MemoryPoolMXBean>> model = new LoadableDetachableModel<List<MemoryPoolMXBean>>() {

            @Override
            protected List<MemoryPoolMXBean> load() {
                return ManagementFactory.getMemoryPoolMXBeans();
            }
        };
        border.add(new ListView<MemoryPoolMXBean>("memoryPool", model) {

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
                    sb.append(drawMemoryStatus(usage, 200));
                }
                sb.append(MgmtUtils.toString(usage, true));
                final Label label = new Label(wid, sb.toString());
                label.setEscapeModelStrings(false);
                item.add(label);
            }
        });
    }

    private void displayMemInfo(final AppBorder border, final List<? extends MemoryManagerMXBean> b, final String listId, final String nameId, final String validId, final String propsId) {
        final List<? extends MemoryManagerMXBean> beans = b != null ? b : Collections.<MemoryManagerMXBean>emptyList();
        final IModel<List<MemoryManagerMXBean>> model = new LoadableDetachableModel<List<MemoryManagerMXBean>>(new ArrayList<MemoryManagerMXBean>(beans)) {

            @Override
            protected List<MemoryManagerMXBean> load() {
                return null;
            }
        };
        border.add(new ListView<MemoryManagerMXBean>(listId, model) {

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
        });
    }
}

