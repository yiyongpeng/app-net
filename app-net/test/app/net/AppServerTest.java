package app.net;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AppServerTest extends TestCase {
	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public AppServerTest(String testName) {
		super(testName);

	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppServerTest.class);
	}

	public void testInstance() {
		AppServer server = new AppServer();
		server.setPort(123);
		assertTrue(!server.isRuning());
	}

	public void testStartup() {
		AppServer server = new AppServer();
		server.setPort(123);
		server.start();
		assertTrue(server.isRuning());
		server.stop();
	}

	public void testShutdown() {
		AppServer server = new AppServer();
		server.setPort(123);
		server.start();
		server.stop();
		assertTrue(!server.isRuning());
	}
}
