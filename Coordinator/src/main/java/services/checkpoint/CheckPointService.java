package services.checkpoint;

import message.Message;
import services.io.NetConfig;
import services.io.NetService;
import services.io.UDPService;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

/**
 * Created by xingchij on 11/17/15.
 */
public class CheckPointService implements NetService {
	private NetService imp = new UDPService();

	/**
	 * for now, I will send checkpoint to each secondary one by one. Any one
	 * fails midway will just be skipped, and will not affect the following ones
	 * 
	 * @param msg
	 * @param serverSocket
	 * @param netConf
	 * @return
	 * @throws IOException
	 */
	public boolean sendMessage(Message msg, DatagramSocket serverSocket,
			NetConfig netConf) throws IOException {

		List<InetAddress> secondaries = netConf.getBrdCastAddr();
		int port = netConf.getPort();
		serverSocket.setSoTimeout(100000);

		for (InetAddress addr : secondaries) {
			try {
				imp.sendMessage(msg, serverSocket, new NetConfig(addr, port));

				Message ack = imp.receiveMessage(serverSocket);

				if (ack == null) {
					System.out.printf("Send checkpoint to %s fail\n",
							addr.getHostAddress() + port);
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.printf("Send checkpoint to %s fail\n",
						addr.getHostAddress() + port);
			}
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
