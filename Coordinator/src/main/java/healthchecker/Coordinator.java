package healthchecker;

import com.sun.javafx.tk.Toolkit;
import shared.ConnMetrics;
import shared.Distributer;

import java.io.IOException;
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
