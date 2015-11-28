package services.io;

import message.Message;
import message.MessageTypes;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;

/**
 * Created by xingchij on 11/17/15.
 */
public class UDPService implements NetService{

    public boolean sendMessage(Message msg, DatagramSocket serverSocket, NetConfig netConf) throws IOException {
        if(netConf == null || msg == null || serverSocket == null) return false;
        ByteArrayOutputStream objBytesStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(objBytesStream);
        out.writeObject(msg);
        out.flush();

        byte[] sendData = objBytesStream.toByteArray();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                netConf.getInetAddress(), netConf.getPort());
        serverSocket.send(sendPacket);

        return true;
    }

    /**
     * It will receive messages, it will block for message.
     * If you prefer a nonblocked mode, DatagramSocket.setSoTimeout() before pass in
     * @param serverSocket
     * @return Message object
     * @throws IOException
     */
    public Message receiveMessage(DatagramSocket serverSocket) throws IOException {
        byte[] recvData = new byte[2048];
        DatagramPacket receivePacket = new DatagramPacket(recvData, recvData.length);
        Message recvMsg = null;
        try {
            serverSocket.receive(receivePacket);
            ByteArrayInputStream objInputBytes = new ByteArrayInputStream(recvData);
            ObjectInputStream objInputStream = new ObjectInputStream(objInputBytes);

            try {
                recvMsg = (Message) objInputStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }catch(SocketTimeoutException e){       // timeout, only if you set the timeout in socket
            //e.printStackTrace();
            return null;
        }catch (IllegalBlockingModeException e){   // connection reset
            e.printStackTrace();
            return null;
        }
        return recvMsg;
    }

    public Message recvAckMessage(DatagramSocket serverSocket) throws IOException {
        byte[] recvData = new byte[2048];
        DatagramPacket receivePacket = new DatagramPacket(recvData, recvData.length);
        Message recvMsg = null;
        try {
            serverSocket.receive(receivePacket);
            ByteArrayInputStream objInputBytes = new ByteArrayInputStream(recvData);
            ObjectInputStream objInputStream = new ObjectInputStream(objInputBytes);

            InetAddress clientAddr = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();
            Message ack = new Message(MessageTypes.ACK, "ack");
            sendMessage(ack, serverSocket,new NetConfig(clientAddr, clientPort));

            try {
                recvMsg = (Message) objInputStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }catch(SocketTimeoutException e){       // timeout, only if you set the timeout in socket
            //e.printStackTrace();
            return null;
        }catch (IllegalBlockingModeException e){   // connection reset
            e.printStackTrace();
            return null;
        }
        return recvMsg;
    }

}
