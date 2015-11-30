package message;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by xingchij on 11/17/15.
 */
public class Message implements Serializable {
	private String content;
	private int type;

	public String getContent() {
		return content;
	}

	public int getType() {
		return type;
	}

	public Message(int type, String content) {
		this.type = type;
		this.content = content;
	}

	public String toString() {
		JSONObject json = new JSONObject();
		try {
			json.put("type", type);
			json.put("content", content);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json.toString();
	}
}
