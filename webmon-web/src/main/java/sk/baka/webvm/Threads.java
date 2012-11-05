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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.analyzer.HistorySample;
import sk.baka.webvm.analyzer.IHistorySampler;
import sk.baka.webvm.analyzer.ThreadMap;

/**
 * Shows the thread history.
 * @author vyzivus
 */
public class Threads extends WebVMPage {

    private static final long serialVersionUID = 1L;
    private static final int MAX_THREAD_NAME_LENGTH = 30;

    /**
     * Constructor.
     */
    public Threads() {
        border.add(new ThreadListView("threads", new ThreadListModel()));
    }

    @Inject
    private IHistorySampler historySampler;

    private SortedMap<Long, List<ThreadMap.Item>> analyzeThreads() {
        return ThreadMap.historyToTable(historySampler.getVmstatHistory());
    }

    /**
     * Returns an ASCII-graphic character representing the thread state.
     * @param info the thread.
     * @return the thread character.
     */
    private static char getStateChar(final ThreadInfo info) {
        if (info == null) {
            return ' ';
        }
        switch (info.getThreadState()) {
            case NEW:
                return '.';
            case BLOCKED:
                return 'x';
            case RUNNABLE:
                return '|';
            case TERMINATED:
                return '_';
            case TIMED_WAITING:
            case WAITING:
                return 'z';
        }
        throw new AssertionError();
    }

    private class ThreadListModel extends LoadableDetachableModel<List<List<ThreadMap.Item>>> {

        private static final long serialVersionUID = 1L;

        @Override
        protected List<List<ThreadMap.Item>> load() {
            final List<List<ThreadMap.Item>> result = new ArrayList<List<ThreadMap.Item>>(analyzeThreads().values());
            return result;
        }
    }

    /**
     * Wicket ListView showing thread names and thread states.
     */
    private static class ThreadListView extends ListView<List<ThreadMap.Item>> {

        private static final long serialVersionUID = 1L;

        public ThreadListView(String id, IModel<? extends List<? extends List<ThreadMap.Item>>> model) {
            super(id, model);
        }

        @Override
        protected void populateItem(ListItem<List<ThreadMap.Item>> item) {
            final List<ThreadMap.Item> infos = item.getModelObject();
            ThreadMap.Item ti = null;
            for (final ThreadMap.Item info : infos) {
                if (info != null) {
                    ti = info;
                    break;
                }
            }
            final ThreadMap.Item last = infos.get(infos.size() - 1);
            String name = ti.info.getThreadName();
            String title = name;
            if (name.length() > MAX_THREAD_NAME_LENGTH) {
                name = name.substring(0, MAX_THREAD_NAME_LENGTH) + "...";
            }
            final Label l = new Label("threadName", name);
            item.add(l);
            l.add(AttributeModifier.replace("title", title));
            final String state = last == null ? "dead" : last.info.getThreadState().toString();
            item.add(new Label("threadState", state));
            final StringBuilder sb = new StringBuilder();
            for (final ThreadMap.Item info : infos) {
                sb.append(getStateChar(info.info));
            }
            final long threadCPUTimeNanos = ti.threadId < 0 ? -1 : ManagementFactory.getThreadMXBean().getThreadCpuTime(ti.threadId);
            sb.append("  Total CPU: ").append(threadCPUTimeNanos / 1000000).append(" ms");
            item.add(new Label("threadHistory", sb.toString()));
        }
    }
}

