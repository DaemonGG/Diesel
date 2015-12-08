package healthchecker;

import error.WrongMessageTypeException;
import message.Message;
import message.MessageTypes;
import message.msgconstructor.MemberShipConstructor;
import message.msgconstructor.WhoIsPrimaryConstructor;
import org.json.JSONObject;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import shared.ConnMetrics;
import shared.CurrentTime;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by xingchij on 11/20/15.
 */
public class WatcherGroup implements ConnMetrics {
	class Watcher {
		NetConfig conn;
		int identity;
		int health_state;
		String representedId;
		long lastUpdate;

		Watcher(String id, String ip, int port, int identity)
				throws UnknownHostException {
			conn = new NetConfig(ip, port);
			representedId = id;
			lastUpdate = System.currentTimeMillis();
			this.identity = identity;
		}

		String getRepresentedId() {
			return representedId;
		}

		long getLastUpdate() {
			return lastUpdate;
		}

		boolean isTimeout() {
			return (System.currentTimeMillis() - lastUpdate) > TIMEOUT;
		}

		void changeIdentity(int identity) {
			this.identity = identity;
		}

		int whatIRepresent() {
			return identity;
		}

		NetConfig getConn() {
			return conn;
		}
	}

	public static final int ID_PRIMARY = 0;
	public static final int ID_SECONDARY = 1;
	public static final int ID_UNKNOWN = -1;

	public static final int HEALTH_HEALTHY = 2;
	public static final int HEALTH_DEAD = 3;
	public static final long TIMEOUT = 15000;

	public static final int RECV_HEARTBEAT_TIMEOUT = 5000;
	public static final int RECV_WHOISPRIMARY_TIMEOUT = 20;

	public int watcherNum = 0;
	int nextPrimaryIndex = 0;
	Watcher primary = null;

	HashMap<String, Watcher> group = new HashMap<String, Watcher>();
	HashMap<String, Watcher> backUpGroup = new HashMap<String, Watcher>();
	List<String> secondaryIdList = new ArrayList<String>();

	DatagramSocket heartBeatDock;
	DatagramSocket whoIsPrimaryDock;
	NetServiceProxy tellAboutPrimaryService;
	NetServiceProxy heartBeatService;
	NetServiceProxy membershipService;

	public void closeConnections() {
		whoIsPrimaryDock.close();
		heartBeatDock.close();
	}

	public void serve() {

	}

	public WatcherGroup() throws SocketException {
		heartBeatDock = new DatagramSocket(portOfCoordinatorHeartBeat);
		heartBeatDock.setSoTimeout(RECV_HEARTBEAT_TIMEOUT);

		whoIsPrimaryDock = new DatagramSocket(portOfCoordinatorForPrimaryAddr);
		whoIsPrimaryDock.setSoTimeout(RECV_WHOISPRIMARY_TIMEOUT);

		heartBeatService = NetServiceFactory.getHeartBeatService();
		membershipService = NetServiceFactory.getMembershipService();
		tellAboutPrimaryService = NetServiceFactory.getRawUDPService();
	}

	public NetConfig getPrimary() {
		if (primary == null)
			return null;
		return primary.getConn();
	}

	private void addBackUp(String id, String ip, int port)
			throws UnknownHostException {
		Watcher b = new Watcher(id, ip, port, ID_SECONDARY);

		if (primary != null && announceNewSecondary(b) == false) {
			System.out.println("Announce to add new secondary totally fail");
			return;
		}


		group.put(b.getRepresentedId(), b);
		backUpGroup.put(b.getRepresentedId(), b);
		secondaryIdList.add(id);
		watcherNum++;
	}

	public void delWatcher(String id){
		if(!group.containsKey(id)){
			System.out.printf("Try to remove watcher: %s, but not exist\n", id);
			return;
		}
		watcherNum --;
		if(primary != null && id.equals(primary.getRepresentedId())){
			group.remove(primary.getRepresentedId());
			primary = null;
		}else{
			if (backUpGroup.containsKey(id)) {
				backUpGroup.remove(id);
				secondaryIdList.remove(id);
			}
			group.remove(id);
		}
	}
	private boolean announceNewSecondary(Watcher newOne) {
		Message newSecondaryMsg = MemberShipConstructor
				.newSecondaryMemberMsgConstructor(newOne.getRepresentedId(),
						newOne.getConn());

		try {

			boolean _success = membershipService.sendMessage(newSecondaryMsg,
					new DatagramSocket(), primary.getConn());
			if (_success == false)
				return false; // if announce to primary fails, the function
								// return as false

			/**
			 * tell all backups that there's a new guy I will return true, if
			 * even only one success.
			 */
			for (Watcher w : backUpGroup.values()) {

				try {
					membershipService.sendMessage(newSecondaryMsg,
							new DatagramSocket(), w.getConn());

				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		} catch (IOException e) {
			e.printStackTrace();

		}

		return true; // otherwise return true
	}

	/**
	 * When detect primary dead, it will choose another secondary to be primary
	 * and choose the next secondary be a possible primary
	 * 
	 * @return
	 */
	private boolean changePrimary() {
		if (secondaryIdList.size() == 0) {
			System.out.println("No backup remaining...system crash..");
			return false;
		}
		try {
			Message assignPrimary = MemberShipConstructor
					.youArePrimaryMsgConstructor();

			boolean success = false;
			int loopCount = 0;
			Watcher nextPrimary = null;

			while(success == false && loopCount < secondaryIdList.size()) {
				String nextId = secondaryIdList.get(nextPrimaryIndex);
				nextPrimaryIndex++;
				if(nextPrimaryIndex >= secondaryIdList.size()){
					nextPrimaryIndex = 0;
				}

				nextPrimary = backUpGroup.get(nextId);

				CurrentTime.tprintln(String.format("Sending YOUAREPRIMARY to [id: %s, ip: %s]\n",
						nextPrimary.getRepresentedId(), nextPrimary.getConn()
								.getIP()));
				success = membershipService.sendMessage(assignPrimary,
						new DatagramSocket(), nextPrimary.getConn());

				if (success == false) {
//					System.out.printf("Assign new Primary to [id: %s, ip: %s] fail\n",
//							nextPrimary.getRepresentedId(), nextPrimary.getConn().getIP());
					nextPrimary = null;
				}else{
					CurrentTime.tprintln(String.format("RECOVERY: Assigned new Primary to [id: %s, ip: %s]",
							nextPrimary.getRepresentedId(), nextPrimary.getConn().getIP()));
				}
				loopCount ++;
			}

			// the old primary is dead, remove it from list
			if (primary != null
					&& group.containsKey(primary.getRepresentedId())) {
				delWatcher(primary.getRepresentedId());  // primary will be null
			}
			if(nextPrimary == null) {
				System.out.println("No secondary can take the place!");
				return false;
			}
			primary = nextPrimary;
			primary.changeIdentity(ID_PRIMARY);
			backUpGroup.remove(nextPrimary.getRepresentedId());
			secondaryIdList.remove(nextPrimary.getRepresentedId());

			if (backUpGroup.isEmpty()) {
				System.out.println("NO Secondary remaining...");
			}
			if(nextPrimaryIndex >= secondaryIdList.size()){
				nextPrimaryIndex = 0;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

//	private void setNextPrimary() {
//		Watcher currentNext = nextPrimary;
//		nextPrimary = null;
//		for (Watcher w : backUpGroup.values()) {
//			if( currentNext == null ||
//					!currentNext.getRepresentedId().equals(w.getRepresentedId())){
//				nextPrimary = w;
//				break;
//			}
//		}
//	}

	/**
	 * Tell all primary, secondaries, that some secondary is now dead
	 * 
	 * @param deadOne
	 * @return This function will return false only when announce to primary
	 *         fail.
	 */
	private boolean announceSecondaryDead(Watcher deadOne) {
		NetConfig deadSecondary = deadOne.getConn();
		Message announceDeadSecondary = MemberShipConstructor
				.scondaryDeadMsgConstructor(deadOne.getRepresentedId());
		try {
			boolean _success = membershipService.sendMessage(
					announceDeadSecondary, new DatagramSocket(),
					primary.getConn());
			if (_success == false)
				return false; // if announce to primary fails, the function
								// return as false

			/**
			 * tell all backups that there's a new guy I will return true, if
			 * even only one success.
			 */
			for (Watcher w : backUpGroup.values()) {

				try {
					membershipService.sendMessage(announceDeadSecondary,
							new DatagramSocket(), w.getConn());

				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			// membershipService.sendMessage(announceDeadSecondary, new
			// DatagramSocket(), primary.getConn());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Listen for heart beat message, register new secondary when new member
	 * send heart beat. NOTE: Secondary choose his own id, and tell Coordinator
	 * his id.
	 * 
	 * @return The ID number of whom that sent the heart beat message
	 * @throws IOException
	 */
	public int watchForHeartBeat() throws IOException,
			WrongMessageTypeException {
		Message hbt = heartBeatService.receiveMessage(heartBeatDock);
		if (hbt == null)
			return ID_UNKNOWN;

		int who = -1;
		if (hbt.getType() == MessageTypes.HEARTBEAT) {

			String content = hbt.getContent();
			JSONObject json = new JSONObject(content);
			String distributorId = json.getString("id");

			Watcher theOne = group.get(distributorId);

			/**
			 * if I can not find the distributor, I will register this new guy
			 * add him as a backup. if right now, no primary, which means it's
			 * now a very starting point I will automatically change this backup
			 * to be primary. and send message telling him about my decision
			 */
			if (theOne == null) {
				String ip = json.getString("ip");
				CurrentTime.tprintln(String.format(
						"Find new distributor[id: %s, ip: %s], register it\n",
						distributorId, ip));
				addBackUp(distributorId, ip, portForMemberShipConfig);

				if (primary == null) {
					System.out
							.println("This new distributor will be primary...");
					if (changePrimary()) {
						who = ID_PRIMARY;
					}
				} else {
					who = ID_SECONDARY;
				}
			} else {
//				System.out.printf("Get HeartBeat from[id: %s, ip: %s]\n",
//						theOne.getRepresentedId(), theOne.getConn().getIP());

				if (theOne.identity == ID_PRIMARY) {
					who = ID_PRIMARY;
				} else if (theOne.identity == ID_SECONDARY) {
					who = ID_SECONDARY;
				} else {
					who = ID_UNKNOWN;
				}
				theOne.lastUpdate = System.currentTimeMillis();
				theOne.health_state = HEALTH_HEALTHY;
			}

			return who;

		} else {
			throw new WrongMessageTypeException(hbt.getType(),
					MessageTypes.HEARTBEAT);
		}

	}

	public void checkDead() {
		Collection<Watcher> allMonitors = new ArrayList<Watcher>();
		for (Watcher monitor : group.values()) {
			allMonitors.add(monitor);
		}

		for (Watcher monitor : allMonitors) {
			if (monitor.isTimeout()) {
				monitor.health_state = HEALTH_DEAD;

				if (monitor.whatIRepresent() == ID_PRIMARY) {
					watcherNum --;
					CurrentTime.tprintln(String.format(
							"DETECTED: Find primary [id: %s, ip: %s] dead, choose a new one..",
							monitor.getRepresentedId(), monitor.getConn().getIP()));
					if (changePrimary() == false) {
						CurrentTime.tprintln("CRASH: No Primary alive, system crashed...");
						primary = null;
						System.exit(1);
					} else {
						CurrentTime.tprintln("RECOVERY: Selected a new PRIMARY");
					}

				} else if (monitor.whatIRepresent() == ID_SECONDARY) {

					CurrentTime.tprintln(String.format(
							"DETECTED: Found secondary [id: %s, ip: %s] dead\n", monitor
									.getRepresentedId(), monitor.getConn()
									.getIP()));
					String id = monitor.getRepresentedId();

					// if the dead secondary is the next primary, select another
					// "nextPrimary"
					delWatcher(id);

					if (announceSecondaryDead(monitor) == false) {
						System.out.println("\tUnable to fix");
					} else {
						//System.out.println("\tinformed");
					}

				} else {
					System.out.println("Unknown representative");
				}

			} else {

			}
		}
	}

	public void watchForWhoIsPrimary() throws IOException,
			WrongMessageTypeException {
		Message query = tellAboutPrimaryService
				.receiveMessage(whoIsPrimaryDock);

		if (query == null)
			return;

		if (query.getType() != MessageTypes.WHOISPRIMARY) {
			throw new WrongMessageTypeException(query.getType(),
					MessageTypes.WHOISPRIMARY);
		}

		String content = query.getContent();
		JSONObject json = new JSONObject(content);
		String ip = json.getString("sip");
		int port = json.getInt("port");

		if (primary == null) {
			System.out
					.println("Err: Get query for primary, but primary is None");
			return;
		}
		Message answer = WhoIsPrimaryConstructor.constructAnswer(primary
				.getConn().getIP());
		//System.out.printf("Tell [ip:%s, port:%d] primary address\n",  ip, port);
		tellAboutPrimaryService.sendMessage(answer, whoIsPrimaryDock,
				new NetConfig(ip, port));
	}
}
