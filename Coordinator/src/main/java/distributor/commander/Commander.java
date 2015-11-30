package distributor.commander;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import message.Message;
import message.MessageTypes;
import message.msgconstructor.CheckPointConstructor;
import message.msgconstructor.MemberShipConstructor;

import org.json.JSONObject;

import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import shared.AllSecondaries;
import shared.AllSlaves;
import shared.Job;
import distributor.Distributer;
import error.WrongMessageTypeException;

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

	public static final int RECV_JOB_TIMEOUT = 500;
	public static final int RECV_REPORT_TIMEOUT = 500;
	public static final int RECV_HEARTBEAT_TIMEOUT = 50;


	public Commander(String id) throws SocketException, UnknownHostException {

		ip = NetConfig.getMyIp();
		this.id = id;
		coordinator = new NetConfig(IPOfCoordinator, portOfCoordinatorHeartBeat);

		slaveOffice = AllSlaves.getOffice();
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
		 * send heart beat to Membership Coordinator
		 */

		/**
		 * receive test tasks from coordinator and delegate task to slaves
		 */
		try {
			Message newTestMsg = toSlaves.recvAckMessage(getJobDock);
			Job newJob = Job.getJobFromDelegateMsg(newTestMsg);
			// Job newJob = new Job("scroll", "www.baidu.com", 0, "jin");
			if (newJob != null) {
				String slave = slaveOffice.pushOneJob(newJob, toSlaves);

				Message checkAddJob = CheckPointConstructor
						.constructAddJobMessage(newJob, slave);
				sendCheckPoint(checkAddJob);
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
			slaveOffice.watchForHeartBeat(heartBeatService, getHeartBeatDock);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (WrongMessageTypeException e) {
			e.printStackTrace();
		}

		/**
		 * check for dead slaves
		 */
		List<String> deadSlaves = slaveOffice.checkDead();

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
			System.out.println(report);
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

				System.out.printf("Register new secondary [id: %s, ip: %s]\n",
						id, ip);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return false;
			}
		} else if (type.equals(MemberShipConstructor.SECONDARYDEAD)) {
			String id = json.getString("id");
			backUps.delSecondary(id);
			System.out.printf("Secondary %s dead\n", id);
		} else {
			System.out.println("Un-acceptable membership message");
			System.out.println(msg);
			return false;
		}
		return true;
	}
}
