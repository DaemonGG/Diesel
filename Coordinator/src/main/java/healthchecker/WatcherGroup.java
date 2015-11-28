package healthchecker;

import error.WrongMessageTypeException;
import message.Message;
import message.MessageTypes;
import message.msgconstructor.MemberShipConstructor;
import org.json.JSONObject;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import shared.ConnMetrics;

import java.io.IOException;
import java.io.WriteAbortedException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

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
        int whatIRepresent(){
            return identity;
        }
        NetConfig getConn(){
            return conn;
        }
    }

    public static final int ID_PRIMARY = 0;
    public static final int ID_SECONDARY = 1;
    public static final int ID_UNKNOWN = -1;

    public static final int HEALTH_HEALTHY = 2;
    public static final int HEALTH_DEAD = 3;
    public static final long TIMEOUT = 12000;
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

    public void serve() {

    }

    public WatcherGroup() throws SocketException {
        heartBeatDock = new DatagramSocket(portOfCoordinatorHeartBeat);
        heartBeatDock.setSoTimeout(5000);
    }

//    public void addPrimary(String id, String ip, int port) throws UnknownHostException {
//        Watcher p = new Watcher( id, ip, port, ID_PRIMARY);
//        primary = p;
//        group.put(p.getRepresentedId(), p);
//    }

    public NetConfig getPrimary(){
        if(primary == null) return null;
        return primary.getConn();
    }
    private void addBackUp(String id, String ip, int port) throws UnknownHostException {
        Watcher b = new Watcher(id , ip, port, ID_SECONDARY);


        if(primary != null && announceNewSecondary(b) == false){
            System.out.println("Announce to add new secondary totally fail");
            return;
        }
         /*
            set the first participating backup to be the next primary
         */
        if(nextPrimary == null){
            nextPrimary = b;
        }

        group.put(b.getRepresentedId(), b);
        backUpGroup.put(b.getRepresentedId(), b);
        watcherNum ++;
    }

    private boolean announceNewSecondary(Watcher newOne){
        Message newSecondaryMsg = MemberShipConstructor.newSecondaryMemberMsgConstructor
                    (newOne.getRepresentedId(), newOne.getConn());

        try {

            boolean _success = membershipService.sendMessage(newSecondaryMsg, new DatagramSocket(), primary.getConn());
            if(_success == false) return false;         // if announce to primary fails, the function return as false

            /**
             * tell all backups that there's a new guy
             * I will return true, if even only one success.
             */
            for (Watcher w : backUpGroup.values()) {

                try {
                    membershipService.sendMessage(newSecondaryMsg, new DatagramSocket(), w.getConn());

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }catch (IOException e){
            e.printStackTrace();

        }

        return true;    // otherwise return true
    }

    /**
     * When detect primary dead, it will choose another secondary to be primary
     * and choose the next secondary be a possible primary
     * @return
     */
    private boolean changePrimary() {
        if(nextPrimary == null){
            System.out.println("No backup remaining...system crash..");
            return false;
        }
        try {
            Message assignPrimary = MemberShipConstructor.youArePrimaryMsgConstructor();
            System.out.printf("Sending YOUAREPRIMARY to [id: %s, ip: %s]\n",
                    nextPrimary.getRepresentedId(), nextPrimary.getConn().getIP());
            boolean success = membershipService.sendMessage(assignPrimary, new DatagramSocket(), nextPrimary.getConn());

            // the old primary is dead, remove it from list
            if(primary !=null && group.containsKey(primary.getRepresentedId())) {
                group.remove(primary.getRepresentedId());
            }

            primary = nextPrimary;
            primary.changeIdentity(ID_PRIMARY);
            backUpGroup.remove(nextPrimary.getRepresentedId());

            if (backUpGroup.isEmpty()) {
                System.out.println("NO Secondary remaining...");
                nextPrimary = null;
            } else {
                setNextPrimary();
            }
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void setNextPrimary(){
        nextPrimary = null;
        for (Watcher w : backUpGroup.values()) {
            nextPrimary = w;
            break;
        }
    }
    /**
     *  Tell all primary, secondaries, that some secondary is now dead
     * @param deadOne
     * @return This function will return false only when announce to primary fail.
     */
    private boolean announceSecondaryDead(Watcher deadOne){
        NetConfig deadSecondary = deadOne.getConn();
        Message announceDeadSecondary = MemberShipConstructor.scondaryDeadMsgConstructor(deadOne.getRepresentedId());
        try {
            boolean _success = membershipService.sendMessage(announceDeadSecondary, new DatagramSocket(), primary.getConn());
            if(_success == false) return false;         // if announce to primary fails, the function return as false

            /**
             * tell all backups that there's a new guy
             * I will return true, if even only one success.
             */
            for (Watcher w : backUpGroup.values()) {

                try {
                    membershipService.sendMessage(announceDeadSecondary, new DatagramSocket(), w.getConn());

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            //membershipService.sendMessage(announceDeadSecondary, new DatagramSocket(), primary.getConn());
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     *  Listen for heart beat message, register new secondary when new member send heart beat.
     *  NOTE: Secondary choose his own id, and tell Coordinator his id.
     * @return The ID number of whom that sent the heart beat message
     * @throws IOException
     */
    public int watchForHeartBeat() throws IOException, WrongMessageTypeException {
        Message hbt = heartBeatService.receiveMessage(heartBeatDock);
        if (hbt == null) return ID_UNKNOWN;

        int who = -1;
        if(hbt.getType() == MessageTypes.HEARTBEAT){

            String content = hbt.getContent();
            JSONObject json = new JSONObject(content);
            String distributorId = json.getString("id");

            Watcher theOne = group.get(distributorId);


            /**
             * if I can not find the distributor, I will register this new guy
             * add him as a backup.
             * if right now, no primary, which means it's now a very starting point
             * I will automatically change this backup to be primary. and send message
             * telling him about my decision
             */
            if(theOne == null){
                String ip = json.getString("ip");
                System.out.printf("Find new distributor[id: %s, ip: %s], register it\n",
                        distributorId, ip);
                addBackUp(distributorId, ip, portForMemberShipConfig);

                if(primary == null) {
                    System.out.println("This new distributor will be primary...");
                    if(changePrimary()) {
                        who = ID_PRIMARY;
                    }
                }else{
                    who = ID_SECONDARY;
                }
            }else{
                System.out.printf("Get HeartBeat from[id: %s, ip: %s]\n",
                        theOne.getRepresentedId(), theOne.getConn().getIP());

                if(theOne.identity == ID_PRIMARY) {
                    who = ID_PRIMARY;
                }else if(theOne.identity == ID_SECONDARY){
                    who = ID_SECONDARY;
                }else{
                    who = ID_UNKNOWN;
                }
                theOne.lastUpdate = System.currentTimeMillis();
                theOne.health_state = HEALTH_HEALTHY;
            }

            return who;

        }else{
            throw new WrongMessageTypeException(hbt.getType(), MessageTypes.HEARTBEAT);
        }


    }

    public void checkDead(){
        Collection<Watcher> allMonitors = new ArrayList<Watcher>();
        for(Watcher monitor : group.values()){
            allMonitors.add(monitor);
        }

        for(Watcher monitor : allMonitors){
            if(monitor.isTimeout()){
                watcherNum --;
                monitor.health_state = HEALTH_DEAD;

                if(monitor.whatIRepresent() == ID_PRIMARY){
                    System.out.println("Find primary dead, choose a new one..");
                    if(changePrimary() == false){
                        System.out.println("No Primary alive, system crashed...");
                        primary = null;
                        System.exit(1);
                    }else{
                        System.out.println("Found Primary Dead, selected a new one");
                    }

                }else if(monitor.whatIRepresent() == ID_SECONDARY){

                    System.out.printf("Found secondary [id: %s, ip: %s] daed\n",
                            monitor.getRepresentedId(), monitor.getConn().getIP());
                    String id = monitor.getRepresentedId();

                    // if the dead secondary is the next primary, select another "nextPrimary"
                    if(id.equals(nextPrimary.getRepresentedId())){
                        setNextPrimary();
                    }
                    // remove this secondary from list
                    if(backUpGroup.containsKey(id)){
                        backUpGroup.remove(id);
                    }
                    if(group.containsKey(id)){
                        group.remove(id);
                    }

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
