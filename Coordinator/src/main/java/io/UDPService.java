package io;

import message.Message;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by xingchij on 11/17/15.
 */
public class UDPService implements NetService{

    public void sendMessage(Message msg, DatagramSocket serverSocket, NetConfig netConf) throws IOException {
        ByteArrayOutputStream objBytesStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(objBytesStream);
        out.writeObject(msg);
        out.flush();

        byte[] sendData = objBytesStream.toByteArray();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                netConf.getInetAddress(), netConf.getPort());
        serverSocket.send(sendPacket);
    }

    /**
     * It will receive messages, it will not block for message.
     * If you prefer a blocked mode, DatagramSocket.setSoTimeout() before pass in
     * @param serverSocket
     * @return Message object
     * @throws IOException
     */
    public Message nonBlockForMessage(DatagramSocket serverSocket) throws IOException {
        byte[] recvData = new byte[2048];
        DatagramPacket receivePacket = new DatagramPacket(recvData, recvData.length);

        serverSocket.receive(receivePacket);
        ByteArrayInputStream objInputBytes = new ByteArrayInputStream(recvData);
        ObjectInputStream  objInputStream= new ObjectInputStream(objInputBytes);
        Message recvMsg = null;
        try {
            recvMsg = (Message) objInputStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
