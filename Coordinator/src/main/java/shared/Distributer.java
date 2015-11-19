package shared;

/**
 * Created by xingchij on 11/19/15.
 */
public abstract class Distributer implements ConnMetrics {
    public abstract void serve() ;
    public AllSlaves slaveOffice;
    public AllSecondaries backUps;
}
