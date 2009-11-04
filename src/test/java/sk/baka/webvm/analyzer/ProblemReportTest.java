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

import java.util.Arrays;
import java.util.Collections;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

/**
 * Tests {@link ProblemReport}.
 * @author Martin Vysny
 */
public class ProblemReportTest {

    /**
     * Test of isProblem method, of class ProblemReport.
     */
    @Test
    public void testIsProblem_Collection() {
        assertFalse(ProblemReport.isProblem(Collections.<ProblemReport>emptyList()));
        assertFalse(ProblemReport.isProblem(Arrays.asList(new ProblemReport(false, "", "", ""))));
        assertTrue(ProblemReport.isProblem(Arrays.asList(new ProblemReport(true, "", "", ""))));
    }

    /**
     * Test of escape method, of class ProblemReport.
     */
    @Test
    public void testEscape() {
        assertEquals("&lt;foo&gt;", ProblemReport.escape("<foo>"));
    }

    /**
     * Test of equals method, of class ProblemReport.
     */
    @Test
    public void testEquals_Object() {
        assertFalse(new ProblemReport(false, "", "", "").equals(null));
        assertFalse(new ProblemReport(false, "", "", "").equals(new ProblemReport(true, "", "", "")));
        assertFalse(new ProblemReport(false, "", "", "").equals(new ProblemReport(false, "foo", "", "")));
        assertTrue(new ProblemReport(false, "foo", "", "").equals(new ProblemReport(false, "foo", "", "")));
    }

    /**
     * Test of equals method, of class ProblemReport.
     */
    @Test
    public void testEquals_Collection_Collection() {
        assertTrue(ProblemReport.equals(Arrays.asList(new ProblemReport(false, "foo", "", "")), Collections.<ProblemReport>emptyList()));
        assertFalse(ProblemReport.equals(Arrays.asList(new ProblemReport(true, "foo", "", "")), Collections.<ProblemReport>emptyList()));
        assertTrue(ProblemReport.equals(Arrays.asList(new ProblemReport(true, "foo", "", "")), Arrays.asList(new ProblemReport(true, "foo", "", ""))));
        assertFalse(ProblemReport.equals(Arrays.asList(new ProblemReport(true, "foo", "", "")), Arrays.asList(new ProblemReport(true, "bar", "", ""))));
        assertFalse(ProblemReport.equals(Arrays.asList(new ProblemReport(true, "foo", "", "")), Arrays.asList(new ProblemReport(false, "bar", "", ""))));
        assertTrue(ProblemReport.equals(Arrays.asList(new ProblemReport(true, "foo", "", "")), Arrays.asList(new ProblemReport(true, "foo", "", ""), new ProblemReport(false, "", "", ""))));
    }
}
