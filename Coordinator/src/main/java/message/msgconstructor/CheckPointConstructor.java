package message.msgconstructor;

import distributor.Distributer;
import message.Message;
import message.MessageTypes;
import org.json.JSONArray;
import org.json.JSONObject;
import shared.Job;

/**
 * Created by xingchij on 11/19/15.
 */
public class CheckPointConstructor {
	public static final String ADD_JOB = "addJob";
	public static final String ADD_UNF_JOB = "addunfJob";

	public static final String ADD_SLAVE = "addSlave";
	public static final String DEAD_SLAVE = "delSlave";

	public static final String SET_JOB_STATUS = "setJobStatus";
	public static final String SNAPSHOT = "snapshot";


	public static Message constructAddJobMessage(Job job, String id) {
		if (job == null || id == null)
			return null;
		JSONObject json = new JSONObject();
		JSONObject jobjson = job.getJobInJson();
		json.put("jobDetail", jobjson);
		json.put("sid", id);
		json.put("checktype", ADD_JOB);
		String content = json.toString();
		return new Message(MessageTypes.CHECKPOINT, content);
	}
	
	public static Message constructAddUnfJobMessage(Job job) {
		if (job == null )
			return null;
		JSONObject json = new JSONObject();
		JSONObject jobjson = job.getJobInJson();
		json.put("jobDetail", jobjson);
		json.put("checktype", ADD_UNF_JOB);
		String content = json.toString();
		return new Message(MessageTypes.CHECKPOINT, content);
	}

	public static Message constructAddSlaveMessage(String slaveIp, String id) {
		if (slaveIp == null)
			return null;

		JSONObject json = new JSONObject();
		json.put("ip", slaveIp);
		json.put("sid", id);
		json.put("checktype", ADD_SLAVE);
		String content = json.toString();
		return new Message(MessageTypes.CHECKPOINT, content);
	}

	public static Message constructDelSlaveMessage( String id) {
		if (id == null)
			return null;

		JSONObject json = new JSONObject();
		json.put("sid", id);
		json.put("checktype", DEAD_SLAVE);
		String content = json.toString();
		return new Message(MessageTypes.CHECKPOINT, content);
	}

	public static Message constructSetJobStatusMessage(String id, String jobId,
			String status) {
		if (id == null || jobId == null || status == null)
			return null;

		JSONObject json = new JSONObject();
		json.put("sid", id);
		json.put("jobid", jobId);
		json.put("status", status);
		json.put("checktype", SET_JOB_STATUS);
		String content = json.toString();
		return new Message(MessageTypes.CHECKPOINT, content);
	}
	public static Message constructSnapShotMessage(Distributer server){
		JSONArray secondaries = server.backUps.dump();
		JSONArray slaves = server.slaveOffice.dump();
		JSONObject json = new JSONObject();

		json.put("secondaries", secondaries);
		json.put("slaves", slaves);
		json.put("unfinished", server.dumpQueue());
		json.put("checktype", SNAPSHOT);

		return new Message(MessageTypes.CHECKPOINT, json.toString());
	}
}
