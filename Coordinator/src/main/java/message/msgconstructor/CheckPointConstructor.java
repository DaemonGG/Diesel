package message.msgconstructor;

import shared.Job;
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

    public static Message constructAddJobMessage(Job job, String id){
        if(job == null || id == null) return null;
        JSONObject json = new JSONObject();
        JSONObject jobjson = job.getJobInJson();
        json.put("jobDetail", jobjson);
        json.put("sid", id);
        json.put("checktype", ADD_JOB);
        String content = json.toString();
        return new Message(MessageTypes.CHECKPOINT, content);
    }
    public static Message constructAddSlaveMessage(NetConfig slave, String id){
        if(slave == null) return null;

        JSONObject json = new JSONObject();
        json.put("ip", slave.getIP());
        json.put("sid", id);
        json.put("checktype", ADD_SLAVE);
        String content = json.toString();
        return new Message(MessageTypes.CHECKPOINT, content);
    }

    public static Message constructSetJobStatusMessage(String id, String jobId, String status){
        if(id == null || jobId == null || status == null) return null;

        JSONObject json = new JSONObject();
        json.put("sid", id);
        json.put("jobid", jobId);
        json.put("status", status);
        json.put("checktype", SET_JOB_STATUS);
        String content = json.toString();
        return new Message(MessageTypes.CHECKPOINT, content);
    }
}
