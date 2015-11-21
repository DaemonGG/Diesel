package services.common;

import services.io.NetService;
import services.io.UDPService;

/**
 * Created by xingchij on 11/17/15.
 */
public class NetServiceFactory {
    private NetServiceFactory(){};

    public static NetServiceProxy getRawUDPService(){
        return new NetServiceProxy(new UDPService());
    }

    public static NetServiceProxy getHeartBeatService(){
        return new NetServiceProxy(new services.heartbeat.HeartBeatService());
    }

    public static NetServiceProxy getCheckPointService(){
        return new NetServiceProxy(new services.checkpoint.CheckPointService());
    }

    public static NetServiceProxy getCommandService(){
        return new NetServiceProxy(new services.troop.CommandService());
    }
    public static NetServiceProxy getMembershipService(){
        return new NetServiceProxy(new services.heartbeat.MemberShipService());
    }
}
