package distributor;

import java.io.IOException;
import java.net.DatagramSocket;

import message.Message;
import message.msgconstructor.HeartBeatConstructor;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import shared.AllSecondaries;
import shared.AllSlaves;
import shared.ConnMetrics;

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
	private NetServiceProxy heartBeatService = NetServiceFactory
			.getHeartBeatService();

	public abstract boolean dealWithMemberShipMsg(Message msg);

	public void sendHeartBeat() throws IOException {
		if (coordinator == null) {
			System.out
					.println("Err: Coordinator network connection not initialized!");
			return;
		}
		heartBeatService.sendMessage(
				HeartBeatConstructor.constructHeartBeat(id, ip),
				new DatagramSocket(), coordinator);
	}

}
