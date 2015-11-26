package secondary;

import message.Message;
import message.MessageTypes;
import message.msgconstructor.MemberShipConstructor;
import org.json.JSONObject;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import shared.AllSecondaries;
import shared.AllSlaves;
import shared.ConnMetrics;
import shared.Distributer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Created by xingchij on 11/19/15.
 */
public class Secondary extends Distributer {
    private DatagramSocket getCheckPointDock;

    NetServiceProxy checkPointService = NetServiceFactory.getCheckPointService();

    public Secondary() throws SocketException {
        id = UUID.randomUUID().toString();

        slaveOffice = AllSlaves.getOffice();
        backUps = AllSecondaries.getInstance();

        getCheckPointDock = new DatagramSocket(ConnMetrics.portOfSecondaryCheckPoint);

        getCheckPointDock.setSoTimeout(100);
    }

    public void serve() {

        try{
            Message check = checkPointService.recvAckMessage(getCheckPointDock);
            if(check != null) {
                System.out.println(check);
            }
        }catch (IOException e){
            e.printStackTrace();
        }

    }


    public void closeConnections() {
        if(getCheckPointDock!=null) {
            getCheckPointDock.close();
        }
    }

    /**
     *  used by RunMain
     *
     * @param msg
     * @return
     */
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
