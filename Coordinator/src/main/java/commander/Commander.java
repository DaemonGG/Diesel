package commander;

import message.Message;
import message.msgconstructor.CheckPointConstructor;
import services.checkpoint.CheckPointService;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import shared.AllSecondaries;
import shared.AllSlaves;
import shared.ConnMetrics;
import shared.Distributer;;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;


/**
 * Created by xingchij on 11/18/15.
 */
public class Commander extends Distributer {

    DatagramSocket reportDock = null;
    DatagramSocket getJobDock = null;
    NetServiceProxy toSlaves =  NetServiceFactory.getCommandService();
    NetServiceProxy toSendaries = NetServiceFactory.getCheckPointService();

    public Commander() throws  SocketException{

        id = UUID.randomUUID().toString();
        slaveOffice = AllSlaves.getOffice();
        backUps = AllSecondaries.getInstance();
        //test only
//        try {
//            //backUps.addSecondary("127.0.0.1", ConnMetrics.portOfSecondaryCheckPoint);
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }

        reportDock = new DatagramSocket(portReceiveReport);
        getJobDock = new DatagramSocket(portReceiveJobs);

        reportDock.setSoTimeout(500);
        getJobDock.setSoTimeout(500);
    }

    public boolean sendCheckPoint(NetConfig slave, Job newJob, NetConfig brdCastAddr){
        Message checkAddJob = CheckPointConstructor.constructAddJobMessage(newJob, slave);
        try {
            toSendaries.sendMessage(checkAddJob, new DatagramSocket(), brdCastAddr);
        }catch (SocketException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void serve() {

        /**
         * send heart beat to Membership Coordinator
         */


        /**
         * receive test tasks from web client
         * and delegate task to slaves
         */
        try {
//            Message newTestMsg = toSlaves.recvAckMessage(getJobDock);
//            Job newJob = Job.getJobFromDelegateMsg(newTestMsg);
            Job newJob = new Job("scroll", "www.baidu.com", 0, "jin");
            if (newJob != null) {
                //slaveOffice.pushOneJob(newJob, toSlaves);
                sendCheckPoint(
                        new NetConfig("127.0.0.1", ConnMetrics.portOfSlaveDelegateTask),
                        newJob,
                        backUps.generateBrdCastNetConfig());
            } else {
                System.out.println("Receive job from client, but not a Delegate message");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean receiveMemberShipMes() {
        return false;
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
