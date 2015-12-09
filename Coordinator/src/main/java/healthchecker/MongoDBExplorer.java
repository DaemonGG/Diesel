package healthchecker;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import error.WrongMessageTypeException;
import message.Message;
import message.MessageTypes;
import message.msgconstructor.WhoIsPrimaryConstructor;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import shared.ConnMetrics;

public class MongoDBExplorer implements Runnable{
	List<String> IPList;
	private String ip;
	NetServiceProxy tellAboutPrimaryService;
	DatagramSocket whoIsPrimaryDock;
	public static final int RECV_WHOISPRIMARY_TIMEOUT = 3000;

	public MongoDBExplorer(String[] dbIPs) throws SocketException{
		IPList = new ArrayList<String>();
		for(String s : dbIPs){
			IPList.add(s);
		}
		whoIsPrimaryDock = new DatagramSocket(ConnMetrics.portOfCoordinatorForPrimaryDB);
		tellAboutPrimaryService = NetServiceFactory.getRawUDPService();
		whoIsPrimaryDock.setSoTimeout(RECV_WHOISPRIMARY_TIMEOUT);
	}
	
	private String explore(){
		for(String s:IPList){
			if(MongoDBConn.insertNonsense(s)){
				return s;
			}
		}
		return "None";
	}
	private void watchForQuery() throws IOException,
	WrongMessageTypeException{
		Message query = tellAboutPrimaryService
				.receiveMessage(whoIsPrimaryDock);

		if (query == null)
			return;

		if (query.getType() != MessageTypes.WHOISPRIMARY) {
			throw new WrongMessageTypeException(query.getType(),
					MessageTypes.WHOISPRIMARY);
		}

		String content = query.getContent();
		JSONObject json = new JSONObject(content);
		String ip = json.getString("sip");
		int port = json.getInt("port");
		
		//String dbip = explore();
		if(ip == null) ip = "None";
		Message answer = WhoIsPrimaryConstructor.constructDBAnswer(ip);
		System.out.printf("Tell [ip:%s, port:%d] db primary address\n",  ip, port);
		tellAboutPrimaryService.sendMessage(answer, whoIsPrimaryDock,
				new NetConfig(ip, port));
	}
	@Override
	public void run() {
		while(true){
			try {
				ip = explore();
				System.out.println(ip);
				watchForQuery();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WrongMessageTypeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
}
