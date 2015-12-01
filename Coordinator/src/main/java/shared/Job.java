package shared;

import error.WrongMessageTypeException;
import message.Message;
import message.MessageTypes;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by xingchij on 11/18/15.
 */

/**
 * this is a Job represents a test task Job object and "DELEGATE" Message
 * objects can be equally transformed between each other. NOTE: the "DELEGATE"
 * Message sent by web client contains no JobId. JobId is first assigned by web
 * client. Master , Slaves will receive "DELEGATE" Message with a unique JobId.
 */

public class Job implements JobSettings {
	private String type; // "scroll" "click"
	private String value; // like url
	private String jobId; // every job will be assigned a random but unique id
	private int userId;
	private String username;

	private String status;

	public Job(String type, String value, int uid, String username) {
		this.type = type;
		this.value = value;
		userId = uid;
		this.username = username;
		jobId = UUID.randomUUID().toString();
		status = JOB_WAITING;
	}

	public Job(String jobId, String type, String value, int uid, String username) {
		this.type = type;
		this.value = value;
		userId = uid;
		this.username = username;
		this.jobId = jobId;
		status = JOB_WAITING;
		this.jobId = jobId;
	}

	public Job(JSONObject jobjson) {
		this.jobId = jobjson.getString("jobid");
		this.type = jobjson.getString("type");
		this.value = jobjson.getString("url");

		this.userId = jobjson.getInt("userId");
		this.username = jobjson.getString("userName");
		this.status = jobjson.getString("status");
	}

	public String getJobId() {
		return this.jobId;
	}

	public String getValue() {
		return this.value;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Message generateMessage() {
		JSONObject json = new JSONObject();
		json.put("jobid", jobId);
		json.put("type", type);
		json.put("url", value);
		json.put("userId", userId);
		json.put("userName", username);
		json.put("status", status);

		Message command = new Message(MessageTypes.DELEGATE, json.toString());
		return command;
	}

	public JSONObject getJobInJson() {
		JSONObject json = new JSONObject();
		json.put("jobid", jobId);
		json.put("type", type);
		json.put("url", value);
		json.put("userId", userId);
		json.put("userName", username);
		json.put("status", status);

		return json;
	}

	public static Job getJobFromDelegateMsg(Message msg)
			throws WrongMessageTypeException {
		if (msg == null)
			return null;
		if (msg.getType() == (MessageTypes.DELEGATE)) {
			String content = msg.getContent();
			JSONObject json = new JSONObject(content);
			System.out.println(json.toString());
			String type = json.getString("type");
			String value = json.getString("url");
			int uid = json.getInt("userId");
			String username = json.getString("userName");
			String jid = null;

			try {
				jid = json.getString("jobid");
			}catch (JSONException e){
				e.printStackTrace();
			}

			if (jid != null) {
				return new Job(jid, type, value, uid, username);
			} else {
				return new Job(type, value, uid, username);
			}
		} else {
			throw new WrongMessageTypeException(msg.getType(),
					MessageTypes.DELEGATE);
		}
	}

	public static boolean isValidJobStatus(String status){
		if(status.equals(JOB_FAIL) || status.equals(JOB_SUCCESS) || status.equals(JOB_WAITING))
			return true;
		return false;
	}
}
