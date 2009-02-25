package sk.baka.webvm.analyzer;

import java.util.Arrays;
import java.util.Collections;
import junit.framework.TestCase;

/**
 * Tests {@link ProblemReport}.
 * @author Martin Vysny
 */
public class ProblemReportTest extends TestCase {

	/**
	 * Test of isProblem method, of class ProblemReport.
	 */
	public void testIsProblem_Collection() {
		assertFalse(ProblemReport.isProblem(Collections.<ProblemReport>emptyList()));
		assertFalse(ProblemReport.isProblem(Arrays.asList(new ProblemReport(false, "", "", ""))));
		assertTrue(ProblemReport.isProblem(Arrays.asList(new ProblemReport(true, "", "", ""))));
	}

	/**
	 * Test of escape method, of class ProblemReport.
	 */
	public void testEscape() {
		assertEquals("&lt;foo&gt;", ProblemReport.escape("<foo>"));
	}

	/**
	 * Test of equals method, of class ProblemReport.
	 */
	public void testEquals_Object() {
		assertFalse(new ProblemReport(false, "", "", "").equals(null));
		assertFalse(new ProblemReport(false, "", "", "").equals(new ProblemReport(true, "", "", "")));
		assertFalse(new ProblemReport(false, "", "", "").equals(new ProblemReport(false, "foo", "", "")));
		assertTrue(new ProblemReport(false, "foo", "", "").equals(new ProblemReport(false, "foo", "", "")));
	}

	/**
	 * Test of equals method, of class ProblemReport.
	 */
	public void testEquals_Collection_Collection() {
		assertTrue(ProblemReport.equals(Arrays.asList(new ProblemReport(false, "foo", "", "")), Collections.<ProblemReport>emptyList()));
		assertFalse(ProblemReport.equals(Arrays.asList(new ProblemReport(true, "foo", "", "")), Collections.<ProblemReport>emptyList()));
		assertTrue(ProblemReport.equals(Arrays.asList(new ProblemReport(true, "foo", "", "")), Arrays.asList(new ProblemReport(true, "foo", "", ""))));
		assertFalse(ProblemReport.equals(Arrays.asList(new ProblemReport(true, "foo", "", "")), Arrays.asList(new ProblemReport(true, "bar", "", ""))));
		assertFalse(ProblemReport.equals(Arrays.asList(new ProblemReport(true, "foo", "", "")), Arrays.asList(new ProblemReport(false, "bar", "", ""))));
		assertTrue(ProblemReport.equals(Arrays.asList(new ProblemReport(true, "foo", "", "")), Arrays.asList(new ProblemReport(true, "foo", "", ""), new ProblemReport(false, "", "", ""))));
	}
}
