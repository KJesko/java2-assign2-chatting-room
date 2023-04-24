package cn.edu.sustech.cs209.chatting.common;

//import jdk.internal.org.objectweb.asm.Handle;

import java.io.Serializable;

public class Message implements Serializable {
    private int type;
    private Long timestamp;

    private String sentByUser;

    private String sendToUser;

    private String sentByUrl;

    private String sendToUrl;

    private String data;

    private String belongToChat;

    private String belongToUser;

    public Message(int type, Long timestamp, String sentByUser, String sendToUser, String sentByUrl, String sendToUrl, String data) {
        this.type = type;
        this.timestamp = timestamp;
        this.sentByUser = sentByUser;
        this.sendToUser = sendToUser;
        this.sentByUrl = sentByUrl;
        this.sendToUrl = sendToUrl;
        this.data = data;
    }

    public Message(Message message) {
        this.type = 0;
        this.timestamp = message.timestamp;
        this.sentByUser = message.sentByUser;
        this.sendToUser = message.sendToUser;
        //转发过来的消息的目的地址message.sendToUrl为服务器，转发出去的发送地址sentByUrl也是服务器地址
        this.sentByUrl = message.sendToUrl;
        this.sendToUrl = "unknown";
        this.data = message.data;

    }

    public Message() {

    }


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setSentByUser(String sentByUser) {
        this.sentByUser = sentByUser;
    }

    public void setSendToUser(String sendToUser) {
        this.sendToUser = sendToUser;
    }

    public void setSentByUrl(String sentByUrl) {
        this.sentByUrl = sentByUrl;
    }

    public void setSendToUrl(String sendToUrl) {
        this.sendToUrl = sendToUrl;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSentByUser() {
        return sentByUser;
    }

    public String getSendToUser() {
        return sendToUser;
    }

    public String getSentByUrl() {
        return sentByUrl;
    }

    public String getSendToUrl() {
        return sendToUrl;
    }

    public String getData() {
        return data;
    }

    public String getBelongToChat() {
        return belongToChat;
    }

    public void setBelongToChat(String belongToChat) {
        this.belongToChat = belongToChat;
    }

    public String getBelongToUser() {
        return belongToUser;
    }

    public void setBelongToUser(String belongToUser) {
        this.belongToUser = belongToUser;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", timestamp=" + timestamp +
                ", sentByUser='" + sentByUser + '\'' +
                ", sendToUser='" + sendToUser + '\'' +
                ", sentByUrl='" + sentByUrl + '\'' +
                ", sendToUrl='" + sendToUrl + '\'' +
                ", data='" + data + '\'' +
                ", belongToChat='" + belongToChat + '\'' +
                '}';
    }
}
