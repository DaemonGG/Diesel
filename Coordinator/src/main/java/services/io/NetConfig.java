package services.io;

import java.net.*;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by xingchij on 11/17/15.
 */
public class NetConfig {
	private String IP;
	private InetAddress inetAddress;
	private List<InetAddress> brdCastAddresses;
	private int port;

	public NetConfig(String ip, int port) throws UnknownHostException {
		IP = ip;
		this.port = port;

		inetAddress = InetAddress.getByName(ip);
	}

	public NetConfig(InetAddress addr, int port) {
		inetAddress = addr;
		this.port = port;
	}

	public NetConfig(List<InetAddress> brdCastAddresses, int port) {
		this.brdCastAddresses = brdCastAddresses;
		this.port = port;
	}

	public List<InetAddress> getBrdCastAddr() {
		return brdCastAddresses;
	}

	public String getIP() {
		return IP;
	}

	public InetAddress getInetAddress() {
		return inetAddress;
	}

	public int getPort() {
		return port;
	}

	public static String getMyIp() throws SocketException {
		String myip = null;
		NetworkInterface ni = NetworkInterface.getByName("wlan0");
		Enumeration inetAddress = ni.getInetAddresses();

		if (!inetAddress.hasMoreElements()) {
			return null;
		}

		InetAddress addr = (InetAddress) inetAddress.nextElement();

		while (inetAddress.hasMoreElements()) {
			addr = (InetAddress) inetAddress.nextElement();
			if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
				myip = addr.getHostAddress();
				break;
			}
		}
		return myip;
	}

}
