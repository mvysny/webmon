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

import org.apache.commons.lang.SystemUtils;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Tests the {@link ProblemAnalyzer}.
 * @author Martin Vysny
 */
public class ProblemAnalyzerTest {

    /**
     * Test of getDeadlockReport method, of class ProblemAnalyzer.
     * @throws Exception
     */
    @Test
    public void testGetDeadlockReport() throws Exception {
        if (!SystemUtils.isJavaVersionAtLeast(160)) {
            System.out.println("Skipping ProblemAnalyzerTest.testGetDeadlockReport() as Java 1.5.x does not report deadlock in Locks");
            return;
        }
        final Deadlock d = new Deadlock();
        d.simulate();
        try {
            d.checkThreads();
            final ProblemReport pr = ProblemAnalyzer.getDeadlockReport();
            assertTrue(pr.isProblem);
            assertTrue(pr.diagnosis.contains("deadlock1"));
            assertTrue(pr.diagnosis.contains("deadlock2"));
            // check for the stack-trace presence
            assertTrue(pr.diagnosis.contains(Deadlock.class.getName()));
            assertTrue(pr.diagnosis.contains("run("));
        } finally {
            d.cancel();
        }
    }
}
