package slave;

abstract public class AbstractAppiumExecutionService implements Runnable {
	protected static final int TIMEOUT = 1000;

	protected AppiumServer server;
	protected boolean runCondition = true;

	public void registerServer(AppiumServer server) {
		this.server = server;
	}

	abstract public void close();
}
