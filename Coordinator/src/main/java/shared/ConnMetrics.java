package shared;

import java.net.SocketException;

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
    public static final String IPOfCoordinator = "localhost";

    public void closeConnections();
}
