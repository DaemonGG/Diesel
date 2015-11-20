package shared;

import commander.Commander;
import message.Message;
import secondary.Secondary;

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
    private DatagramSocket getMemberShipChangeDock;

    public Distributer player = null;


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

    public void switchIdentiry(int identity) throws SocketException {
        if(identity == ID_PRIMARY && whoIAm() != ID_PRIMARY){
            if(player != null){
                player.closeConnections();
            }
            player = new Commander();
        }else if(identity == ID_SECONDARY && whoIAm() != ID_SECONDARY){
            if(player != null){
                player.closeConnections();
            }
            player = new Secondary();
        }else{
            System.out.println("Illegal change of identity");
        }
    }

    public int whoIAm(){
        return identity;
    }

    public void closeConnections(){
        if(getMemberShipChangeDock != null) {
            getMemberShipChangeDock.close();
        }
        if(getMemberShipChangeDock != null){
            getMemberShipChangeDock.close();
        }
    }

    public static void main(String[] args) throws SocketException {

        RunMain machine = new RunMain(ID_PRIMARY);

        while(true){

            machine.player.serve();

        }

    }
}
