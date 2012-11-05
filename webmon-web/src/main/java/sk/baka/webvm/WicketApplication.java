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
import sk.baka.webvm.analyzer.IHistorySampler;
import sk.baka.webvm.analyzer.IProblemAnalyzer;

/**
 * The main Wicket application class.
 * @author Martin Vysny
 */
public final class WicketApplication extends WebApplication {

    @Override
    public Class<HomePage> getHomePage() {
        return HomePage.class;
    }

    /**
     * Sets the new configuration and restarts analyzer and sampler.
     * @param config the new config, must not be null.
     */
    public static synchronized void configChanged() {
        injector.getInstance(IHistorySampler.class).stop();
        injector.getInstance(IHistorySampler.class).configChanged();
        injector.getInstance(IProblemAnalyzer.class).configChanged();
        injector.getInstance(IHistorySampler.class).start();
    }
    private static Injector injector;

    public static Injector getInjector() {
        if (injector == null) {
            throw new RuntimeException("Injector is null");
        }
        return injector;
    }

    @Override
    protected void init() {
        super.init();
        injector = Guice.createInjector(new WebmonModule(getInitParameter("configFile")));
        injector.getInstance(IHistorySampler.class).start();
        getComponentInstantiationListeners().add(new GuiceComponentInjector(this, injector));
        getMarkupSettings().setDefaultBeforeDisabledLink("<a href=\"#\" class=\"current_page_item\">");
        getMarkupSettings().setDefaultAfterDisabledLink("</a>");
    }

    @Override
    protected void onDestroy() {
        injector.getInstance(HistorySampler.class).stop();
        super.onDestroy();
    }
}
