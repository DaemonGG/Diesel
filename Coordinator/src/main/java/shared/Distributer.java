package shared;

import message.Message;
import message.msgconstructor.HeartBeatConstructor;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by xingchij on 11/19/15.
 */
public abstract class Distributer implements ConnMetrics {

    public AllSlaves slaveOffice;
    public AllSecondaries backUps;
    public int identity;

    public String ip;
    public String id;

    public NetConfig coordinator;
    private NetServiceProxy heartBeatService = NetServiceFactory.getHeartBeatService();

    public abstract boolean dealWithMemberShipMsg(Message msg);

    public void sendHeartBeat() throws IOException {
        heartBeatService.sendMessage(HeartBeatConstructor.constructHeartBeat(id, ip),
                new DatagramSocket(), coordinator);
    }

}
