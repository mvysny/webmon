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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.profiler.MethodInvocationStats;
import sk.baka.webvm.profiler.ProfilerEngine;

/**
 * The profiler Wicket page.
 * @author Martin Vysny
 */
public final class Profiler extends WebVMPage {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the page.
     */
    public Profiler() {
        border.add(new Label("profilerState", ProfilerEngine.getInstance().isRunning() ? "profiling" : "stopped"));
        border.add(new StartProfilerLink("profilerStart"));
        border.add(new StopProfilerLink("profilerStop"));
        border.add(new ProfilerOutputListView("profilerTable", new ProfilerDataModel()));
    }

    private static class StartProfilerLink extends Link<Void> {

        private static final long serialVersionUID = 1L;

        public StartProfilerLink(String id) {
            super(id);
        }

        @Override
        public void onClick() {
            ProfilerEngine.getInstance().start(100);
            setResponsePage(Profiler.class);
        }
    }

    private static class StopProfilerLink extends Link<Void> {

        private static final long serialVersionUID = 1L;

        public StopProfilerLink(String id) {
            super(id);
        }

        @Override
        public void onClick() {
            ProfilerEngine.getInstance().stop();
            setResponsePage(Profiler.class);
        }
    }

    private static class ProfilerOutputListView extends ListView<Entry<String, Map<String, MethodInvocationStats>>> {

        private static final long serialVersionUID = 1L;

        public ProfilerOutputListView(String id, IModel<? extends List<? extends Entry<String, Map<String, MethodInvocationStats>>>> model) {
            super(id, model);
        }

        @Override
        protected void populateItem(ListItem<Entry<String, Map<String, MethodInvocationStats>>> item) {
            item.add(new Label("profiledClass", item.getModelObject().getKey()));
            item.add(new Label("profiledMethod", item.getModelObject().getValue().keySet().toString()));
            item.add(new Label("profiledTime", item.getModelObject().getValue().values().toString()));
        }
    }

    static class ProfilerDataModel extends LoadableDetachableModel<List<? extends Entry<String, Map<String, MethodInvocationStats>>>> {

        private static final long serialVersionUID = 1L;

        @Override
        protected List<? extends Entry<String, Map<String, MethodInvocationStats>>> load() {
            final Map<String, Map<String, MethodInvocationStats>> profilingData = ProfilerEngine.getInstance().getData();
            return profilingData == null ? new ArrayList<Entry<String, Map<String, MethodInvocationStats>>>() : new ArrayList<Entry<String, Map<String, MethodInvocationStats>>>(profilingData.entrySet());
        }
    }
}
