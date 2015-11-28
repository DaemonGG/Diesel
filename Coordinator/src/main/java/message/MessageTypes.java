package message;

/**
 * Created by xingchij on 11/17/15.
 */
public class MessageTypes {
    public final static int CHECKPOINT = 0;
    public final static int HEARTBEAT = 1;
    /*
      Task delegate message. Used when web client send to primary and primary send to slaves
     */
    public final static int DELEGATE = 2;
    /*
       Message used when slaves report running result( SUCCESS or FAILURE) to primary.
     */
    public final static int REPORT = 3;
    public final static int ACK = 4;
    public final static int MEMBERSHIP = 5;

    public static boolean unknownType(int type){
        if(type<0 || type>5) return true;
        return false;
    }

    public static String explain(int type){
        if(unknownType(type)) return "Unknown";
        String explain = null;
        switch(type){
            case CHECKPOINT:
                explain = "CheckPoint";
                break;
            case HEARTBEAT:
                explain = "HeartBeat";
                break;
            case DELEGATE:
                explain = "Delegate";
                break;
            case REPORT:
                explain = "Report";
                break;
            case ACK:
                explain = "ACK";
                break;
            case MEMBERSHIP:
                explain = "MemberShip";
                break;
            default:
                break;
        }
        return explain;
    }
}
