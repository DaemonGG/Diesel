package message;

import java.io.Serializable;

/**
 * Created by xingchij on 11/17/15.
 */
public class Message implements Serializable{
    private String content;
    private int type;

    public String getContent(){
        return content;
    }
    public int getType(){
        return type;
    }

    public Message(int type, String content){
        this.type = type;
        this.content = content;
    }
}
