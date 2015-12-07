package slave;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import error.WrongMessageTypeException;
import message.Message;
import message.MessageTypes;
import message.msgconstructor.ReportConstructor;
import message.msgconstructor.WhoIsPrimaryConstructor;
import org.bson.Document;
import org.bson.types.Binary;
import org.json.JSONObject;
import org.junit.runner.JUnitCore;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import services.io.NetService;
import services.io.UDPService;
import shared.ConnMetrics;
import shared.Job;
import shared.JobSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramSocket;

public class TaskExecutor extends AbstractAppiumExecutionService {
	private static final int QUERY_PORT = 12354;
	private static final String IMG_LOC = "imgs/";

	private String id;
	private MongoClient client;
	private MongoCollection<Document> job, result;
	private MongoDatabase db;
	private String ip;

	public TaskExecutor(String id, String ip) {
		this.id = id;
		this.ip = ip;
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
		int jobID;
		Message msg;
		while (this.runCondition) {
			try {
				job = this.server.removeJob();
				if (job == null) {
					Thread.sleep(TIMEOUT);
				} else {
					System.out.println("Executing test");
					val = job.getJobId();
					jobID = Integer.parseInt(job.getJobId());
					setJobStatus(jobID, JobStatus.RUNNING);
					System.setProperty("url", job.getValue());
					System.setProperty("image_count", "1");
					junit.run(SingleTest.class);
					File dir = new File(IMG_LOC);
					File[] directoryListing = dir.listFiles();
					if (directoryListing != null && directoryListing.length > 0) {
						for (File child : directoryListing) {
							insertImage(child, val);
						}
						System.out.println("Saved photos");
						setJobStatus(jobID, JobStatus.DONE);
						msg=ReportConstructor.generateReport(this.id, val, JobSettings.JOB_SUCCESS);
					} else {
						setJobStatus(jobID, JobStatus.FAILED);
						msg=ReportConstructor.generateReport(this.id, val, JobSettings.JOB_FAIL);
					}
					DatagramSocket socket = new DatagramSocket(8031);
					while(!sendMessage(msg, socket, getPrimary()));
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WrongMessageTypeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void close() {
		this.runCondition = false;
		this.client.close();
	}
	
	private NetConfig getPrimary() throws IOException, WrongMessageTypeException {
		DatagramSocket socket = new DatagramSocket(QUERY_PORT);
		Message msg = WhoIsPrimaryConstructor.constructQuery(this.ip,
				QUERY_PORT);
		NetServiceProxy proxy = NetServiceFactory.getRawUDPService();
		proxy.sendMessage(msg, socket, new NetConfig(
				ConnMetrics.IPOfCoordinator,
				ConnMetrics.portOfCoordinatorForPrimaryAddr));
		Message response = proxy.receiveMessage(socket);
		socket.close();
		if (response.getType() == MessageTypes.WHOISPRIMARY) {
			JSONObject obj = new JSONObject(response.getContent());
			return new NetConfig(obj.getString("ip"),
					ConnMetrics.portReceiveReport);
		} else {
			throw new WrongMessageTypeException(response.getType(),
					MessageTypes.WHOISPRIMARY);
		}
	}
	
	private boolean sendMessage(Message msg, DatagramSocket serverSocket,
			NetConfig netConf) throws IOException {
		NetService imp = new UDPService();
		if (imp.sendMessage(msg, serverSocket, netConf) == false)
			return false;

		serverSocket.setSoTimeout(5000);

		Message ack = imp.receiveMessage(serverSocket);

		if (ack == null) {
			System.out.println("wait for ack after send sendMessage. timeout");
			return false;
		}
		serverSocket.close();
		return true;
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
