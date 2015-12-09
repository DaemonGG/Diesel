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
	private static final int QUERY_PORT = 12354, DB_QUERY_PORT = 12355;
	private static final String IMG_LOC = "imgs/";

	private String id;
	private String ip;

	public TaskExecutor(String id, String ip) {
		this.id = id;
		this.ip = ip;
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
					System.out.println("Received test");
					val = job.getJobId();
					jobID = Integer.parseInt(job.getJobId());
					setJobStatus(jobID, JobStatus.RUNNING);
					System.setProperty("url", job.getValue());
					System.setProperty("image_count", "1");
					System.out.println("Running the testing");
					junit.run(SingleTest.class);
					System.out.println("Done executing test");
					File dir = new File(IMG_LOC);
					File[] directoryListing = dir.listFiles();
					if (directoryListing != null && directoryListing.length > 0) {
						for (File child : directoryListing) {
							insertImage(child, val);
						}
						System.out.println("Saved photos");
						setJobStatus(jobID, JobStatus.DONE);
						msg = ReportConstructor.generateReport(this.id, val,
								JobSettings.JOB_SUCCESS);
					} else {
						setJobStatus(jobID, JobStatus.FAILED);
						msg = ReportConstructor.generateReport(this.id, val,
								JobSettings.JOB_FAIL);
					}
					DatagramSocket socket = new DatagramSocket(8031);
					while (!sendMessage(msg, socket, getPrimary()))
						;
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
	}

	private NetConfig getPrimary() throws IOException,
			WrongMessageTypeException {
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

	private NetConfig getMongo() throws IOException, WrongMessageTypeException {
		DatagramSocket socket = new DatagramSocket(DB_QUERY_PORT);
		Message msg = WhoIsPrimaryConstructor.constructQuery(this.ip,
				DB_QUERY_PORT);
		NetServiceProxy proxy = NetServiceFactory.getRawUDPService();
		System.out.println("Finished setup");
		proxy.sendMessage(msg, socket, new NetConfig(
				ConnMetrics.IPOfCoordinator,
				ConnMetrics.portOfCoordinatorForPrimaryDB));
		System.out.println("Message sent");
		Message response = proxy.receiveMessage(socket);
		System.out.println("Response received");
		socket.close();
		if (response.getType() == MessageTypes.WHOISPRIMARY) {
			JSONObject obj = new JSONObject(response.getContent());
			String ip = obj.getString("dbip");

			System.out.println("GOT DBIP : " + ip);
			return (ip.equals("None")) ? null : new NetConfig(ip,
					ConnMetrics.portReceiveReport);
		} else {
			System.out.println("FAIL");
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
		MongoClient client = null;
		try {
			client = new MongoClient("128.237.191.159",
					ConnMetrics.portOfMongoDB);
			MongoDatabase db = client.getDatabase(ConnMetrics.DB_NAME);

			MongoCollection<Document> result = db.getCollection("result");
			FileInputStream stream = new FileInputStream(file);

			byte b[] = new byte[stream.available()];
			stream.read(b);

			result.insertOne(new Document("photo", new Binary(b)).append(
					"name", jobID));
			System.out.println("Inserted record.");

			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (client != null) {
				client.close();
			}
		}
	}

	private void setJobStatus(int jobID, JobStatus status) {
		System.out.println("Getting job status : " + jobID);
		MongoClient client = null;
		try {
			// NetConfig config = getMongo();
			// if (config == null) {
			// System.out.println("CRASH");
			// } else {
			// System.out.println(config.getIP());
			// }
			client = new MongoClient("128.237.191.159",
					ConnMetrics.portOfMongoDB);
			MongoDatabase db = client.getDatabase(ConnMetrics.DB_NAME);

			db.getCollection("job").updateOne(
					new Document("jobID", jobID),
					new Document("$set", new Document("status", status
							.getStatus())));
		} finally {
			if (client != null) {
				client.close();
			}
		}
	}
}
