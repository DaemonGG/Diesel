package message.msgconstructor;

import commander.Job;
import message.Message;
import message.MessageTypes;
import org.json.JSONObject;
import services.io.NetConfig;

/**
 * Created by xingchij on 11/19/15.
 */
public class CheckPointConstructor {
    public static final String ADD_JOB = "addJob";
    public static final String ADD_SLAVE = "addSlave";
    public static final String SET_JOB_STATUS = "setJobStatus";

    public static Message constructAddJobMessage(Job job, NetConfig slave){
        JSONObject json = job.getJobInJson();
        json.put("ip", slave.getIP());
        json.put("port", slave.getPort());
        json.put("checktype", ADD_JOB);
        String content = json.toString();
        return new Message(MessageTypes.CHECKPOINT, content);
    }
    public static Message constructAddSlaveMessage(NetConfig slave){
        JSONObject json = new JSONObject();
        json.put("ip", slave.getIP());
        json.put("port", slave.getPort());
        json.put("checktype", ADD_SLAVE);
        String content = json.toString();
        return new Message(MessageTypes.CHECKPOINT, content);
    }

    public static Message constructSetJobStatusMessage(NetConfig slave, String jobId, String status){
        JSONObject json = new JSONObject();
        json.put("ip", slave.getIP());
        json.put("port", slave.getPort());
        json.put("jobid", jobId);
        json.put("status", status);
        json.put("checktype", SET_JOB_STATUS);
        String content = json.toString();
        return new Message(MessageTypes.CHECKPOINT, content);
    }
}
