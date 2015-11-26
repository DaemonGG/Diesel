package shared;

import commander.Commander;
import message.Message;
import message.MessageTypes;
import message.msgconstructor.MemberShipConstructor;
import org.json.JSONObject;
import secondary.Secondary;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by xingchij on 11/19/15.
 */
public class RunMain {
    public static final int ID_PRIMARY = 0;
    public static final int ID_SECONDARY = 1;

    int identity;
    private DatagramSocket terminateDock = null;
    private DatagramSocket memberShipChangeDock = null;

    public Distributer player = null;

    private NetServiceProxy membershipService = NetServiceFactory.getMembershipService();


    public RunMain(int identity) throws SocketException {
        //terminateDock = new DatagramSocket(ConnMetrics.portReceiveTerminate);
        //getMemberShipChangeDock = new DatagramSocket(ConnMetrics.portForMemberShipConfig);

        //getMemberShipChangeDock.setSoTimeout(50);
        //terminateDock.setSoTimeout(30);
        if(identity == ID_PRIMARY){
            this.player = new Commander();
        }else if(identity == ID_SECONDARY){
            this.player = new Secondary();
        }else{
            System.out.println("Who I am ?");
        }
    }

    public boolean listenmemberShipChange() throws IOException {
        Message memberShipMsg = membershipService.recvAckMessage(memberShipChangeDock);

        if(memberShipMsg.getType() != MessageTypes.MEMBERSHIP){
            System.out.printf("Receive wrong type from membership dock: %d\n",
                    memberShipMsg.getType());
            return false;
        }

        String content = memberShipMsg.getContent();
        String type = new JSONObject(content).getString("type");
        /*
            If this is a "YOUAREPRIMARY" message, and if current identity is secondary
            Then identity type change
         */
        if( type.equals(MemberShipConstructor.YOUAREPRIMARY) ){
            if(identity == ID_SECONDARY) {
                switchIdentiry(ID_PRIMARY);
            }
            return true;
        }else{
            return player.dealWithMemberShipMsg(memberShipMsg);
        }

    }
    private void switchIdentiry(int identity) throws SocketException {
        if(identity == ID_PRIMARY && whoIAm() != ID_PRIMARY){
            if(player != null){
                player.closeConnections();
            }
            player = new Commander();
            this.identity = identity;
        }else if(identity == ID_SECONDARY && whoIAm() != ID_SECONDARY){
            if(player != null){
                player.closeConnections();
            }
            player = new Secondary();
            this.identity = identity;
        }else{
            System.out.println("Illegal change of identity");
        }

    }

    public int whoIAm(){
        return identity;
    }

    public void closeConnections(){
        if(memberShipChangeDock != null) {
            memberShipChangeDock.close();
        }
        if(terminateDock != null){
            terminateDock.close();
        }
    }

    public void run(){
        while(true){
            try{

                player.sendHeartBeat();

                listenmemberShipChange();
                player.serve();

            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws SocketException, InterruptedException {

        RunMain machine = new RunMain(ID_PRIMARY);

        System.out.println(System.currentTimeMillis());
        System.out.println(System.currentTimeMillis()/1000l);

        Thread.sleep(5000);

        System.out.println(System.currentTimeMillis());
        System.out.println(System.currentTimeMillis()/1000l);
    }
}
