package message.msgconstructor;

import message.Message;
import message.MessageTypes;
import org.json.JSONObject;

/**
 * Created by xingchij on 11/20/15.
 */
public class HeartBeatConstructor {
    /**
     *
     * @param id  id of this distributor
     * @param ip  Coordinator need ip when this heartbeat come from a stranger
     * @return
     */
    public static Message constructHeartBeat(String id, String ip){
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("ip", ip);
        Message hbt = new Message(MessageTypes.HEARTBEAT, json.toString());
        return hbt;
    }
}
