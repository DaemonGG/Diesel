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
	public static final int portOfCoordinatorForPrimaryDB = 8010;

	public static final int portOfMongoDB = 27017;
	public static final String[] IPsOfMongoDB = { "128.2.57.41","128.237.191.159","128.237.179.42"};
	public static final String IPOfMongoDB = "108.39.205.74";
	public static final String DB_NAME = "diesel";
	public static final String IPOfCoordinator = "128.237.168.220";

	public void closeConnections();

	public void serve();
}
