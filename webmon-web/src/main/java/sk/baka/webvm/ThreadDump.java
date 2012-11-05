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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.analyzer.utils.MiscUtils;

/**
 * Performs and displays a full thread dump.
 * @author vyzivus
 */
public final class ThreadDump extends WebVMPage {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public ThreadDump() {
        border.add(new ThreadListView("threadList", new ThreadListModel()));
    }

    /**
     * Provides a list of all VM threads.
     */
    private static class ThreadListModel extends LoadableDetachableModel<List<ThreadInfo>> {

        private static final long serialVersionUID = 1L;

        @Override
        protected List<ThreadInfo> load() {
            final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            final ThreadInfo[] info = bean.getThreadInfo(bean.getAllThreadIds(), Integer.MAX_VALUE);
            final List<ThreadInfo> result = new ArrayList<ThreadInfo>(info.length);
            for (final ThreadInfo i : info) {
                if (i != null) {
                    result.add(i);
                }
            }
            return result;
        }
    }

    /**
     * Wicket ListView which displays a thread list.
     */
    private static class ThreadListView extends ListView<ThreadInfo> {

        public ThreadListView(String id, IModel<? extends List<? extends ThreadInfo>> model) {
            super(id, model);
        }
        private static final long serialVersionUID = 1L;

        @Override
        protected void populateItem(final ListItem<ThreadInfo> item) {
            final ThreadInfo info = item.getModelObject();
            item.add(new Label("threadName", MiscUtils.getThreadMetadata(info)));
            item.add(new Label("threadStacktrace", MiscUtils.getThreadStacktrace(info)));
        }
    }
}
