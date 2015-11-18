package common;

import io.NetConfig;
import io.NetService;
import message.Message;

import java.io.IOException;
import java.net.DatagramSocket;

/**
 * Created by xingchij on 11/17/15.
 */
public class NetServiceProxy implements io.NetService{
    NetService netService = null;

    public NetServiceProxy(NetService netService){
        this.netService = netService;
    }

    public void sendMessage(Message msg, DatagramSocket serverSocket, NetConfig netConf) throws IOException {
        this.netService.sendMessage(msg, serverSocket, netConf);
    }

    public Message nonBlockForMessage(DatagramSocket serverSocket) throws IOException {
        return this.netService.nonBlockForMessage(serverSocket);
    }
}
