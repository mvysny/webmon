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
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import sk.baka.webvm.analyzer.HistorySampler;
import sk.baka.webvm.misc.DivGraph;
import sk.baka.webvm.misc.GraphStyle;
import sk.baka.webvm.misc.MgmtUtils;

/**
 * Shows the JVM memory usage and GC CPU usage graphs.
 * @author Martin Vysny
 */
public final class Graphs extends WebVMPage {

    public Graphs(PageParameters params) {
        final List<HistorySample> history = WicketApplication.getHistory().getVmstatHistory();
        drawGcCpuUsage(border, history);
        drawMemoryUsageGraph(history, "heapUsage", 0);
        if (MgmtUtils.isNonHeapPool()) {
            drawMemoryUsageGraph(history, "nonHeapUsage", 1);
        } else {
            border.add(new Label("nonHeapUsage", "No information available"));
        }
    }

    private void drawGcCpuUsage(AppBorder border, final List<HistorySample> history) {
        final GraphStyle gs = new GraphStyle();
        gs.colors = new String[]{"#7e43b2"};
        gs.height = 100;
        gs.width = 2;
        gs.border = "black";
        gs.yLegend = true;
        final DivGraph dg = new DivGraph(100, gs);
        for (final HistorySample hs : history) {
            dg.add(new int[]{hs.getGcCpuUsage()});
        }
        dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength());
        final Label label = new Label("gcCPUUsage", dg.draw());
        label.setEscapeModelStrings(false);
        // TODO draw the graph directly to a writer
        border.add(label);
    }
}
