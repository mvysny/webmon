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
import sk.baka.webvm.misc.BluffGraph;
import sk.baka.webvm.misc.DivGraph;
import sk.baka.webvm.misc.GraphStyle;

/**
 * A superclass of all webvm pages.
 * @author Martin Vysny
 */
public class WebVMPage extends WebPage {

    private static final long serialVersionUID = 1L;
    /**
     * Each page is wrapped in this border.
     */
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
    public final void drawMemoryUsageGraph(final List<HistorySample> history, final String wid, final int index) {
        if (history.size() == 0) {
            border.add(new Label(wid, ""));
            return;
        }
        final GraphStyle gs = Graphs.newDefaultStyle();
        gs.colors = new String[]{Graphs.COLOR_BLUE, Graphs.COLOR_BROWN};
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
        final BluffGraph dg = new BluffGraph((int) maxMem, gs);
        for (final HistorySample hs : history) {
            final MemoryUsage usage = hs.memPoolUsage[index];
            dg.add(new int[]{(int) usage.getUsed(), (int) usage.getCommitted()});
        }
        dg.fillWithZero(HistorySampler.HISTORY_VMSTAT.getHistoryLength());
        unescaped(wid, dg.draw());
    }

    /**
     * Shows given string unescaped.
     * @param wid the wicket component
     * @param value the value to show
     */
    public final void unescaped(final String wid, final String value) {
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
    public final void drawMemoryStatus(final MemoryUsage usage, final String wid, final int width) {
        final String bar = DivGraph.drawMemoryStatus(usage, width);
        unescaped(wid, bar);
    }
}
