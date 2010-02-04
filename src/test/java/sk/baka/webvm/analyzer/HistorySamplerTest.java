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
package sk.baka.webvm.analyzer;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.SystemUtils;
import org.testng.annotations.Test;
import sk.baka.webvm.analyzer.hostos.IMemoryInfoProvider;
import sk.baka.webvm.analyzer.hostos.MemoryJMXStrategy;
import static org.testng.AssertJUnit.*;

/**
 * Tests the {@link HistorySampler}.
 * @author Martin Vysny
 */
public class HistorySamplerTest {

    /**
     * Test of getProblemHistory method, of class HistorySampler.
     * @throws Exception 
     */
    @Test
    public void testGetProblemHistory() throws Exception {
        if (!SystemUtils.isJavaVersionAtLeast(160)) {
            System.out.println("Skipping HistorySamplerTest.testGetProblemHistory() as Java 1.5.x does not report deadlock in Locks");
            return;
        }
        final Injector inj = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                bind(IMemoryInfoProvider.class).to(MemoryJMXStrategy.class);
            }
        });
        final HistorySampler hs = new HistorySampler(new SamplerConfig(100, Integer.MAX_VALUE, Integer.MAX_VALUE), new SamplerConfig(100, 50, 0));
        inj.injectMembers(hs);
        hs.start();
        try {
            Thread.sleep(100);
            List<List<ProblemReport>> history = hs.getProblemHistory();
            assertEquals(new ArrayList<Object>(), history);
            final Deadlock d = new Deadlock();
            d.simulate();
            try {
                d.checkThreads();
                Thread.sleep(200);
                history = hs.getProblemHistory();
                assertEquals(1, history.size());
            } finally {
                d.cancel();
            }
            Thread.sleep(200);
            history = hs.getProblemHistory();
            assertEquals(2, history.size());
        } finally {
            hs.stop();
        }
    }
}
