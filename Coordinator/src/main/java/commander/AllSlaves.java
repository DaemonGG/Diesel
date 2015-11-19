package commander;

import services.common.NetServiceProxy;
import services.io.NetConfig;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by xingchij on 11/18/15.
 */
public class AllSlaves {
    class Slave {
        String ip;
        NetConfig delegateTaskConn;


        List<Job> jobList = new ArrayList<Job>();
        int jobsNum = 0;

        public Slave(String ip, int port) throws UnknownHostException {
            delegateTaskConn = new NetConfig(ip, port);
            this.ip = ip;
        }

        public String getSlaveIP(){
            return ip;
        }

        /**
         * this slave will send this job to the real slave
         * if sent success, he will truly take this job.
         * Otherwise IOException throws out
         * @param job
         * @param commander
         * @throws IOException
         */
         void takeNewJob(Job job, NetServiceProxy commander) throws IOException {
            DatagramSocket server = new DatagramSocket();

            try {
                boolean success = commander.sendMessage(job.generateMessage(), server, delegateTaskConn);
                if(success){
                    jobList.add(job);
                    jobsNum++;
                }
            }catch (IOException e){
                e.printStackTrace();

            }finally {
                server.close();
            }

        }

    }
    HashMap<String, Slave> slaves = new HashMap<String, Slave>();
    ArrayList<String> slavesIPList = new ArrayList<String>();
    int index = 0;

    private AllSlaves(){};
    private static AllSlaves instance=null;

    public static AllSlaves getOffice(){
        if (instance == null){
            instance = new AllSlaves();
        }

        return instance;
    }

    public void addSlave(String ip, int port) throws UnknownHostException {
        Slave newSlave = new Slave(ip, port);
        slaves.put(ip, newSlave);
        slavesIPList.add(ip);
    }

    public void delSlave(String ip){
        slavesIPList.remove(ip);
        slaves.remove(ip);
    }

    public void pushOneJob(Job job, NetServiceProxy commander) throws IOException {
        String targetIP = slavesIPList.get(index);
        Slave slave = slaves.get(targetIP);

        slave.takeNewJob(job, commander);
    }
}
