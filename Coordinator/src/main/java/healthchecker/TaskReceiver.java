package healthchecker;
import error.WrongMessageTypeException;
import shared.Job;
import message.Message;
import message.MessageTypes;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import shared.ConnMetrics;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by xingchij on 11/26/15.
 */
public class TaskReceiver implements  Runnable, ConnMetrics {

    private LinkedBlockingQueue<Job> pendingJob;
    private final static int MAX_JOB_NUM = 9999;
    private DatagramSocket recvNewJobDock;
    private NetServiceProxy commandService;
    private int JobNum;

    /**
     * offer one job into the queue
     * @param newJob
     * @return false if the queue is full
     */
    private boolean addJob(Job newJob){
        return pendingJob.offer(newJob);
    }

    /**
     * get one job out of the queue
     * @return  A "DELEGATE" message
     *          null if the queue is empty
     */
    private Message getNextJob(){
        if(pendingJob.isEmpty()) return null;
        return pendingJob.poll().generateMessage();
    }

    public TaskReceiver() throws SocketException {
        pendingJob = new LinkedBlockingQueue<Job>(MAX_JOB_NUM);
        recvNewJobDock = new DatagramSocket(portOfCoordinatorRecvJobs);
        JobNum = 0;
        commandService = NetServiceFactory.getCommandService();
    }

    public void run() {
        while(true){
            serve();
        }
    }

    public void closeConnections() {
        recvNewJobDock.close();
    }

    public void serve() {
        try {
            Message newTask = commandService.recvAckMessage(recvNewJobDock);
            if(newTask.getType() != MessageTypes.DELEGATE){
                System.out.println("Receive wrong message type. It should be DELEGATE message");
                return;
            }
            Job newJob = Job.getJobFromDelegateMsg(newTask);
            addJob(newJob);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WrongMessageTypeException e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
    public boolean sendTask(NetConfig primary){
        if(primary == null) return false;
        Message task = getNextJob();
        try {
            commandService.sendMessage(task, new DatagramSocket(), primary);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
