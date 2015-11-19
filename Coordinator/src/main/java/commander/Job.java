package commander;

import message.Message;
import message.MessageTypes;
import org.json.JSONObject;


import java.util.UUID;

/**
 * Created by xingchij on 11/18/15.
 */
public class Job implements JobSettings{
    private String type;
    private String value;
    private String jobId;
    private int userId;
    private String username;

    private String status;

    public Job(String type, String value, int uid, String username){
        this.type = type;
        this.value = value;
        userId = uid;
        this.username = username;
        jobId = UUID.randomUUID().toString();
    }

    public Message generateMessage(){
        JSONObject json = new JSONObject();
        json.put("type",type);
        json.put("url", value);
        json.put("userId", userId);
        json.put("userName", username);

        Message command = new Message(MessageTypes.DELEGATE, json.toString());
        return command;
    }

    public static Job getJobFromDelegateMsg(Message msg){
        if(msg.getType() == (MessageTypes.DELEGATE)){
            String content = msg.getContent();
            JSONObject json = new JSONObject(content);
            String type = json.getString("type");
            String value = json.getString("value");
            int uid = json.getInt("userId");
            String username = json.getString("userName");

            return new Job(type, value, uid, username);
        }
        return null;
    }
}
