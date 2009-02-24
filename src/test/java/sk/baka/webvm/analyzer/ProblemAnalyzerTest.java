package sk.baka.webvm.analyzer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
		final Lock lock1 = new ReentrantLock();
		final Lock lock2 = new ReentrantLock();
		final Thread t1 = new Thread("deadlock1") {

			@Override
			public void run() {
				lock1.tryLock();
				try {
					Thread.sleep(500);
					lock2.tryLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
				} catch (InterruptedException ex) {
					// okay
				}
			}
		};
		t1.setDaemon(true);
		final Thread t2 = new Thread("deadlock2") {

			@Override
			public void run() {
				lock2.tryLock();
				try {
					Thread.sleep(500);
					lock1.tryLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
				} catch (InterruptedException ex) {
					// okay
				}
			}
		};
		t2.setDaemon(true);
		t1.start();
		t2.start();
		try {
			Thread.sleep(1000);
			// t1 and t2 should form a deadlock by now
			final ProblemReport pr = ProblemAnalyzer.getDeadlockReport();
			System.out.println(pr.getDesc());
			assertTrue(pr.isProblem());
			assertTrue(pr.getDesc().contains("deadlock1"));
			assertTrue(pr.getDesc().contains("ProblemAnalyzerTest"));
			assertTrue(pr.getDesc().contains("run("));
			assertTrue(pr.getDesc().contains("deadlock2"));
		} finally {
			t1.interrupt();
			t2.interrupt();
			t1.join();
			t2.join();
		}
	}
}
