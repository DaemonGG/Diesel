package shared;

import error.WrongMessageTypeException;
import message.Message;
import message.MessageTypes;
import org.json.JSONArray;
import org.json.JSONObject;
import services.common.NetServiceProxy;
import services.io.NetConfig;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by xingchij on 11/18/15.
 */
public class AllSlaves {
	public static final long TIMEOUT = 10000;

	public static final int HEALTH_HEALTHY = 2;
	public static final int HEALTH_DEAD = 3;

	/**
	 * a class representing a slave use ip + port(port for delegate tasks) to
	 * identify a slave
	 */
	class Slave {
		String id;
		NetConfig delegateTaskConn;
		int health_state;
		long lastUpdate;

		List<Job> jobList = new ArrayList<Job>();
		HashMap<String, Job> jobMap = new HashMap<String, Job>();
		int jobsNum = 0;

		public Slave(String id, String ip) throws UnknownHostException {
			delegateTaskConn = new NetConfig(ip,
					ConnMetrics.portOfSlaveDelegateTask);
			this.id = id;
			lastUpdate = System.currentTimeMillis();
			this.health_state = HEALTH_HEALTHY;
		}

		public void refresh(){
			lastUpdate = System.currentTimeMillis();
			this.health_state = HEALTH_HEALTHY;
		}

		public NetConfig getNetConfigOfSlave() {
			return delegateTaskConn;
		}

		public String getSlaveIP() {
			return delegateTaskConn.getIP();
		}

		public String getId() {
			return id;
		}

		boolean isTimeout() {
			return (System.currentTimeMillis() - lastUpdate) > TIMEOUT;
		}

		/**
		 * this slave will send this job to the real slave if sent success, he
		 * will truly take this job. Otherwise IOException throws out
		 * 
		 * @param job
		 * @param commander
		 * @throws IOException
		 */
		boolean takeNewJob(Job job, NetServiceProxy commander) throws IOException {
			DatagramSocket server = new DatagramSocket();
			boolean success = false;
			try {
				success = commander.sendMessage(job.generateMessage(),
						server, delegateTaskConn);
				if (success) {
					jobList.add(job);
					jobMap.put(job.getJobId(), job);
					jobsNum++;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				server.close();
				return success;
			}

		}

		void checkPointAddNewJob(Job job) {
			jobList.add(job);
			jobMap.put(job.getJobId(), job);
			jobsNum++;
		}

		void setJobStatus(String jobId, String status) {
			Job job = jobMap.get(jobId);
			if (job == null) {
				System.out.printf("Try to set a non-exist job[%s] status\n",
						jobId);
				return;
			}
			job.setStatus(status);
		}
		JSONObject dump(){
			JSONObject json = new JSONObject();
			json.put("sid", id);
			json.put("ip", getSlaveIP());

			JSONArray jobarray = new JSONArray();
			for(Job job: jobMap.values()){
				jobarray.put(job.getJobInJson());
			}
			json.put("jobs", jobarray);
			return json;
		}
	}

	HashMap<String, Slave> slaves = new HashMap<String, Slave>();
	ArrayList<String> slavesIdList = new ArrayList<String>();
	int index = 0;

	private AllSlaves() {
	};

	private static AllSlaves instance = null;

	public static AllSlaves getOffice() {
		if (instance == null) {
			instance = new AllSlaves();
		}
		return instance;
	}
	public void refreshAll(){
		for(Slave s: slaves.values()){
			s.refresh();
		}
	}
	public void addSlave(String id, String ip) throws UnknownHostException {
		Slave newSlave = new Slave(id, ip);
		slaves.put(id, newSlave);
		slavesIdList.add(id);
	}

	public void delSlave(String id, Queue<Job> unfinishedQueue) {
		// dump unfinished jobs out from this dead slave

		Slave monitor = slaves.get(id);
		if(monitor == null) return;
		for(Job j : monitor.jobList){
			if(!j.getStatus().equals(JobSettings.JOB_SUCCESS)){
				unfinishedQueue.add(j);
			}
		}

		slaves.remove(id);
		slavesIdList.remove(id);
	}

	public String getSlaveIp(String id){
		Slave s = slaves.get(id);
		if(s == null) return null;

		return s.getSlaveIP();
	}
	/**
	 * This function will find a slave to delegate the task, in a round robin
	 * way
	 * 
	 * @param job
	 * @param commander
	 * @return the id of this delegated slave.
	 * @throws IOException
	 */
	public String pushOneJob(Job job, NetServiceProxy commander)
			throws IOException {
		if(index >= slavesIdList.size()) {
			System.out.println("No Slave is working now, unable to serve");
			return null;
		}
		String targetKey = slavesIdList.get(index);
		index++;

		if (targetKey == null) {
			return null;
		}

		if (index >= slavesIdList.size())
			index = 0;

		Slave slave = slaves.get(targetKey);

		boolean success = slave.takeNewJob(job, commander);
		if(success)
			return slave.getId();
		else return null;
	}

	public void setJobStatus(String slaveId, String jobId, String status) {
		Slave slave = slaves.get(slaveId);
		if (slave == null) {
			System.out.printf("Try to set a non-exist slave[%s] status\n",
					slaveId);
			return;
		}
		slave.setJobStatus(jobId, status);
	}

	public void checkAddNewJob(String slaveId, Job job) {
		if (job == null)
			return;
		Slave slave = slaves.get(slaveId);
		if (slave == null) {
			System.out
					.printf("Try to checkpoint job, but slave not exist[%s]\n",
							slaveId);
			return;
		}
		slave.checkPointAddNewJob(job);
	}

	public List<String> checkDead(Queue<Job> unfinishedQueue) {
		Collection<Slave> allMonitors = new ArrayList<Slave>();
		for (Slave monitor : slaves.values()) {
			allMonitors.add(monitor);
		}
		List<String> death = new ArrayList<String>();

		for (Slave monitor : allMonitors) {
			if (monitor.isTimeout()) {
				monitor.health_state = HEALTH_DEAD;

				String sid = monitor.getId();

				// dump unfinished jobs out from this dead slave
				for(Job j : monitor.jobList){
					if(!j.getStatus().equals(JobSettings.JOB_SUCCESS)){
						unfinishedQueue.add(j);
					}
				}

				slaves.remove(sid);
				slavesIdList.remove(sid);
				death.add(sid);
				CurrentTime.tprintln(String.format(
						"DETECTED: Find Slave [id: %s, ip: %s] dead, REMOVED\n",
						monitor.getId(), monitor.getSlaveIP()));
			} else {

			}
		}
		return death;
	}

	/**
	 *
	 * @param heartBeatService
	 * @param heartBeatDock
	 * @return The id of a new slave, if it's not  a new slave, return null
	 * @throws IOException
	 * @throws WrongMessageTypeException
     */
	public String watchForHeartBeat(NetServiceProxy heartBeatService,
									DatagramSocket heartBeatDock) throws IOException,
			WrongMessageTypeException {
		Message hbt = heartBeatService.receiveMessage(heartBeatDock);
		if (hbt == null)
			return null;

		int who = -1;
		if (hbt.getType() == MessageTypes.HEARTBEAT) {

			String content = hbt.getContent();
			JSONObject json = new JSONObject(content);
			String slaveId = json.getString("id");

			Slave theOne = slaves.get(slaveId);

			/**
			 * if I can not find the distributor, I will register this new guy
			 * add him as a backup. if right now, no primary, which means it's
			 * now a very starting point I will automatically change this backup
			 * to be primary. and send message telling him about my decision
			 */
			if (theOne == null) {
				String ip = json.getString("ip");
				CurrentTime.tprintln(String.format(
						"SCALE: Find new slave[id: %s, ip: %s], register it\n",
						slaveId, ip));
				addSlave(slaveId, ip);
				return slaveId;
			} else {
				System.out.printf("Get HeartBeat from[id: %s, ip: %s]\n",
						theOne.getId(), theOne.getSlaveIP());

				theOne.lastUpdate = System.currentTimeMillis();
				theOne.health_state = HEALTH_HEALTHY;
			}

		} else {
			throw new WrongMessageTypeException(hbt.getType(),
					MessageTypes.HEARTBEAT);
		}

		return null;
	}
	public JSONArray dump(){
		JSONArray slaveArray = new JSONArray();
		for(Slave s: slaves.values()){
			slaveArray.put(s.dump());
		}
		return slaveArray;
	}

	public void construct(JSONArray sarray){
		slaves.clear();
		slavesIdList.clear();

		for(int i=0; i<sarray.length(); i++){
			try {
				JSONObject slave = sarray.getJSONObject(i);
				String sid = slave.getString("sid");
				String ip = slave.getString("ip");
				addSlave(sid, ip);
				Slave s = slaves.get(sid);

				JSONArray jobarray = slave.getJSONArray("jobs");
				for(int j=0; j<jobarray.length(); j++){
					JSONObject job = jobarray.getJSONObject(j);
					s.checkPointAddNewJob(new Job(job));
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}
}
