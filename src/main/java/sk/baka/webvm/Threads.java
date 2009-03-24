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

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.wicket.PageParameters;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.analyzer.HistorySample;

/**
 * Shows the thread history.
 * @author vyzivus
 */
public final class Threads extends WebPage {

    public Threads(PageParameters params) {
        final AppBorder border = new AppBorder("appBorder");
        add(border);
        final SortedMap<Long, List<ThreadInfo>> threadAnalysis = analyzeThreads();
        drawThreads(border, threadAnalysis);
    }

    private SortedMap<Long, List<ThreadInfo>> analyzeThreads() {
        final List<HistorySample> samples = WicketApplication.getHistory().getVmstatHistory();
        final SortedMap<Long, List<ThreadInfo>> history = new TreeMap<Long, List<ThreadInfo>>();
        // compute the map
        int i = 0;
        for (final HistorySample sample : samples) {
            for (final ThreadInfo info : sample.threads) {
                if (info == null) {
                    continue;
                }
                List<ThreadInfo> list = history.get(info.getThreadId());
                if (list == null) {
                    list = new ArrayList<ThreadInfo>(samples.size());
                    history.put(info.getThreadId(), list);
                }
                ensureSize(list, i);
                list.add(info);
            }
            i++;
        }
        // align dead threads' list for a proper length.
        for (final List<ThreadInfo> infos : history.values()) {
            ensureSize(infos, i);
        }
        return history;
    }

    private void drawThreads(final AppBorder border, final SortedMap<Long, List<ThreadInfo>> threadAnalysis) {
        border.add(new ListView<List<ThreadInfo>>("threads", new Detachable(threadAnalysis)) {

            @Override
            protected void populateItem(ListItem<List<ThreadInfo>> item) {
                final List<ThreadInfo> infos = item.getModelObject();
                ThreadInfo ti = null;
                for (final ThreadInfo info : infos) {
                    if (info != null) {
                        ti = info;
                        break;
                    }
                }
                final ThreadInfo last = infos.get(infos.size() - 1);
                String name = ti.getThreadName();
                String title = name;
                if (name.length() > 30) {
                    name = name.substring(0, 30) + "...";
                }
                final Label l = new Label("threadName", name);
                item.add(l);
                l.add(new SimpleAttributeModifier("title", title));
                final String state = last == null ? "dead" : last.getThreadState().toString();
                item.add(new Label("threadState", state));
                final StringBuilder sb = new StringBuilder();
                for (final ThreadInfo info : infos) {
                    sb.append(getState(info));
                }
                item.add(new Label("threadHistory", sb.toString()));
            }
        });
    }

    private char getState(final ThreadInfo info) {
        if (info == null) {
            return ' ';
        }
        switch (info.getThreadState()) {
            case NEW:
                return '-';
            case BLOCKED:
                return 'b';
            case RUNNABLE:
                return 'R';
            case TERMINATED:
                return 't';
            case TIMED_WAITING:
            case WAITING:
                return 'w';
        }
        throw new AssertionError();
    }

    private static class Detachable extends LoadableDetachableModel<List<List<ThreadInfo>>> {

        private final transient Map<Long, List<ThreadInfo>> map;

        public Detachable(final Map<Long, List<ThreadInfo>> map) {
            this.map = map;
        }

        @Override
        protected List<List<ThreadInfo>> load() {
            final List<List<ThreadInfo>> result = new ArrayList<List<ThreadInfo>>(map.values());
            return result;
        }
    }

    private List<ThreadInfo> ensureSize(List<ThreadInfo> list, final int size) {
        while (list.size() < size) {
            list.add(null);
        }
        return list;
    }
}

