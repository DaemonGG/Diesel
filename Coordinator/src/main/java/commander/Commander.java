package commander;

import message.Message;
import message.MessageTypes;
import message.msgconstructor.CheckPointConstructor;
import message.msgconstructor.MemberShipConstructor;
import org.json.JSONObject;
import services.checkpoint.CheckPointService;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import services.io.NetService;
import shared.AllSecondaries;
import shared.AllSlaves;
import shared.ConnMetrics;
import shared.Distributer;
import sun.nio.ch.Net;;

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
    NetServiceProxy reportService = NetServiceFactory.getRawUDPService();

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

    public void serve() {

        /**
         * send heart beat to Membership Coordinator
         */


        /**
         * receive test tasks from coordinator
         * and delegate task to slaves
         */
        try {
//            Message newTestMsg = toSlaves.recvAckMessage(getJobDock);
//            Job newJob = Job.getJobFromDelegateMsg(newTestMsg);
            Job newJob = new Job("scroll", "www.baidu.com", 0, "jin");
            if (newJob != null) {
                NetConfig slave = slaveOffice.pushOneJob(newJob, toSlaves);

                Message checkAddJob = CheckPointConstructor.constructAddJobMessage(newJob, slave);
                sendCheckPoint(checkAddJob);
            } else {
                System.out.println("Receive job from client, but not a Delegate message");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
         * receive reports from slaves
         * and check point to secondaries
         */

        try {
            workOnReport();
        } catch (IOException e) {
            e.printStackTrace();
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

    private boolean sendCheckPoint(Message check){

        if(check == null){
            System.out.println("Sending null checkpoint");
            return false;
        }

        NetConfig brdCastAddr = backUps.generateBrdCastNetConfig();
        try {
            toSendaries.sendMessage(check, new DatagramSocket(), brdCastAddr);
        }catch (SocketException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean workOnReport() throws IOException {
        Message report = reportService.recvAckMessage(reportDock);
        if(report.getType() != MessageTypes.REPORT){
            System.out.println("Get wrong type from report dock");
            System.out.println(report);
            return false;
        }
        System.out.println(report);
        return true;
    }

    @Override
    public boolean dealWithMemberShipMsg(Message msg) {
        if(msg.getType() != MessageTypes.MEMBERSHIP){
            System.out.printf("Receive wrong type from membership dock: %d\n", msg.getType());
            return false;
        }
        String content = msg.getContent();

        JSONObject json = new JSONObject(content);

        String type = json.getString("type");

        if(type.equals(MemberShipConstructor.NEWSECONDARY)){
            String id = json.getString("id");
            String ip = json.getString("ip");
            try {
                backUps.addSecondary(id, ip, portOfSecondaryCheckPoint);
                /*
                    primary need to find a way to transfer whole state to this new secondary
                    figuring out ...
                 */


            } catch (UnknownHostException e) {
                e.printStackTrace();
                return false;
            }
        }else if(type.equals(MemberShipConstructor.SECONDARYDEAD)){
            String id = json.getString("id");
            backUps.delSecondary(id);
        }else{
            System.out.println("Un-acceptable membership message");
            System.out.println(msg);
            return false;
        }
        return true;
    }
}
