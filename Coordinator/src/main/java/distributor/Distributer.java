package distributor;

import message.Message;
import message.msgconstructor.HeartBeatConstructor;
import org.json.JSONArray;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import shared.AllSecondaries;
import shared.AllSlaves;
import shared.ConnMetrics;
import shared.Job;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.Queue;
import java.util.Set;

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
	public Set<Job> unfinishedJobSet;

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

	public String toString(){
		if(ip == null || id == null || slaveOffice == null || backUps == null || unfinishedJobSet == null)
			return null;

		StringBuilder output = new StringBuilder();
		output.append(String.format("id: %s\tip: %s\n", id, ip));
		output.append(String.format("Slaves: %s\n", slaveOffice.dump().toString()));
		output.append(String.format("Secondaries: %s\n", backUps.dump().toString()));
		output.append(String.format("UnfinishedQueue: %s\n", dumpQueue().toString()));
		return output.toString();
	}
	public JSONArray dumpQueue(){
		JSONArray array = new JSONArray();
		if(unfinishedJobSet == null)
			return array;
		for(Job j : unfinishedJobSet){
			array.put(j.getJobInJson());
		}

		return array;
	}

}
