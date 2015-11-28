package healthchecker;

import error.WrongMessageTypeException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;

/**
 * Created by xingchij on 11/20/15.
 */
public class Coordinator{

    private TaskReceiver gate;
    private WatcherGroup spies;

    public Coordinator() throws SocketException {
        gate = new TaskReceiver();
        spies = new WatcherGroup();
    }

    public void run(){
        Thread reception = new Thread(gate);
        reception.start();

        while(true){
            try {
                int identity = spies.watchForHeartBeat();

                spies.checkDead();

                if(identity == WatcherGroup.ID_PRIMARY){
                    // send task to primary when receive hearteat from primary
                    gate.sendTask(spies.getPrimary());
                }
            }catch (InterruptedIOException e){
                closeConnections();
                System.out.println("Coordinator terminated. All resources released");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (WrongMessageTypeException e) {
                e.printStackTrace();
            }
        }
    }
    public void closeConnections(){
        if(gate != null){
            gate.closeConnections();
        }
        if(spies != null){
            spies.closeConnections();
        }
    }

    public static void main(String[] args) throws SocketException {
        System.out.println("Coordinator now running...");
        Coordinator coordinator = new Coordinator();
        coordinator.run();
    }
}
