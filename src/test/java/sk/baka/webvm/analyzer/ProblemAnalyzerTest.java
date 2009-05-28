package sk.baka.webvm.analyzer;

import org.junit.Test;
import static org.junit.Assert.*;

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
