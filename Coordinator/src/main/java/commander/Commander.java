package commander;

import message.Message;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import shared.AllSecondaries;
import shared.AllSlaves;
import shared.Distributer;;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;


/**
 * Created by xingchij on 11/18/15.
 */
public class Commander extends Distributer {

    DatagramSocket reportDock = null;
    DatagramSocket getJobDock = null;
    DatagramSocket terminateDock = null;

    public Commander() throws  SocketException{
        slaveOffice = AllSlaves.getOffice();
        backUps = AllSecondaries.getInstance();

        reportDock = new DatagramSocket(portReceiveReport);
        getJobDock = new DatagramSocket(portReceiveJobs);
        terminateDock = new DatagramSocket(portReceiveTerminate);
        terminateDock.setSoTimeout(30);
        reportDock.setSoTimeout(500);
        getJobDock.setSoTimeout(500);
    }
    public void serve() {

        NetServiceProxy toSlaves = (NetServiceProxy) NetServiceFactory.getCommandService();

        while(true) {
            /**
             * send heart beat to Membership Coordinator
             */


            /**
             * receive test tasks from web client
             * and delegate task to slaves
             */
            try {
                Message newTestMsg = toSlaves.recvAckMessage(getJobDock);
                Job newJob = Job.getJobFromDelegateMsg(newTestMsg);
                if (newJob != null) {
                    slaveOffice.pushOneJob(newJob, toSlaves);
                } else {
                    System.out.println("Receive job from client, but not a Delegate message");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    public void closeConnections() {
        if(reportDock!=null) {
            reportDock.close();
        }
        if(getJobDock!=null) {
            getJobDock.close();
        }
    }

}
