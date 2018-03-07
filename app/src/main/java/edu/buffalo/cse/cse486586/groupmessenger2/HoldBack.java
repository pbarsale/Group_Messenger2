package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by prati on 3/6/2018.
 */

public class HoldBack {

    String msgId;
    String msg;
    int proposed;
    String sender_port;
    boolean deliver;

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


    public int getProposed() {
        return proposed;
    }

    public void setProposed(int proposed) {
        this.proposed = proposed;
    }

    public String getSender_port() {
        return sender_port;
    }

    public void setSender_port(String sender_port) {
        this.sender_port = sender_port;
    }

    public boolean isDeliver() {
        return deliver;
    }

    public void setDeliver(boolean deliver) {
        this.deliver = deliver;
    }

    public HoldBack(String msg, String msgId, int proposed, String sender_port, boolean deliver) {
        this.msgId = msgId;
        this.msg = msg;
        this.proposed = proposed;
        this.sender_port = sender_port;
        this.deliver = deliver;
    }
}
