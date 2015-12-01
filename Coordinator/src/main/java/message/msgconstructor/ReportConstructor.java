package message.msgconstructor;

import message.Message;
import message.MessageTypes;
import org.json.JSONObject;
import shared.Job;

/**
 * Created by xingchij on 12/1/15.
 */
public class ReportConstructor {
    public static Message generateReport(String slaveId, String jobId, String status){
        if(slaveId == null || jobId == null || status == null)
            return null;
        if(!Job.isValidJobStatus(status)){
            System.out.println("Constructing report with invalid job status!");
            return null;
        }
        JSONObject json = new JSONObject();
        json.put("sid", slaveId);
        json.put("jid", jobId);
        json.put("status", status);

        return new Message(MessageTypes.REPORT, json.toString());
    }
}
