package shared;

/**
 * Created by xingchij on 11/18/15.
 */
public interface ConnMetrics {
	public static final int portOfSlaveDelegateTask = 8000;
	public static final int portReceiveReport = 8001;
	public static final int portReceiveJobs = 8002;
	public static final int portReceiveTerminate = 9999;
	public static final int portReceiveHeartBeatFromSlave = 8003;
	public static final int portOfCoordinatorHeartBeat = 8004;
	public static final int portOfSecondaryCheckPoint = 8005;
	public static final int portForMemberShipConfig = 8006;
	public static final int portOfCoordinatorRecvJobs = 8008;
	public static final int portOfCoordinatorForPrimaryAddr = 8009;
	public static final int portOfMongoDB = 27017;
	public static final String IPOfMongoDB = "128.237.135.135";
	public static final String DB_NAME = "diesel";
	public static final String IPOfCoordinator = "192.168.1.2";

	public void closeConnections();

	public void serve();
}
