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

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.wicket.guice.GuiceComponentInjector;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.lang.PackageName;
import sk.baka.webvm.analyzer.HistorySampler;
import sk.baka.webvm.analyzer.ProblemAnalyzer;

/**
 * The main Wicket application class.
 * @author Martin Vysny
 */
public final class WicketApplication extends WebApplication {

    @Deprecated
    public static HistorySampler getHistory() {
        return getInjector().getInstance(HistorySampler.class);
    }

    @Override
    public Class<HomePage> getHomePage() {
        return HomePage.class;
    }

    /**
     * Sets the new configuration and restarts analyzer and sampler.
     * @param config the new config, must not be null.
     */
    public static synchronized void configChanged() {
        injector.getInstance(HistorySampler.class).stop();
        injector.getInstance(HistorySampler.class).configChanged();
        injector.getInstance(ProblemAnalyzer.class).configChanged();
        injector.getInstance(HistorySampler.class).start();
    }
    private static Injector injector;

    static Injector getInjector() {
        if (injector == null) {
            throw new RuntimeException("Injector is null");
        }
        return injector;
    }

    @Override
    protected void init() {
        super.init();
        injector = Guice.createInjector(new WebmonModule(getInitParameter("configFile")));
        injector.getInstance(HistorySampler.class).start();
        addComponentInstantiationListener(new GuiceComponentInjector(this, injector));
        mount("a", PackageName.forClass(getClass()));
        getMarkupSettings().setDefaultBeforeDisabledLink("<a href=\"#\" class=\"current_page_item\">");
        getMarkupSettings().setDefaultAfterDisabledLink("</a>");
    }

    @Override
    protected void onDestroy() {
        injector.getInstance(HistorySampler.class).stop();
        super.onDestroy();
    }
}
