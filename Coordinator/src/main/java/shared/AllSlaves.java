package shared;

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
    public static final long TIMEOUT = 12000;
    /**
     * a class representing a slave
     * use ip + port(port for delegate tasks) to identify a slave
     */
    class Slave {
        String id;
        NetConfig delegateTaskConn;
        long lastUpdate;

        List<Job> jobList = new ArrayList<Job>();
        HashMap<String, Job> jobMap = new HashMap<String, Job>();
        int jobsNum = 0;

        public Slave(String id, String ip) throws UnknownHostException {
            delegateTaskConn = new NetConfig(ip, ConnMetrics.portOfSlaveDelegateTask);
            this.id = id;
        }
        public NetConfig getNetConfigOfSlave(){
            return delegateTaskConn;
        }
        public String getSlaveIP(){
            return delegateTaskConn.getIP();
        }
        public String getId(){
            return id;
        }
        boolean isTimeout(){
            return (System.currentTimeMillis() - lastUpdate) > TIMEOUT;
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
        void checkPointAddNewJob(Job job){
            jobList.add(job);
            jobMap.put(job.getJobId(), job);
            jobsNum++;
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
    ArrayList<String> slavesIdList = new ArrayList<String>();
    int index = 0;

    private AllSlaves(){};
    private static AllSlaves instance=null;

    public static AllSlaves getOffice(){
        if (instance == null){
            instance = new AllSlaves();
        }

        return instance;
    }

    public void addSlave(String id, String ip) throws UnknownHostException {
        Slave newSlave = new Slave(id, ip);
        slaves.put(id, newSlave);
        slavesIdList.add(id);
    }

    public void delSlave(String id){
        //slavesIPList.remove(ip);
        slaves.remove(id);
        slavesIdList.remove(id);
    }

    /**
     * This function will find a slave to delegate the task, in a round robin way
     *
     * @param job
     * @param commander
     * @return the id of this delegated slave.
     * @throws IOException
     */
    public String pushOneJob(Job job, NetServiceProxy commander) throws IOException {
        String targetKey = slavesIdList.get(index);
        index++;

        if(targetKey == null){
            return null;
        }

        if(index >= slavesIdList.size()) index = 0;

        Slave slave = slaves.get(targetKey);


        slave.takeNewJob(job, commander);

        return slave.getId();
    }
    public void setJobStatus(String slaveId, String jobId, String status){
        Slave slave = slaves.get(slaveId);
        if(slave == null){
            System.out.printf("Try to set a non-exist slave[%s] status\n", slaveId);
            return;
        }
        slave.setJobStatus(jobId, status);
    }
    public void checkAddNewJob(String slaveId, Job job){
        if(job == null) return;
        Slave slave = slaves.get(slaveId);
        if(slave == null){
            System.out.printf("Try to checkpoint job, but slave not exist[%s]\n", slaveId);
            return;
        }
        slave.checkPointAddNewJob(job);
    }
}
