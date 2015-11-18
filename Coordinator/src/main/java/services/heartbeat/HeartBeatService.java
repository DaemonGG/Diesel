package services.heartbeat;

import services.io.NetConfig;
import services.io.NetService;
import services.io.UDPService;
import message.Message;

import java.io.IOException;
import java.net.DatagramSocket;

/**
 * Created by xingchij on 11/17/15.
 */
public class HeartBeatService implements NetService {

    private NetService imp = new UDPService();

    public void sendMessage(Message msg, DatagramSocket serverSocket, NetConfig netConf) throws IOException {
        imp.sendMessage(msg, serverSocket, netConf);
    }

    public Message receiveMessage(DatagramSocket serverSocket) throws IOException {
        return null;
    }
}
