package distributor.secondary;

import distributor.Distributer;
import error.WrongMessageTypeException;
import message.Message;
import message.MessageTypes;
import message.msgconstructor.CheckPointConstructor;
import message.msgconstructor.MemberShipConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import shared.*;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by xingchij on 11/19/15.
 */
public class Secondary extends Distributer {
	private DatagramSocket getCheckPointDock;

	NetServiceProxy checkPointService = NetServiceFactory
			.getCheckPointService();

	public Secondary(String id) throws SocketException, UnknownHostException {
		ip = NetConfig.getMyIp();
		this.id = id;
		coordinator = new NetConfig(IPOfCoordinator, portOfCoordinatorHeartBeat);
		unfinishedJobSet = new HashSet<Job>();

		slaveOffice = AllSlaves.getOffice();
		slaveOffice.refreshAll();

		backUps = AllSecondaries.getInstance();

		getCheckPointDock = new DatagramSocket(
				ConnMetrics.portOfSecondaryCheckPoint);

		getCheckPointDock.setSoTimeout(100);
	}

	public void serve() {

		try {
			checkPointing();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (WrongMessageTypeException e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}

	}

	public void closeConnections() {
		if (getCheckPointDock != null) {
			getCheckPointDock.close();
		}
	}

	/**
	 * used by RunMain
	 * 
	 * @param msg
	 * @return
	 */
	@Override
	public boolean dealWithMemberShipMsg(Message msg) {
		if (msg.getType() != MessageTypes.MEMBERSHIP) {
			System.out.printf("Receive wrong type from membership dock: %d\n",
					msg.getType());
			return false;
		}
		String content = msg.getContent();

		JSONObject json = new JSONObject(content);

		String type = json.getString("type");

		if (type.equals(MemberShipConstructor.NEWSECONDARY)) {
			String id = json.getString("id");
			String ip = json.getString("ip");
			try {
				backUps.addSecondary(id, ip, portOfSecondaryCheckPoint);
				CurrentTime.tprintln(String.format("SCALE: Register new secondary [id: %s, ip: %s]\n",
						id, ip));
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return false;
			}
		} else if (type.equals(MemberShipConstructor.SECONDARYDEAD)) {
			String id = json.getString("id");
			backUps.delSecondary(id);
			CurrentTime.tprintln(String.format("DETECTED: Secondary %s dead\n", id));
		} else {
			System.out.println("Un-acceptable membership message");
			System.out.println(msg);
			return false;
		}
		return true;
	}

	private boolean checkPointing() throws IOException,
			WrongMessageTypeException {
		Message check = checkPointService.recvAckMessage(getCheckPointDock);

		if (check == null)
			return true;

		//System.out.println(check);

		if (check.getType() != MessageTypes.CHECKPOINT) {
			throw new WrongMessageTypeException(check.getType(),
					MessageTypes.CHECKPOINT);
		}

		String content = check.getContent();

		JSONObject json = new JSONObject(content);
		String type = json.getString("checktype");

		if (type == null) {
			System.out.println("Err: Bad CheckPoint Message");
			return false;
		}
		if (type.equals(CheckPointConstructor.ADD_JOB)) {
			JSONObject jobjson = json.getJSONObject("jobDetail");
			String id = json.getString("sid");

			Job newJob = new Job(jobjson);
			slaveOffice.checkAddNewJob(id, newJob);
			
			CurrentTime.tprintln(String.format("CHECHPOINT add new job [%s]\n", newJob.toString()));

			/* This newly delegated job could be a rerouted one
				We need to remove it from unfinished list
			 */
			delFromUnfinishedQueue(id);

		}else if (type.equals(CheckPointConstructor.ADD_UNF_JOB)) {
			JSONObject jobjson = json.getJSONObject("jobDetail");

			Job newJob = new Job(jobjson);
			unfinishedJobSet.add(newJob);
			
			CurrentTime.tprintln(String.format("CHECHPOINT add unfinished job [%s]\n", newJob.toString()));


		} else if (type.equals(CheckPointConstructor.ADD_SLAVE)) {
			String sid = json.getString("sid");
			String ip = json.getString("ip");
			if (sid == null || ip == null) {
				System.out
						.printf("Err: CheckPoint add slave, but missing critical information[id: %s, ip: %s]\n",
								sid, ip);
				return false;
			}
			CurrentTime.tprintln(String.format("CHECKPOINT: Get new slave [id: %s, ip:%s]\n", sid, ip));
			slaveOffice.addSlave(sid, ip);

		} else if (type.equals(CheckPointConstructor.SET_JOB_STATUS)) {
			String sid = json.getString("sid");
			String jid = json.getString("jobid");
			String status = json.getString("status");

			slaveOffice.setJobStatus(sid, jid, status);
		} else if(type.equals(CheckPointConstructor.DEAD_SLAVE)){
			String sid = json.getString("sid");
			slaveOffice.delSlave(sid, unfinishedJobSet);
			CurrentTime.tprintln(String.format("CHECKPOINT: Slave id: %s  dead\n", sid));

		} else if(type.equals(CheckPointConstructor.SNAPSHOT)){
			JSONArray secondaries = json.getJSONArray("secondaries");
			JSONArray slaves = json.getJSONArray("slaves");
			JSONArray unfinished = json.getJSONArray("unfinished");

			backUps.construct(secondaries);
			slaveOffice.construct(slaves);
			backUps.delSecondary(id);

			construct(unfinished);

			CurrentTime.tprintln("==============SNAPSHOT=============");
			System.out.println(this);
		} else{
			System.out.println("Err: Unknown CheckPoint Type");
			return false;
		}
		return true;
	}
	private void construct(JSONArray unfarray){
		unfinishedJobSet.clear();
		for(int i=0; i<unfarray.length(); i++){
			JSONObject jobjson = unfarray.getJSONObject(i);
			Job unf = new Job(jobjson);
			unfinishedJobSet.add(unf);
		}
	}
	private void delFromUnfinishedQueue(String id){
		Iterator<Job> it = unfinishedJobSet.iterator();
		while(it.hasNext()){
			Job thisOne = it.next();
			if(thisOne.getJobId().equals(id)){
				it.remove();
			}
		}
	}
}
