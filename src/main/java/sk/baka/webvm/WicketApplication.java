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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.protocol.http.WebApplication;
import sk.baka.webvm.analyzer.HistorySampler;
import sk.baka.webvm.analyzer.ProblemAnalyzer;
import sk.baka.webvm.config.Binder;
import sk.baka.webvm.config.Config;

/**
 * The main Wicket application class.
 * @author Martin Vysny
 */
public final class WicketApplication extends WebApplication {

	@Override
	public Class<HomePage> getHomePage() {
		return HomePage.class;
	}
	private static HistorySampler SAMPLER = null;
	private static ProblemAnalyzer ANALYZER = null;

	/**
	 * Returns a history sampler.
	 * @return the history sampler.
	 */
	public static HistorySampler getHistory() {
		return SAMPLER;
	}

	/**
	 * Returns a problem analyzer instance.
	 * @return the problem analyzer.
	 */
	public static ProblemAnalyzer getAnalyzer() {
		return ANALYZER;
	}
	/**
	 * The configuration properties.
	 */
	private static Config CONFIG = null;

	/**
	 * Returns the configuration properties (config.properties)
	 * @return the configuration properties.
	 */
	public static Config getConfig() {
		return CONFIG;
	}

	@Override
	protected void init() {
		super.init();
		CONFIG = loadConfig();
		ANALYZER = new ProblemAnalyzer();
		ANALYZER.configure(CONFIG);
		SAMPLER = new HistorySampler(CONFIG, ANALYZER);
		SAMPLER.start();
		mountBookmarkablePage("/graphs.html", Graphs.class);
		mountBookmarkablePage("/problems.html", Problems.class);
		mountBookmarkablePage("/memory.html", Memory.class);
		mountBookmarkablePage("/sysinfo.html", SysInfo.class);
		mountBookmarkablePage("/jndi.html", Jndi.class);
		mountBookmarkablePage("/configure.html", Configure.class);
	}

	@Override
	protected void onDestroy() {
		SAMPLER.stop();
		super.onDestroy();
	}

	private Config loadConfig() {
		String configUrl = getInitParameter("configFile");
		if (configUrl == null) {
			configUrl = "classpath:config.properties";
		}
		try {
			final InputStream in;
			if (configUrl.startsWith("classpath:")) {
				final String resource = configUrl.substring("classpath:".length());
				in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
				if (in == null) {
					throw new IOException("Resource not found: " + resource);
				}
			} else {
				in = new URL(configUrl).openStream();
			}
			try {
				final Properties props = new Properties();
				props.load(in);
				final Config result = new Config();
				final Map<String, String> warnings = Binder.bindBeanMap(result, props, false, true);
				Binder.log(log, warnings);
				return result;
			} finally {
				IOUtils.closeQuietly(in);
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "Failed to load " + configUrl + ", keeping defaults", ex);
		}
		return new Config();
	}
	private static final Logger log = Logger.getLogger(WicketApplication.class.getName());
}
