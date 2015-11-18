package checkpoint;

import io.NetConfig;
import io.NetService;
import message.Message;

import java.io.IOException;
import java.net.DatagramSocket;

/**
 * Created by xingchij on 11/17/15.
 */
public class CheckPointService implements NetService {
    public void sendMessage(Message msg, DatagramSocket serverSocket, NetConfig netConf) throws IOException {

    }

    public Message nonBlockForMessage(DatagramSocket serverSocket) throws IOException {
        return null;
    }
}
