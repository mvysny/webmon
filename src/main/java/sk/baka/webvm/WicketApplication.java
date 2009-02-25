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

import org.apache.wicket.protocol.http.WebApplication;
import sk.baka.webvm.analyzer.HistorySampler;
import sk.baka.webvm.analyzer.ProblemAnalyzer;

/**
 * The main Wicket application class.
 * @author Martin Vysny
 */
public final class WicketApplication extends WebApplication {

    @Override
    public Class<HomePage> getHomePage() {
        return HomePage.class;
    }
    private static HistorySampler sampler = null;

    private static ProblemAnalyzer analyzer = null;

    /**
     * Returns a history sampler.
     * @return the history sampler.
     */
    public static HistorySampler getHistory() {
        return sampler;
    }

    /**
     * Returns a problem analyzer instance.
     * @return the problem analyzer.
     */
    public static ProblemAnalyzer getAnalyzer() {
        return analyzer;
    }

    @Override
    protected void init() {
        super.init();
        analyzer = new ProblemAnalyzer();
        analyzer.configure(this);
        sampler = new HistorySampler(analyzer);
        sampler.start();
        mountBookmarkablePage("/graphs.html", Graphs.class);
        mountBookmarkablePage("/problems.html", Problems.class);
        mountBookmarkablePage("/memory.html", Memory.class);
        mountBookmarkablePage("/sysinfo.html", SysInfo.class);
        mountBookmarkablePage("/jndi.html", Jndi.class);
    }

    @Override
    protected void onDestroy() {
        sampler.stop();
        super.onDestroy();
    }
}
