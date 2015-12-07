package message.msgconstructor;

import message.Message;
import message.MessageTypes;
import org.json.JSONObject;

/**
 * Created by xingchij on 11/28/15.
 */
public class WhoIsPrimaryConstructor {

	public static Message constructQuery(String ip, int port) {
		JSONObject json = new JSONObject();
		json.put("sip", ip);
		json.put("port", port);
		return new Message(MessageTypes.WHOISPRIMARY, json.toString());
	}

	public static Message constructAnswer(String primaryIp) {
		JSONObject json = new JSONObject();
		
		if(primaryIp == null) primaryIp = "None";
		json.put("ip", primaryIp);
	
		return new Message(MessageTypes.WHOISPRIMARY, json.toString());
	}
	
	public static Message constructDBAnswer( String dbIP) {
		JSONObject json = new JSONObject();
		
		if(dbIP == null) dbIP = "None";
		json.put("dbip", dbIP);
		return new Message(MessageTypes.WHOISPRIMARY, json.toString());
	}
}
