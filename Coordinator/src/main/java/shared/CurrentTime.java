package shared;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by xingchij on 12/1/15.
 */
public class CurrentTime {
    public static String getTime(){
        SimpleDateFormat pattern = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        pattern.setTimeZone(TimeZone.getDefault());
        return pattern.format(new Date());
    }

    public static void tprintln(String s){
        System.out.println(getTime() + '\t' + s);
    }
}
