package shared;

import org.json.JSONArray;
import org.json.JSONObject;
import services.io.NetConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by xingchij on 11/19/15.
 */
public class AllSecondaries {
	// private List<String> secondariesList;
	private HashMap<String, NetConfig> secondariesMap = new HashMap<String, NetConfig>();
	int checkPort = ConnMetrics.portOfSecondaryCheckPoint;
	int num = 0;

	private AllSecondaries() {
	};

	private static AllSecondaries instance = null;

	public static AllSecondaries getInstance() {
		if (instance == null) {
			instance = new AllSecondaries();
		}
		return instance;
	}

	public void addSecondary(String id, String ip, int port)
			throws UnknownHostException {
		NetConfig newBackup = new NetConfig(ip, port);
		secondariesMap.put(id, newBackup);
		num++;
	}

	public void delSecondary(String id) {
		secondariesMap.remove(id);
		num--;
	}

	public NetConfig generateBrdCastNetConfig() {
		List<InetAddress> addresses = new ArrayList<InetAddress>();
		for (NetConfig conf : secondariesMap.values()) {
			addresses.add(conf.getInetAddress());
		}
		NetConfig brdCastNetConfig = new NetConfig(addresses, checkPort);
		return brdCastNetConfig;
	}

	public JSONArray dump(){
		JSONArray secondaryArray = new JSONArray();
		for(String id: secondariesMap.keySet()){
			JSONObject secondary = new JSONObject();
			secondary.put("secondaryId", id);
			NetConfig conn = secondariesMap.get(id);
			secondary.put("ip",conn.getIP());
			secondary.put("port", conn.getPort());
			secondaryArray.put(secondary);
		}
		return secondaryArray;
	}
	public void construct(JSONArray jarray){
		secondariesMap.clear();

		for(int i=0; i<jarray.length(); i++){
			try {
				JSONObject secjson = jarray.getJSONObject(i);
				String id = secjson.getString("secondaryId");
				String ip = secjson.getString("ip");
				int port = secjson.getInt("port");
				addSecondary(id, ip, port);
			}catch (UnknownHostException e){
				e.printStackTrace();
			}
		}
	}
}
