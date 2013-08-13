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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.baka.webvm.analyzer.HistorySampler;
import sk.baka.webvm.analyzer.IHistorySampler;
import sk.baka.webvm.analyzer.IProblemAnalyzer;
import sk.baka.webvm.analyzer.ProblemAnalyzer;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;
import sk.baka.webvm.analyzer.config.Binder;
import sk.baka.webvm.analyzer.config.Config;
import sk.baka.webvm.analyzer.utils.INotificationDelivery;
import sk.baka.webvm.analyzer.utils.MemoryUsages;
import sk.baka.webvm.analyzer.utils.MiscUtils;
import sk.baka.webvm.analyzer.utils.NotificationDelivery;

/**
 * The Guice module.
 * @author Martin Vysny
 */
public class WebmonModule extends AbstractModule {

    private final String configFileUrl;

    /**
     * Creates new module instance.
     * @param configFileUrl an optional config file URL. If not null then this file is used, if null, other locations are probed.
     */
    public WebmonModule(final String configFileUrl) {
        this.configFileUrl = configFileUrl;
    }

    @Override
    protected void configure() {
    }
    private static final Logger LOG = Logger.getLogger(WebmonModule.class.getName());

    @Provides
    @Singleton
    protected Config loadConfig() {
        String configUrl = configFileUrl;
        if (configUrl != null) {
            LOG.info("Configuration file specified in the webmon.war/WEB-INF/web.xml descriptor: " + configUrl);
        } else {
            configUrl = "/etc/webmon.properties";
            final File f = new File(configUrl);
            if (!f.exists() || !f.isFile()) {
                configUrl = null;
            } else {
                configUrl = "file:" + configUrl;
                LOG.info("Loading configuration file from /etc: " + configUrl);
            }
        }
        if (configUrl == null) {
            configUrl = "classpath:config.properties";
            LOG.info("Loading default configuration file from webmon.war/WEB-INF/classes/config.properties");
        }
        try {
            final InputStream in;
            if (configUrl.startsWith("classpath:")) {
                final String resource = configUrl.substring("classpath:".length());
                in = MiscUtils.getResource(resource);
            } else {
                in = new URL(configUrl).openStream();
            }
            try {
                final Properties props = new Properties();
                props.load(in);
                final Config result = new Config();
                final Map<String, String> warnings = Binder.bindBeanMap(result, props, false, true);
                Binder.log(LOG, warnings);
                return result;
            } finally {
                MiscUtils.closeQuietly(in);
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Failed to load " + configUrl + ", keeping defaults", ex);
        }
        return new Config();
    }

    @Provides
    @Singleton
    public IHistorySampler newHistorySampler(Config cfg, IMemoryInfoProvider meminfo, IProblemAnalyzer a) {
        final INotificationDelivery d = new NotificationDelivery(cfg);
        return new HistorySampler(meminfo, a, d);
    }
    
    @Provides
    @Singleton
    public IProblemAnalyzer newProblemAnalyzer(Config cfg, IMemoryInfoProvider meminfo) {
        return new ProblemAnalyzer(cfg, meminfo);
    }
    
    @Provides
    @Singleton
    protected IMemoryInfoProvider getMemoryInfoProvider() {
        return MemoryUsages.getMemoryInfoProvider();
    }
}
