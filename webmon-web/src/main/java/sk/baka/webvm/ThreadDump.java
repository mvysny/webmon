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

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.analyzer.utils.ThreadID;
import sk.baka.webvm.analyzer.utils.Threads;

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
    private static class ThreadListModel extends LoadableDetachableModel<List<ThreadDumpListItem>> {

        private static final long serialVersionUID = 1L;

        @Override
        protected List<ThreadDumpListItem> load() {
            final sk.baka.webvm.analyzer.utils.Threads.Dump dump = new sk.baka.webvm.analyzer.utils.Threads.Dump();
            final List<ThreadDumpListItem> list = new ArrayList<ThreadDumpListItem>();
            for (ThreadID id: dump.threads.keySet()) {
                list.add(new ThreadDumpListItem(dump, id));
            }
            return list;
        }
    }

    private static class ThreadDumpListItem {
        public final sk.baka.webvm.analyzer.utils.Threads.Dump dump;
        public final ThreadID id;

        public ThreadDumpListItem(Threads.Dump dump, ThreadID id) {
            this.dump = dump;
            this.id = id;
        }

        public String getThreadMetadata() {
            return dump.get(id).getThreadMetadata();
        }

        private String getThreadStacktrace() {
            return dump.getThreadStacktrace(id, true);
        }
    }
    
    /**
     * Wicket ListView which displays a thread list.
     */
    private static class ThreadListView extends ListView<ThreadDumpListItem> {

        public ThreadListView(String id, IModel<? extends List<? extends ThreadDumpListItem>> model) {
            super(id, model);
        }
        private static final long serialVersionUID = 1L;

        @Override
        protected void populateItem(final ListItem<ThreadDumpListItem> item) {
            final ThreadDumpListItem info = item.getModelObject();
            item.add(new Label("threadName", info.getThreadMetadata()));
            item.add(new Label("threadStacktrace", info.getThreadStacktrace()));
        }
    }
}
