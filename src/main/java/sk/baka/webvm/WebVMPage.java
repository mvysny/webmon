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

import java.lang.management.MemoryUsage;
import java.util.List;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import sk.baka.webvm.analyzer.HistorySample;
import sk.baka.webvm.analyzer.HistorySampler;
import sk.baka.webvm.misc.DivGraph;
import sk.baka.webvm.misc.GraphStyle;

/**
 * A superclass of all webvm pages.
 * @author Martin Vysny
 */
public class WebVMPage extends WebPage {

    protected final AppBorder border;

    /**
     * Creates new webvm page.
     */
    public WebVMPage() {
        super();
        border = new AppBorder("appBorder");
        add(border);
    }

    /**
     * Draws details for given memory usage object line.
     * @param history the history to draw
     * @param wid chain result with this wicket id
     * @param index the memory usage index to the {@link HistorySample#memUsage} array.
     */
    public void drawMemoryUsageGraph(final List<HistorySample> history, final String wid, final int index) {
        if (history.size() == 0) {
            border.add(new Label(wid, ""));
            return;
        }
        final GraphStyle gs = new GraphStyle();
        gs.height = 100;
        gs.width = 2;
        gs.colors = new String[]{"#7e43b2", "#ff7f7f"};
        gs.border = "black";
        gs.yLegend = true;
        long maxMem = history.get(0).memPoolUsage[index].getMax();
        if (maxMem == -1) {
            maxMem = 0;
            for (final HistorySample hs : history) {
                final MemoryUsage usage = hs.memPoolUsage[index];
                if (maxMem < usage.getCommitted()) {
                    maxMem = usage.getCommitted();
                }
            }
            maxMem = maxMem * 5 / 4;
        }
        final DivGraph dg = new DivGraph((int) maxMem, gs);
        for (final HistorySample hs : history) {
            final MemoryUsage usage = hs.memPoolUsage[index];
            dg.add(new int[]{(int) usage.getUsed(), (int) usage.getCommitted()});
        }
        dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength());
        // TODO draw the graph directly to a writer
        unescaped(wid, dg.draw());
    }

    /**
     * Shows given string unescaped.
     * @param wid the wicket component
     * @param value the value to show
     */
    public void unescaped(final String wid, final String value) {
        final Label l = new Label(wid, value);
        l.setEscapeModelStrings(false);
        border.add(l);
    }

    /**
     * Draws a memory usage status for given memory usage object
     * @param usage the memory usage object, must be in megabytes as int arithmetics is used.
     * @param wid the wicket component
     * @param width the width of the bar in pixels.
     */
    public void drawMemoryStatus(final MemoryUsage usage, final String wid, final int width) {
        final String bar = drawMemoryStatus(usage, width);
        unescaped(wid, bar);
    }

    /**
     * Draws a memory usage status for given memory usage object
     * @param usage the memory usage object, must be in megabytes as int arithmetics is used.
     * @param width the width of the bar in pixels.
     */
    public static String drawMemoryStatus(final MemoryUsage usage, final int width) {
        final GraphStyle gs = new GraphStyle();
        gs.vertical = false;
        gs.width = width;
        gs.height = 20;
        gs.showPercentage = true;
        gs.colors = new String[]{"#7e43b2", "#ff7f7f"};
        gs.border = "#999999";
        gs.fontColors = new String[]{"#ffffff", null};
        int max = (int) usage.getMax();
        if (max == -1) {
            max = (int) usage.getCommitted() * 5 / 4;
        }
        final String bar = DivGraph.drawStackedBar(gs, new int[]{(int) usage.getUsed(), (int) usage.getCommitted()}, max, false);
        return bar;
    }
}
