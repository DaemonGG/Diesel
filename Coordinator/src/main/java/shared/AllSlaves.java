package shared;

import commander.Job;
import services.common.NetServiceProxy;
import services.io.NetConfig;

import java.io.IOException;
import java.net.DatagramSocket;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by xingchij on 11/18/15.
 */
public class AllSlaves {

    /**
     * a class representing a slave
     * use ip + port(port for delegate tasks) to identify a slave
     */
    class Slave {
        String ip;
        int port;
        NetConfig delegateTaskConn;


        List<Job> jobList = new ArrayList<Job>();
        HashMap<String, Job> jobMap = new HashMap<String, Job>();
        int jobsNum = 0;

        public Slave(String ip, int port) throws UnknownHostException {
            delegateTaskConn = new NetConfig(ip, port);
            this.ip = ip;
            this.port = port;
        }
        public NetConfig getNetConfigOfSlave(){
            return delegateTaskConn;
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
                    jobMap.put(job.getJobId(), job);
                    jobsNum++;
                }
            }catch (IOException e){
                e.printStackTrace();

            }finally {
                server.close();
            }

        }
        void checkPointAddNewJob(){

        }
        void setJobStatus(String jobId, String status){
            Job job = jobMap.get(jobId);
            if(job == null){
                System.out.printf("Try to set a non-exist job[%s] status\n", jobId);
                return;
            }
            job.setStatus(status);
        }

    }
    HashMap<String, Slave> slaves = new HashMap<String, Slave>();
    ArrayList<String> slavesAddrList = new ArrayList<String>();
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
        slaves.put(ip+port, newSlave);
        slavesAddrList.add(ip);
    }

    public void delSlave(String ip){
        //slavesIPList.remove(ip);
        slaves.remove(ip);
    }

    /**
     * This function will find a slave to delegate the task, in a round robin way
     *
     * @param job
     * @param commander
     * @return the Network infoamtion of this delegated slave.
     * @throws IOException
     */
    public NetConfig pushOneJob(Job job, NetServiceProxy commander) throws IOException {
        String targetKey = slavesAddrList.get(index);
        index++;

        if(targetKey == null){
            return null;
        }

        if(index >= slavesAddrList.size()) index = 0;

        Slave slave = slaves.get(targetKey);


        slave.takeNewJob(job, commander);

        return slave.getNetConfigOfSlave();
    }
    public void setJobStatus(String slaveKey, String jobId, String status){
        Slave slave = slaves.get(slaveKey);
        if(slave == null){
            System.out.printf("Try to set a non-exist slave[%s] status\n", slaveKey);
            return;
        }
        slave.setJobStatus(jobId, status);
    }
}
