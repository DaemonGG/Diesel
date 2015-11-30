package message.msgconstructor;

import message.Message;
import message.MessageTypes;

import org.json.JSONObject;

import services.io.NetConfig;

/**
 * Created by xingchij on 11/20/15.
 */
public class MemberShipConstructor {
	// public static final String PRIMARYCHANGED = "primary changed";
	public static final String YOUAREPRIMARY = "you're primary";
	public static final String SECONDARYDEAD = "secondary dead";
	// public static final String NEWSLAVE = "new slave";
	public static final String NEWSECONDARY = "new secondary";

	public static Message primaryChangedMsgConstructor() {
		return null;
	}

	public static Message youArePrimaryMsgConstructor() {
		JSONObject json = new JSONObject();
		json.put("type", YOUAREPRIMARY);
		return new Message(MessageTypes.MEMBERSHIP, json.toString());
	}

	public static Message scondaryDeadMsgConstructor(String id) {
		JSONObject json = new JSONObject();
		json.put("id", id);
		// json.put("ip", deadSecondary.getIP());
		// json.put("port", deadSecondary.getPort());
		json.put("type", SECONDARYDEAD);

		return new Message(MessageTypes.MEMBERSHIP, json.toString());
	}

	/**
	 * When coordinator find out a new secondary, it will send this msg to
	 * primary which will trigger checkpoint
	 * 
	 * @param newSecondary
	 * @return
	 */
	public static Message newSecondaryMemberMsgConstructor(String id,
			NetConfig newSecondary) {
		JSONObject json = new JSONObject();
		json.put("ip", newSecondary.getIP());
		json.put("id", id);
		json.put("type", NEWSECONDARY);

		return new Message(MessageTypes.MEMBERSHIP, json.toString());
	}

	public static Message newSlaveMemberMsgConstructor() {
		return null;
	}
}
