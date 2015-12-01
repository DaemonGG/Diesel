package slave;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.bson.Document;
import org.bson.types.Binary;
import org.junit.runner.JUnitCore;

import shared.ConnMetrics;
import shared.Job;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class TaskExecutor extends AbstractAppiumExecutionService {

	private static final String IMG_LOC = "imgs/";

	private MongoClient client;
	private MongoCollection<Document> job, result;
	private MongoDatabase db;

	public TaskExecutor() {
		this.client = new MongoClient(ConnMetrics.IPOfMongoDB,
				ConnMetrics.portOfMongoDB);
		this.db = client.getDatabase(ConnMetrics.DB_NAME);
		this.job = db.getCollection("job");
		this.result = db.getCollection("result");
	}

	@Override
	public void run() {
		Job job;
		JUnitCore junit = new JUnitCore();
		String val;
		int intVal;
		while (this.runCondition) {
			try {
				job = this.server.removeJob();
				if (job == null) {
					System.out.println("Executor Sleeping");
					Thread.sleep(TIMEOUT);
				} else {
					val = job.getValue();
					intVal = Integer.parseInt(val);
					setJobStatus(intVal, JobStatus.RUNNING);
					System.setProperty("url", val);
					System.setProperty("image_count", "1");
					junit.run(SingleTest.class);
					File dir = new File(IMG_LOC);
					File[] directoryListing = dir.listFiles();
					if (directoryListing != null && directoryListing.length > 0) {
						for (File child : directoryListing) {
							insertImage(child, val);
						}
						System.out.println("Saved photos");
						setJobStatus(intVal, JobStatus.DONE);
					} else {
						setJobStatus(intVal, JobStatus.FAILED);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		this.runCondition = false;
		this.client.close();
	}

	private void insertImage(File file, String jobID) {
		try {
			FileInputStream stream = new FileInputStream(file);

			byte b[] = new byte[stream.available()];
			stream.read(b);

			this.result.insertOne(new Document("photo", new Binary(b)).append(
					"name", jobID));
			System.out.println("Inserted record.");

			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setJobStatus(int jobID, JobStatus status) {
		this.job.updateOne(new Document("jobID", jobID), new Document("$set",
				new Document("status", status.getStatus())));
	}
}
