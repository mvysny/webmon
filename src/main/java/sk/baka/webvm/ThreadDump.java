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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Performs and displays a full thread dump.
 * @author vyzivus
 */
public final class ThreadDump extends WebPage {

    public ThreadDump(PageParameters params) {
        final AppBorder border = new AppBorder("appBorder");
        add(border);
        border.add(new ListView<ThreadInfo>("threadList", new LoadableDetachableModel<List<ThreadInfo>>() {

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
        }) {

            @Override
            protected void populateItem(ListItem<ThreadInfo> item) {
                final ThreadInfo info = item.getModelObject();
                final StringBuilder sb = new StringBuilder();
                sb.append("0x");
                sb.append(Long.toHexString(info.getThreadId()));
                sb.append(" [");
                sb.append(info.getThreadName());
                sb.append("] ");
                sb.append(info.getThreadState().toString());
                if (info.isInNative()) {
                    sb.append(", in native");
                }
                if (info.isSuspended()) {
                    sb.append(", suspended");
                }
                final String lockName = info.getLockName();
                if (lockName != null) {
                    sb.append(", locked on [");
                    sb.append(lockName);
                    sb.append("]");
                }
                item.add(new Label("threadName", sb.toString()));
                sb.delete(0, sb.length());
                final StackTraceElement[] stack = info.getStackTrace();
                if (stack == null) {
                    sb.append("  stack trace not available");
                } else if (stack.length == 0) {
                    sb.append("  stack trace is empty");
                } else {
                    for (final StackTraceElement ste : stack) {
                        sb.append("  at ");
                        sb.append(ste.toString());
                        sb.append('\n');
                    }
                }
                item.add(new Label("threadStacktrace", sb.toString()));
            }
        });
    }
}