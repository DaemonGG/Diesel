package slave;

import org.junit.runner.JUnitCore;

import shared.Job;

public class TaskExecutor extends AbstractAppiumExecutionService {

	@Override
	public void run() {
		Job job;
		JUnitCore junit = new JUnitCore();
		while (this.runCondition) {
			try {
				job = this.server.removeJob();
				if (job == null) {
					Thread.sleep(TIMEOUT);
				} else {
					System.setProperty("url", job.getValue());
					System.setProperty("image_count", "1");
					junit.run(SingleTest.class);
					// TODO send response back
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		this.runCondition = false;
	}
}
