package slave;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

import message.Message;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import shared.ConnMetrics;
import shared.Job;
import error.WrongMessageTypeException;

public class ConnectionHandler extends AbstractAppiumExecutionService {
	private DatagramSocket getTasksDock;
	private NetServiceProxy commandService;

	public ConnectionHandler() throws SocketException {
		this.commandService = NetServiceFactory.getCommandService();
		this.getTasksDock = new DatagramSocket(
				ConnMetrics.portOfSlaveDelegateTask);
		this.getTasksDock.setSoTimeout(TIMEOUT);
	}

	@Override
	public void run() {
		Message msg;
		while (this.runCondition) {
			try {
				msg = this.commandService.recvAckMessage(this.getTasksDock);
				if (msg != null) {
					this.server.addJob(Job.getJobFromDelegateMsg(msg));
				} else {
					Thread.sleep(TIMEOUT);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (WrongMessageTypeException e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		this.runCondition = false;
		if (this.getTasksDock != null) {
			this.getTasksDock.close();
		}
	}
}
