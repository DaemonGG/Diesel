package services.checkpoint;

import services.io.NetConfig;
import services.io.NetService;
import message.Message;

import java.io.IOException;
import java.net.DatagramSocket;

/**
 * Created by xingchij on 11/17/15.
 */
public class CheckPointService implements NetService {
    public boolean sendMessage(Message msg, DatagramSocket serverSocket, NetConfig netConf) throws IOException {
        return true;
    }

    public Message receiveMessage(DatagramSocket serverSocket) throws IOException {
        return null;
    }

    public Message recvAckMessage(DatagramSocket serverSocket) throws IOException {
        return null;
    }
}
