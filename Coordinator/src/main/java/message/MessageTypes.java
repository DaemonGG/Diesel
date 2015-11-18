package message;

/**
 * Created by xingchij on 11/17/15.
 */
public class MessageTypes {
    public final static int CHECKPOINT = 0;
    public final static int HEARTBEAT = 1;
    public final static int DELEGATE = 2;

    public static boolean unknownType(int type){
        if(type<0 || type>2) return true;
        return false;
    }
}
