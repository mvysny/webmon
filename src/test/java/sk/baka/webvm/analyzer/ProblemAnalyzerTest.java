package sk.baka.webvm.analyzer;

import junit.framework.TestCase;

/**
 * Tests the {@link ProblemAnalyzer}.
 * @author Martin Vysny
 */
public class ProblemAnalyzerTest extends TestCase {

	/**
	 * Test of getDeadlockReport method, of class ProblemAnalyzer.
	 */
	public void testGetDeadlockReport() throws Exception {
		final Deadlock d = new Deadlock();
		d.simulate();
		try {
			final ProblemReport pr = ProblemAnalyzer.getDeadlockReport();
			System.out.println(pr.getDesc());
			assertTrue(pr.isProblem());
			assertTrue(pr.getDesc().contains("deadlock1"));
			assertTrue(pr.getDesc().contains("deadlock2"));
			// check for the stack-trace presence
			assertTrue(pr.getDesc().contains(Deadlock.class.getName()));
			assertTrue(pr.getDesc().contains("run("));
		} finally {
			d.cancel();
		}
	}
}
