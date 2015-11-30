package services.troop;

import java.io.IOException;
import java.net.DatagramSocket;

import message.Message;
import services.io.NetConfig;
import services.io.NetService;
import services.io.UDPService;

/**
 * Created by xingchij on 11/17/15.
 */
public class CommandService implements NetService {
	private NetService imp = new UDPService();

	public boolean sendMessage(Message msg, DatagramSocket serverSocket,
			NetConfig netConf) throws IOException {
		if (imp.sendMessage(msg, serverSocket, netConf) == false)
			return false;

		serverSocket.setSoTimeout(5000);

		Message ack = imp.receiveMessage(serverSocket);

		if (ack == null) {
			System.out.println("wait for ack after send command. timeout");
			return false;
		}
		serverSocket.close();
		return true;
	}

	public Message receiveMessage(DatagramSocket serverSocket)
			throws IOException {
		return null;
	}

	public Message recvAckMessage(DatagramSocket serverSocket)
			throws IOException {
		return imp.recvAckMessage(serverSocket);
	}
}
