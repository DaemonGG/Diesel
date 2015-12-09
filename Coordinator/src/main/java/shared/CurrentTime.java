package shared;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by xingchij on 12/1/15.
 */
public class CurrentTime {
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_GREEN = "\u001B[32m";

    public static String getTime(){
        SimpleDateFormat pattern = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        pattern.setTimeZone(TimeZone.getDefault());
        return pattern.format(new Date());
    }

    public static void tprintln(String s){
        System.out.println( ANSI_CYAN + getTime() + '\t' + ANSI_RESET +s);
    }
}
