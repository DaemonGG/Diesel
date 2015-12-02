package slave;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import message.Message;
import message.MessageTypes;
import message.msgconstructor.WhoIsPrimaryConstructor;

import org.json.JSONObject;

import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import shared.ConnMetrics;
import shared.Job;
import distributor.Distributer;
import error.WrongMessageTypeException;

public class AppiumServer extends Distributer {
	private static final int QUERY_PORT = 12345;
	private static final int TIMEOUT = 1000;
	private Queue<Job> taskQueue;
	private ConnectionHandler connectionHandler;
	private TaskExecutor taskExecutor;

	public AppiumServer() throws SocketException {
		this.ip = NetConfig.getMyIp();
		this.id = UUID.randomUUID().toString();
		this.taskQueue = new ConcurrentLinkedQueue<Job>();
		this.connectionHandler = new ConnectionHandler();
		this.taskExecutor = new TaskExecutor(this.id, this.ip);
	}

	@Override
	public void serve() {
		try {
			System.out.println("Getting Primary");
			getPrimary();
			System.out.println("Primary Received...starting to serve");
			this.connectionHandler.registerServer(this);
			this.taskExecutor.registerServer(this);
			new Thread(this.connectionHandler).start();
			new Thread(this.taskExecutor).start();
			int i = 0;
			while(true) {
				i++;
				sendHeartBeat();
				if (i == 5) {
					getPrimary();
					i = 0;
				}
				Thread.sleep(TIMEOUT);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (WrongMessageTypeException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void getPrimary() throws IOException, WrongMessageTypeException {
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
			this.coordinator = new NetConfig(obj.getString("ip"),
					ConnMetrics.portReceiveHeartBeatFromSlave);
		} else {
			throw new WrongMessageTypeException(response.getType(),
					MessageTypes.WHOISPRIMARY);
		}
	}

	public void closeConnections() {
		this.taskExecutor.close();
		this.connectionHandler.close();
	}

	public boolean addJob(Job job) {
		return this.taskQueue.add(job);
	}

	public Job removeJob() throws NoSuchElementException {
		return this.taskQueue.poll();
	}

	@Override
	public boolean dealWithMemberShipMsg(Message msg) {
		return false;
	}

	public static void main(String[] args) throws SocketException {
		new AppiumServer().serve();
	}
}