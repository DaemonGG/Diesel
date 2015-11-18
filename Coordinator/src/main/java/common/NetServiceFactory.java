package common;

import io.NetService;
import io.UDPService;

/**
 * Created by xingchij on 11/17/15.
 */
public class NetServiceFactory {
    private NetServiceFactory(){};

    public static NetServiceProxy getRawUDPService(){
        return new NetServiceProxy(new UDPService());
    }

    public static NetServiceProxy getHeartBeatService(){
        return new NetServiceProxy(new heartbeat.HeartBeatService());
    }

    public static NetServiceProxy getCheckPointService(){
        return new NetServiceProxy(new checkpoint.CheckPointService());
    }

    public static NetService getCommandService(){
        return new NetServiceProxy(new troop.CommandService());
    }

}
