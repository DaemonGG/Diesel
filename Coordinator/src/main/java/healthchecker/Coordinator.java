package healthchecker;

import shared.Distributer;

/**
 * Created by xingchij on 11/20/15.
 */
public class Coordinator extends Distributer {
    @Override
    public void serve() {

    }

    @Override
    public boolean receiveMemberShipMes() {
        return false;
    }

    public void closeConnections() {

    }
}
