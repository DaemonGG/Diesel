package distributor;

import distributor.commander.Commander;
import distributor.secondary.Secondary;
import error.WrongMessageTypeException;
import message.Message;
import message.MessageTypes;
import message.msgconstructor.MemberShipConstructor;
import org.json.JSONObject;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import shared.ConnMetrics;
import shared.CurrentTime;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Created by xingchij on 11/19/15.
 */
public class RunMain {
	public static final int ID_PRIMARY = 0;
	public static final int ID_SECONDARY = 1;

	public static final int RECV_MEMBERSHIP_TIMEOUT = 50;
	public static final int RECV_TERMINATE_TIMEOUT = 30;

	int identity;
	String id;
	private DatagramSocket terminateDock = null;
	private DatagramSocket memberShipChangeDock = null;

	public Distributer player = null;

	private NetServiceProxy membershipService = NetServiceFactory
			.getMembershipService();

	public RunMain(int identity) throws SocketException, UnknownHostException {
		id = UUID.randomUUID().toString();

		terminateDock = new DatagramSocket(ConnMetrics.portReceiveTerminate);
		memberShipChangeDock = new DatagramSocket(
				ConnMetrics.portForMemberShipConfig);

		memberShipChangeDock.setSoTimeout(RECV_MEMBERSHIP_TIMEOUT);
		terminateDock.setSoTimeout(RECV_TERMINATE_TIMEOUT);

		this.identity = identity;
		if (identity == ID_PRIMARY) {
			this.player = new Commander(id);
		} else if (identity == ID_SECONDARY) {
			this.player = new Secondary(id);
		} else {
			System.out.println("Who I am ?");
		}
	}

	public boolean listenmemberShipChange() throws IOException,
			WrongMessageTypeException {
		Message memberShipMsg = membershipService
				.recvAckMessage(memberShipChangeDock);

		if (memberShipMsg == null)
			return true;

		if (memberShipMsg.getType() != MessageTypes.MEMBERSHIP) {
			System.out.printf("Receive wrong type from membership dock: %d\n",
					memberShipMsg.getType());
			throw new WrongMessageTypeException(memberShipMsg.getType(),
					MessageTypes.MEMBERSHIP);
		}

		String content = memberShipMsg.getContent();
		String type = new JSONObject(content).getString("type");
		/*
		 * If this is a "YOUAREPRIMARY" message, and if current identity is
		 * secondary Then identity type change
		 */
		if (type.equals(MemberShipConstructor.YOUAREPRIMARY)) {
			System.out.printf("GET YOUAREPRIMARY, I AM %d\n", identity);
			if (identity == ID_SECONDARY) {
				switchIdentiry(ID_PRIMARY);

				CurrentTime.tprintln(String.format("Secondary[id: %s] becoming PRIMARY", id));
				System.out.println("==============SNAPSHOT=============");
				System.out.println(player);
			}
			return true;
		} else {
			return player.dealWithMemberShipMsg(memberShipMsg);
		}

	}

	private void switchIdentiry(int identity) throws SocketException,
			UnknownHostException {
		if (identity == ID_PRIMARY && whoIAm() != ID_PRIMARY) {
			if (player != null) {
				player.closeConnections();
			}
			player = new Commander(id);
			this.identity = identity;
		} else if (identity == ID_SECONDARY && whoIAm() != ID_SECONDARY) {
			if (player != null) {
				player.closeConnections();
			}
			player = new Secondary(id);
			this.identity = identity;
		} else {
			System.out.println("Illegal change of identity");
		}

	}

	public int whoIAm() {
		return identity;
	}

	public void closeConnections() {
		if (memberShipChangeDock != null) {
			memberShipChangeDock.close();
		}
		if (terminateDock != null) {
			terminateDock.close();
		}
	}

	public void run() {
		int loop = 0;
		while (true) {
			try {

				player.sendHeartBeat();

				if(loop == 5) {
					//CurrentTime.tprintln("HeartBeating");
					loop = 0;
				}

				listenmemberShipChange();
				player.serve();

			} catch(InterruptedIOException e) {
				closeConnections();
				CurrentTime.tprintln("I am dead");
				System.out
						.println("Distributor terminated. All resources released");
				return;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (WrongMessageTypeException e) {
				System.out.println(e);
				e.printStackTrace();
			}
			loop ++;
		}
	}

	public static void main(String[] args) throws SocketException {
		RunMain machine = null;
		try {
			machine = new RunMain(ID_SECONDARY);
			System.out
					.printf("Distributor[id: %s, ip: %s] running, I am now secondary\n",
							machine.player.id, machine.player.ip);
			machine.run();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	
	}
}
