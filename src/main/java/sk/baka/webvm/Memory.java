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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.misc.MgmtUtils;

/**
 * Shows detailed memory information.
 * @author Martin Vysny
 */
public final class Memory extends WebPage {

    /**
     * Creates new object instance
     * @param params page parameters
     */
    public Memory(PageParameters params) {
        final AppBorder border = new AppBorder("appBorder");
        add(border);
        // fill in the memory info
        displayMemInfo(border, ManagementFactory.getMemoryManagerMXBeans(), "memoryManagers", "memManName", "memManValid", "memManProperties");
        displayMemInfo(border, ManagementFactory.getGarbageCollectorMXBeans(), "gc", "gcName", "gcValid", "gcProperties");
        // fill the memory pool info
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
                final StringBuilder sb = new StringBuilder();
                if (bean.isCollectionUsageThresholdSupported()) {
                    sb.append(bean.getCollectionUsageThresholdCount());
                    sb.append(" collections on ");
                    sb.append(bean.getCollectionUsageThreshold() / 1024 / 1024);
                    sb.append("M usage.");
                } else {
                    sb.append("Not collectable;");
                }
                sb.append("<br/>After last GC: ");
                sb.append(MgmtUtils.toString(bean.getCollectionUsage()));
                Label label = new Label("poolCollects", sb.toString());
                label.setEscapeModelStrings(false);
                item.add(label);
                item.add(new Label("poolCollectsPerc", MgmtUtils.getUsagePerc(bean.getCollectionUsage())));
                item.add(new Label("poolPeak", MgmtUtils.toString(bean.getPeakUsage())));
                item.add(new Label("poolPeakPerc", MgmtUtils.getUsagePerc(bean.getPeakUsage())));
                sb.delete(0, sb.length());
                if (bean.isUsageThresholdSupported()) {
                    sb.append(bean.getUsageThresholdCount());
                    sb.append(" collections on ");
                    sb.append(bean.getUsageThreshold() / 1024 / 1024);
                    sb.append("M usage;<br/>");
                } else {
                    sb.append("Not collectable;<br/>");
                }
                sb.append(MgmtUtils.toString(bean.getUsage()));
                label = new Label("poolUsage", sb.toString());
                label.setEscapeModelStrings(false);
                item.add(label);
                item.add(new Label("poolUsagePerc", MgmtUtils.getUsagePerc(bean.getUsage())));
            }
        });
        border.add(new Link<Void>("performGCLink") {

            @Override
            public void onClick() {
                System.gc();
                setResponsePage(Memory.class);
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

