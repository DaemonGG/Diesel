package services.heartbeat;

import message.Message;
import services.io.NetConfig;
import services.io.NetService;
import services.io.UDPService;

import java.io.IOException;
import java.net.DatagramSocket;

/**
 * Created by xingchij on 11/17/15.
 */
public class HeartBeatService implements NetService {

	private NetService imp = new UDPService();

	public boolean sendMessage(Message msg, DatagramSocket serverSocket,
			NetConfig netConf) throws IOException {
		if (imp.sendMessage(msg, serverSocket, netConf) == false)
			return false;
		serverSocket.close();
		return true;
	}

	public Message receiveMessage(DatagramSocket serverSocket)
			throws IOException {
		return imp.receiveMessage(serverSocket);
	}

	/**
	 * will not be used heart beat need no ack
	 */
	public Message recvAckMessage(DatagramSocket serverSocket)
			throws IOException {
		return null;
	}
}
