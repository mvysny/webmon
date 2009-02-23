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

import sk.baka.webvm.misc.TextGraph;
import java.util.List;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;

/**
 * Shows various graphs.
 * @author Martin Vysny
 */
public final class Graphs extends WebPage {

    public Graphs(PageParameters params) {
        final AppBorder border = new AppBorder("appBorder");
        add(border);
        TextGraph tg = new TextGraph();
        tg.setRange(0, 100);
        final List<HistorySample> history = WicketApplication.getHistory();
        for (final HistorySample hs : history) {
            tg.addValue(hs.getGcCpuUsage());
        }
        // TODO draw the graph directly to a writer
        border.add(new Label("gcCPUUsage", tg.draw(10)));
        tg = new TextGraph();
        long maxMem = Runtime.getRuntime().maxMemory();
        if (maxMem != Long.MAX_VALUE) {
            maxMem = maxMem / 1024 / 1024;
            tg.setRange(0, (int) maxMem);
        }
        for (final HistorySample hs : history) {
            tg.addValue(hs.getMemUsage());
        }
        // TODO draw the graph directly to a writer
        border.add(new Label("memoryUsage", tg.draw(10)));
    }
}

