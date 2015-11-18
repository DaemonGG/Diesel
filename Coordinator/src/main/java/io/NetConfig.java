package io;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by xingchij on 11/17/15.
 */
public class NetConfig {
    private String IP;
    private InetAddress inetAddress;
    private int port;
    public NetConfig(String ip, int port) throws UnknownHostException {
        IP = ip;
        this.port = port;
        inetAddress = InetAddress.getByAddress(ip.getBytes());
    }
    public NetConfig(InetAddress addr, int port){
        inetAddress = addr;
        this.port = port;
    }
    public String getIP(){
        return IP;
    }
    public InetAddress getInetAddress(){
        return inetAddress;
    }
    public int getPort(){
        return port;
    }
}
