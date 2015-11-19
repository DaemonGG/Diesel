package commander;

import message.Message;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;


/**
 * Created by xingchij on 11/18/15.
 */
public class Commander implements ConnMetrics {
    AllSlaves office = AllSlaves.getOffice();
    DatagramSocket reportDock = null;
    DatagramSocket getJobDock = null;
    public void serve() throws SocketException {
        configureConnections();
        NetServiceProxy toSlaves = (NetServiceProxy) NetServiceFactory.getCommandService();
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
            if(newJob != null) {
                office.pushOneJob(newJob, toSlaves);
            }else{
                System.out.println("Receive job from client, but not a Delegate message");
            }

        }catch (IOException e) {
            e.printStackTrace();
        }



    }

    public void configureConnections() throws SocketException {
        reportDock = new DatagramSocket(portReceiveReport);
        getJobDock = new DatagramSocket(portReceiveJobs);
        reportDock.setSoTimeout(500);
        getJobDock.setSoTimeout(500);
    }

    public void closeConnections() {
        reportDock.close();
        getJobDock.close();
    }

}
