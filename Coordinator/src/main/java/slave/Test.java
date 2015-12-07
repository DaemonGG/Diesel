package slave;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.bson.Document;
import org.bson.types.Binary;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class Test {

	private static MongoClient client;
	private static MongoCollection<Document> job, result;
	private static MongoDatabase db;

	public static void main(String[] args) throws InterruptedException {
		String myCommand = "cd ~/Desktop; touch harinisboobs; cp hello harinisboobs";
		try {
			Runtime.getRuntime().exec(myCommand);
			System.out.println("HERE");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// client = new MongoClient(ConnMetrics.IPOfMongoDB,
		// ConnMetrics.portOfMongoDB);
		//
		// db = client.getDatabase(ConnMetrics.DB_NAME);
		// job = db.getCollection("job");
		// result = db.getCollection("result");
		// setJobStatus(6353, JobStatus.FAILED);
		//
		// client.close();
	}

	private static void insertImage(File file, String jobID) {
		try {
			FileInputStream stream = new FileInputStream(file);

			byte b[] = new byte[stream.available()];
			stream.read(b);

			result.insertOne(new Document("photo", new Binary(b)).append(
					"name", jobID));
			System.out.println("Inserted record.");

			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void setJobStatus(int jobID, JobStatus status) {
		job.updateOne(new Document("jobID", jobID), new Document("$set",
				new Document("status", status.getStatus())));
	}
}
