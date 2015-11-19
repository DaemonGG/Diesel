package secondary;

import javafx.scene.chart.PieChart;
import message.Message;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import shared.AllSecondaries;
import shared.AllSlaves;
import shared.ConnMetrics;
import shared.Distributer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by xingchij on 11/19/15.
 */
public class Secondary extends Distributer {
    private DatagramSocket getCheckPointDock;
    private DatagramSocket getMemberShipChangeDock;
    private DatagramSocket terminateDock = null;

    public Secondary() throws SocketException {
        slaveOffice = AllSlaves.getOffice();
        backUps = AllSecondaries.getInstance();

        terminateDock = new DatagramSocket(ConnMetrics.portReceiveTerminate);
        getCheckPointDock = new DatagramSocket(ConnMetrics.portOfSecondaryCheckPoint);
        getMemberShipChangeDock = new DatagramSocket(ConnMetrics.portForMemberShipConfig);

        terminateDock.setSoTimeout(30);
        getCheckPointDock.setSoTimeout(100);
        getMemberShipChangeDock.setSoTimeout(50);
    }
    @Override
    public void serve() {
        NetServiceProxy checkPointServive = NetServiceFactory.getCheckPointService();

        while(true){

            try{
                Message check = checkPointServive.recvAckMessage(getCheckPointDock);
                System.out.println(check);
            }catch (IOException e){
                e.printStackTrace();
            }

        }
    }

    public void closeConnections() {
        if(getCheckPointDock!=null) {
            getCheckPointDock.close();
        }
        if(getMemberShipChangeDock!=null) {
            getMemberShipChangeDock.close();
        }
    }
}
