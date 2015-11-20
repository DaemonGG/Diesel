package services.common;

import services.io.NetConfig;
import services.io.NetService;
import message.Message;

import java.io.IOException;
import java.net.DatagramSocket;

/**
 * Created by xingchij on 11/17/15.
 */
public class NetServiceProxy implements services.io.NetService{
    NetService netService = null;

    public NetServiceProxy(NetService netService){
        this.netService = netService;
    }

    public boolean sendMessage(Message msg, DatagramSocket serverSocket, NetConfig netConf) throws IOException {
        return this.netService.sendMessage(msg, serverSocket, netConf);
    }

    public Message receiveMessage(DatagramSocket serverSocket) throws IOException {
        return this.netService.receiveMessage(serverSocket);
    }

    public Message recvAckMessage(DatagramSocket serverSocket) throws IOException {
        return netService.recvAckMessage(serverSocket);
    }
}
