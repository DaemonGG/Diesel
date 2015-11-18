package io;

import message.Message;

import java.io.IOException;
import java.net.DatagramSocket;

/**
 * Created by xingchij on 11/17/15.
 */
public interface NetService {
    public void sendMessage(Message msg, DatagramSocket serverSocket, NetConfig netConf) throws IOException;
    public Message nonBlockForMessage(DatagramSocket serverSocket) throws IOException;
}
