package healthchecker;

import message.Message;
import message.MessageTypes;
import message.msgconstructor.MemberShipConstructor;
import org.json.JSONObject;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import shared.ConnMetrics;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Created by xingchij on 11/20/15.
 */
public class WatcherGroup implements ConnMetrics{
    class Watcher{
        NetConfig conn;
        int identity;
        int health_state;
        String representedId;
        long lastUpdate;

        Watcher(String id, String ip, int port, int identity) throws UnknownHostException {
            conn = new NetConfig(ip, port);
            representedId = id;
            lastUpdate = System.currentTimeMillis();
            this.identity = identity;
        }
        String getRepresentedId(){
            return representedId;
        }
        long getLastUpdate(){
            return lastUpdate;
        }
        boolean isTimeout(){
            return (System.currentTimeMillis() - lastUpdate) > TIMEOUT;
        }
        void changeIdentity(int identity){
            this.identity = identity;
        }
        int whoIRepresent(){
            return identity;
        }
        NetConfig getConn(){
            return conn;
        }
    }

    public static final int ID_PRIMARY = 0;
    public static final int ID_SECONDARY = 1;
    public static final int HEALTH_HEALTHY = 2;
    public static final int HEALTH_DEAD = 3;
    public static final long TIMEOUT = 120000;
    public int watcherNum = 0;
    Watcher nextPrimary = null;
    Watcher primary = null;

    HashMap<String, Watcher> group = new HashMap<String, Watcher>();
    HashMap<String, Watcher> backUpGroup = new HashMap<String, Watcher>();

    DatagramSocket heartBeatDock;
    NetServiceProxy heartBeatService = NetServiceFactory.getHeartBeatService();
    NetServiceProxy membershipService = NetServiceFactory.getMembershipService();

    public void closeConnections() {
       heartBeatDock.close();
    }

    public WatcherGroup() throws SocketException {
        heartBeatDock = new DatagramSocket(ConnMetrics.portReceiveHeartBeatFromDistributor);
        heartBeatDock.setSoTimeout(5000);
    }

//    public void addPrimary(String id, String ip, int port) throws UnknownHostException {
//        Watcher p = new Watcher( id, ip, port, ID_PRIMARY);
//        primary = p;
//        group.put(p.getRepresentedId(), p);
//    }

    public void addBackUp(String id, String ip, int port) throws UnknownHostException {
        Watcher b = new Watcher(id , ip, port, ID_SECONDARY);


        /*
            set the first participating backup to be the next primary
         */
        if(nextPrimary == null){
            nextPrimary = b;
        }
        if(primary != null && announceNewSecondary(b) == false){
            System.out.println("Tell promary to add new secondary fail");
            return;
        }
        group.put(b.getRepresentedId(), b);
        backUpGroup.put(b.getRepresentedId(), b);
    }

    private boolean announceNewSecondary(Watcher newOne){
        Message newSecondaryMsg = MemberShipConstructor.newSecondaryMemberMsgConstructor
                    (newOne.getRepresentedId(), newOne.getConn());
        boolean _success = true;
            /**
             * tell everyone in the group that there's a new guy
             */
            for(Watcher w : group.values()) {

                try {
                    membershipService.sendMessage(newSecondaryMsg, new DatagramSocket(), w.getConn());
                } catch (IOException e) {
                    e.printStackTrace();
                    _success = false;
                }

            }

        return _success;
    }

    /**
     * When detect primary dead, it will choose another secondary to be primary
     * and choose the next secondary be a possible primary
     * @return
     */
    public boolean changePrimary() {
        if(nextPrimary == null){
            System.out.println("No backup remaining...system crash..");
            return false;
        }
        try {
            Message assignPrimary = MemberShipConstructor.youArePrimaryMsgConstructor();
            membershipService.sendMessage(assignPrimary, new DatagramSocket(), nextPrimary.getConn());

            primary = nextPrimary;
            primary.identity = ID_PRIMARY;
            backUpGroup.remove(nextPrimary.getRepresentedId());

            if (backUpGroup.isEmpty()) {
                nextPrimary = null;
            } else {
                for (Watcher w : backUpGroup.values()) {
                    nextPrimary = w;
                    break;
                }
            }
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean announceSecondaryDead(Watcher deadOne){
        NetConfig deadSecondary = deadOne.getConn();
        Message announceDeadSecondary = MemberShipConstructor.scondaryDeadMsgConstructor(deadSecondary);
        try {
            membershipService.sendMessage(announceDeadSecondary, new DatagramSocket(), primary.getConn());
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean watchForHeartBeat() throws IOException {
        Message hbt = heartBeatService.receiveMessage(heartBeatDock);
        if (hbt == null) return false;
        if(hbt.getType() == MessageTypes.HEARTBEAT){

            String content = hbt.getContent();
            JSONObject json = new JSONObject(content);
            String distributorId = json.getString("id");

            Watcher theOne = group.get(distributorId);

            if(theOne == null){
                String ip = json.getString("ip");
                Watcher newDistributor = null;

                addBackUp(distributorId, ip, portForMemberShipConfig);

                if(primary != null) {
                    changePrimary();
                }else{

                }
            }

            theOne.lastUpdate = System.currentTimeMillis();
            theOne.health_state = HEALTH_HEALTHY;
            return true;

        }else{
            return false;
        }


    }

    public void checkDead(){
        for(Watcher monitor : group.values()){
            if(monitor.isTimeout()){

                if(monitor.whoIRepresent() == ID_PRIMARY){
                    if(changePrimary() == false){
                        System.out.println("No Primary alive, system crashed...");
                        System.exit(1);
                    }else{
                        System.out.println("Found Primary Dead, change selected a new one");
                    }

                }else if(monitor.whoIRepresent() == ID_SECONDARY){
                    System.out.printf("Found secondary %s:%d daed\n",
                            monitor.getConn().getIP(), monitor.getConn().getInetAddress());
                    if(announceSecondaryDead(monitor) == false){
                       System.out.println("\tUnable to fix");
                    }else {
                        System.out.println("\tinformed");
                    }

                }else{
                    System.out.println("Unknown representative");
                }

            }else{

            }
        }
    }
}
