package distributor.commander;

import distributor.Distributer;
import error.WrongMessageTypeException;
import message.Message;
import message.MessageTypes;
import message.msgconstructor.CheckPointConstructor;
import message.msgconstructor.HeartBeatConstructor;
import message.msgconstructor.MemberShipConstructor;
import org.json.JSONObject;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import shared.AllSecondaries;
import shared.AllSlaves;
import shared.CurrentTime;
import shared.Job;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by xingchij on 11/18/15.
 */
public class Commander extends Distributer {

	DatagramSocket reportDock = null;
	DatagramSocket getJobDock = null;
	DatagramSocket getHeartBeatDock = null;
	NetServiceProxy toSlaves = NetServiceFactory.getCommandService();
	NetServiceProxy toSendaries = NetServiceFactory.getCheckPointService();
	NetServiceProxy reportService = NetServiceFactory.getRawUDPService();
	NetServiceProxy heartBeatService = NetServiceFactory.getHeartBeatService();
	NetServiceProxy sendBacktoCoordinatorService = NetServiceFactory.getCommandService();

	public static final int RECV_JOB_TIMEOUT = 500;
	public static final int RECV_REPORT_TIMEOUT = 500;
	public static final int RECV_HEARTBEAT_TIMEOUT = 50;


	public Commander(String id) throws SocketException, UnknownHostException {

		ip = NetConfig.getMyIp();
		this.id = id;
		coordinator = new NetConfig(IPOfCoordinator, portOfCoordinatorHeartBeat);
		unfinishedJobSet = new HashSet<Job>();

		slaveOffice = AllSlaves.getOffice();
		slaveOffice.refreshAll();
		backUps = AllSecondaries.getInstance();
		// test only
		// try {
		// //backUps.addSecondary("127.0.0.1",
		// ConnMetrics.portOfSecondaryCheckPoint);
		// } catch (UnknownHostException e) {
		// e.printStackTrace();
		// }

		reportDock = new DatagramSocket(portReceiveReport);
		getJobDock = new DatagramSocket(portReceiveJobs);
		getHeartBeatDock = new DatagramSocket(portReceiveHeartBeatFromSlave);

		reportDock.setSoTimeout(RECV_REPORT_TIMEOUT);
		getJobDock.setSoTimeout(RECV_JOB_TIMEOUT);
		getHeartBeatDock.setSoTimeout(RECV_HEARTBEAT_TIMEOUT);
	}

	public void serve() {

		/**
		 * receive test tasks from coordinator and delegate task to slaves
		 */
		try {
			Message newTestMsg = toSlaves.recvAckMessage(getJobDock);
			Job newJob = Job.getJobFromDelegateMsg(newTestMsg);
			// Job newJob = new Job("scroll", "www.baidu.com", 0, "jin");
			if (newJob != null) {
				String slave = slaveOffice.pushOneJob(newJob, toSlaves);
				//if(slave != null) {
					Message checkAddJob = CheckPointConstructor
							.constructAddJobMessage(newJob, slave);
					sendCheckPoint(checkAddJob);
//				}else{
//					CurrentTime.tprintln(String.format(
//							"DETECTED: Delegate JOB[id: %s] to Slave fail",
//							newJob.getJobId()));
//					unfinishedQueue.add(newJob);
//				}
				if(slave == null){
					unfinishedJobSet.add(newJob);
					
				}else{
					CurrentTime.tprintln(String.format("Delivered one job [%s]", newJob.toString()));
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (WrongMessageTypeException e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}

		/**
		 * receive reports from slaves and check point to secondaries
		 */

		try {
			workOnReport();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (WrongMessageTypeException e) {
			System.out.println(e);
			e.printStackTrace();
		}

		/**
		 * receive heart beat from slaves
		 */
		try {
			String id = slaveOffice.watchForHeartBeat(heartBeatService, getHeartBeatDock);
			String sip = slaveOffice.getSlaveIp(id);
			if(sip != null) {

				Message addSlaveCheckPointMsg = CheckPointConstructor.constructAddSlaveMessage(sip, id);
				sendCheckPoint(addSlaveCheckPointMsg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (WrongMessageTypeException e) {
			e.printStackTrace();
		}

		/**
		 * check for dead slaves
		 */
		List<String> deadSlaves = slaveOffice.checkDead(unfinishedJobSet);
		for(String id: deadSlaves){
			Message checkDead = CheckPointConstructor.constructDelSlaveMessage(id);
			sendCheckPoint(checkDead);
		}

		/**
		 * Reroute unfinished Jobs
		 */
		int count = 0;
		Iterator<Job> unf = unfinishedJobSet.iterator();
		while(unf.hasNext() && count<10){
			Job j = unf.next();

			Message delegate = j.generateMessage();
			try {
				boolean success = sendBacktoCoordinatorService.sendMessage
                        (delegate, new DatagramSocket(), new NetConfig( IPOfCoordinator, portOfCoordinatorRecvJobs));
				if(success) {
					unf.remove();
					CurrentTime.tprintln(String.format("RECOVERING: Rerouted JOB[id: %s]", j.getJobId()));
					
					Message checkAddUnf = CheckPointConstructor.constructAddUnfJobMessage(j);
					sendCheckPoint(checkAddUnf);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			count ++;
		}
	}

	public void closeConnections() {
		if (reportDock != null) {
			reportDock.close();
		}
		if (getJobDock != null) {
			getJobDock.close();
		}
	}

	private boolean sendCheckPoint(Message check) {

		if (check == null) {
			System.out.println("Sending null checkpoint");
			return false;
		}

		NetConfig brdCastAddr = backUps.generateBrdCastNetConfig();
		try {
			toSendaries.sendMessage(check, new DatagramSocket(), brdCastAddr);
		} catch (SocketException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Get report from slave. UNFINISHED: I need id of this slave
	 * 
	 * @return
	 * @throws IOException
	 */
	public boolean workOnReport() throws IOException, WrongMessageTypeException {
		Message report = reportService.recvAckMessage(reportDock);

		if (report == null)
			return true;

		if (report.getType() != MessageTypes.REPORT) {
			System.out.println(report);
			throw new WrongMessageTypeException(report.getType(),
					MessageTypes.REPORT);
		} else {
			String content = report.getContent();
			JSONObject json = new JSONObject(content);
			String slaveId = json.getString("sid");
			String jid = json.getString("jid");
			String status = json.getString("status");
			
			CurrentTime.tprintln(String.format("Set JOb[%s] Status[%s]", jid, status));

			slaveOffice.setJobStatus(slaveId, jid, status);
			// send checkpoint
			Message checkReport = CheckPointConstructor.constructSetJobStatusMessage(slaveId, jid, status);
			sendCheckPoint(checkReport);
			//System.out.println(report);
		}

		return true;
	}

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
				/*
				 * primary need to find a way to transfer whole state to this
				 * new secondary figuring out ...
				 */
				Message snapShot = CheckPointConstructor.constructSnapShotMessage(this);
				sendCheckPoint(snapShot);
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
	
	public void sendHeartBeat() throws IOException {
		if (coordinator == null) {
			System.out
					.println("Err: Coordinator network connection not initialized!");
			return;
		}
		String working = "true";
		if(slaveOffice.getNum() == 0 )
			working = "false";
		heartBeatService.sendMessage(
				HeartBeatConstructor.constructHeartBeatToCoordinator(id, ip, working),
				new DatagramSocket(), coordinator);
	}
}
