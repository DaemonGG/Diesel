package slave;

import java.net.SocketException;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import message.Message;
import services.io.NetConfig;
import shared.Job;
import distributor.Distributer;

public class AppiumServer extends Distributer {
	private Queue<Job> taskQueue;
	private ConnectionHandler connectionHandler;
	private TaskExecutor taskExecutor;

	public AppiumServer() throws SocketException {
		this.ip = NetConfig.getMyIp();
		this.id = UUID.randomUUID().toString();
		this.taskQueue = new ConcurrentLinkedQueue<Job>();
		this.connectionHandler = new ConnectionHandler();
		this.taskExecutor = new TaskExecutor();
	}

	@Override
	public void serve() {
		this.connectionHandler.run();
		this.taskExecutor.run();
	}

	public void closeConnections() {
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
}