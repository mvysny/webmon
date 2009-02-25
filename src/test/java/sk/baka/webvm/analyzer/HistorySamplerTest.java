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
package sk.baka.webvm.analyzer;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/**
 * Tests the {@link HistorySampler}.
 * @author Martin Vysny
 */
public class HistorySamplerTest extends TestCase {

    /**
     * Test of getProblemHistory method, of class HistorySampler.
     */
    public void testGetProblemHistory() throws Exception {
        final HistorySampler hs = new HistorySampler(new SamplerConfig(100, Integer.MAX_VALUE, Integer.MAX_VALUE), new SamplerConfig(100, 50, 0), new ProblemAnalyzer());
        hs.start();
        try {
            Thread.sleep(100);
            List<List<ProblemReport>> history = hs.getProblemHistory();
            assertEquals(new ArrayList<Object>(), history);
            final Deadlock d = new Deadlock();
            d.simulate();
            try {
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
