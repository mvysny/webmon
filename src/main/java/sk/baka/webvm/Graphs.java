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

import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayList;
import sk.baka.webvm.analyzer.HistorySample;
import sk.baka.webvm.misc.TextGraph;
import java.util.List;
import java.util.Random;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import sk.baka.webvm.misc.DivGraph;
import sk.baka.webvm.misc.GraphStyle;
import sk.baka.webvm.misc.MgmtUtils;

/**
 * Shows the JVM memory usage and GC CPU usage graphs.
 * @author Martin Vysny
 */
public final class Graphs extends WebPage {

    public Graphs(PageParameters params) {
        final AppBorder border = new AppBorder("appBorder");
        add(border);
        final GraphStyle gs = new GraphStyle();
        gs.colors = new String[]{"#000000"};
        gs.height = 100;
        gs.width = 2;
        final DivGraph dg = new DivGraph(100, gs);
        final List<HistorySample> history = WicketApplication.getHistory().getVmstatHistory();
        for (final HistorySample hs : history) {
            dg.add(new int[]{hs.getGcCpuUsage()});
        }
        final Label label = new Label("gcCPUUsage", dg.draw());
        label.setEscapeModelStrings(false);
        // TODO draw the graph directly to a writer
        border.add(label);
        TextGraph tg = new TextGraph();
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
        // draw all memory graphs
        final List<TextGraph> graphs = new ArrayList<TextGraph>();
        for (final MemoryPoolMXBean pool : MgmtUtils.getMemoryPools().values()) {
            final TextGraph graph = new TextGraph();
            graph.setRange(0, (int) (pool.getUsage().getMax() / 1024 / 1024));
            graphs.add(graph);
        }
        for (final HistorySample hs : history) {
            int i = 0;
            for (final int usage : hs.getMemPoolUsage()) {
                graphs.get(i++).addValue(usage);
            }
        }
        int i = 0;
        final StringBuilder sb = new StringBuilder();
        for (final String pool : MgmtUtils.getMemoryPools().keySet()) {
            final TextGraph graph = graphs.get(i++);
            sb.append("<h2>[");
            sb.append(pool);
            sb.append("] memory usage:");
            sb.append("</h2><pre class=\"graph\">");
            sb.append(graph.draw(10));
            sb.append("</pre>");
        }
        final Label graphLabel = new Label("memPoolUsages", sb.toString());
        graphLabel.setEscapeModelStrings(false);
        border.add(graphLabel);
    }
}
