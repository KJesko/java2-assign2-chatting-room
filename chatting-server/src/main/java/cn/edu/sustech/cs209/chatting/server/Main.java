package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

class ClientThread extends Thread{
    private MyServerSocket mainServer;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientThread(MyServerSocket mainServer, Socket socket) throws IOException {
        this.socket = socket;
        this.mainServer = mainServer;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }
    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public ObjectInputStream getIn() {
        return in;
    }

    public void setIn(ObjectInputStream in) {
        this.in = in;
    }

    public ObjectOutputStream getOut() {
        return out;
    }

    public void setOut(ObjectOutputStream out) {
        this.out = out;
    }

    @Override
    public void run(){
        while(!currentThread().isInterrupted()){
            try {
                Message message = (Message) in.readObject();
                if (message.getType() == 1){ //收到注册消息
                    synchronized (this.mainServer.Authentication){
                        System.out.println(Thread.currentThread().getName()+" | 服务端收到客户端注册新用户的信息：" + message.getData());
                        String[] namePwd = message.getData().split(",",2);
                        Message returnMessage = new Message();
                        returnMessage.setSendToUser(message.getSentByUser());
                        returnMessage.setSentByUser("server");
                        returnMessage.setType(505);
                        if (namePwd.length !=2){
                            returnMessage.setData("register fail: unknown wrong.");
                        }else{
                            String username = namePwd[0];
                            String pwd = namePwd[1];

                            if (this.mainServer.Authentication.containsKey(username)){
                                returnMessage.setData("register fail: exist user.");
                            }else {
                                this.mainServer.Authentication.put(username,pwd);
                                returnMessage.setType(-1);
                                returnMessage.setData("register successful");
                            }
                        }

                        out.writeObject(returnMessage);
                        System.out.println(returnMessage);

                    }

//                    out.writeObject(new HashSet<String>(new ArrayList<>(this.mainServer.clientPoor.keySet())));
                }
                else if (message.getType() == 2) {//用户登陆时，1.验证身份 2.在线用户
                    System.out.println(Thread.currentThread().getName()+" | 服务端收到客户端登陆身份验证的信息：" + message.getData());
                    synchronized (this.mainServer.Authentication){
                        String[] namePwd = message.getData().split(",",2);
                        Message returnMessage = new Message();
                        returnMessage.setSendToUser(message.getSentByUser());
                        returnMessage.setSentByUser("server");

                        returnMessage.setType(404);
                        if (namePwd.length != 2){
                            returnMessage.setData("login fail: unknown wrong.");
                        } else{
                            String username = namePwd[0];
                            String pwd = namePwd[1];
                            if (!this.mainServer.Authentication.containsKey(username)){
                                returnMessage.setData("login fail: nonexistent user");
                            } else if (!this.mainServer.Authentication.get(username).equals(pwd)) {
                                returnMessage.setData("login fail: wrong password.");
                            } else if (this.mainServer.clientPoor.containsKey(username)) {
                                returnMessage.setData("login fail: The user is online.");
                            }else {//成功
//                                this.mainServer.Authentication.put(username,pwd);
                                returnMessage.setSendToUser(message.getSentByUser());
                                returnMessage.setType(-2);
                                returnMessage.setData("login successful");
                                synchronized (this.mainServer.clientPoor){//type=-2，登陆成功时，直接在data里放 在线用户列表
                                    this.mainServer.clientPoor.put(message.getSentByUser(),this);
                                }
                                sendBroadcastMessage();
                                out.writeObject(returnMessage);
                                System.out.println(returnMessage);
                                for (Message mess : this.mainServer.HistoryMessage){//登陆成功时，向client发送和他有关的历史记录
                                    if (mess.getBelongToUser().equals(username)){
                                        out.writeObject(mess);
                                    }
                                }
                                continue;

                            }
                        }
                        out.writeObject(returnMessage);
                        System.out.println(returnMessage);
                    }


                }
                else if (message.getType() == 3){//更换头像时，向所有和当前user有对话的窗口的人发refresh

                    for (String user: message.getData().split(",")){
                        Message returnMessage = new Message();
                        returnMessage.setType(-3);
                        returnMessage.setSendToUser(user);
                        this.mainServer.clientPoor.get(user).out.writeObject(returnMessage);
                    }
                    System.out.println(Thread.currentThread().getName()+" | 服务端收到客户端refresh的信息：" + message.getData());
                }
                else if (message.getType() == 4) {//退出
                    System.out.println(Thread.currentThread().getName()+" | 服务端收到客户端退出的信息：" + message.getData());

                    Thread.currentThread().interrupt();
                    synchronized (this.mainServer.clientPoor){
                        if (message.getSentByUser()==null){
                            this.mainServer.clientPoor.remove(Thread.currentThread().getName());
                        }else {
                            this.mainServer.clientPoor.remove(message.getSentByUser());
                        }

                    }

                    Message returnMessage = new Message();
                    returnMessage.setType(-4);
                    returnMessage.setTimestamp(System.currentTimeMillis());
                    returnMessage.setSentByUser("server");
                    returnMessage.setSendToUser(message.getSentByUser());
                    returnMessage.setData("confirm disconnect");
                    out.writeObject(returnMessage);
                    sendBroadcastMessage();

                }
                else if (message.getType() == 6){//用户发给用户的文件消息，服务器负责转发
                    System.out.println(Thread.currentThread().getName()+" | 服务端收到客户端发给"+ message.getSendToUser() +"的文件：" + message.getData());
                    Message transferMessage = new Message(message);
                    transferMessage.setType(6);
                    this.mainServer.HistoryMessage.add(message);
//                    transferMessage.setSendToUrl(this.mainServer.clientPoor.get(message.getSendToUser()).socket.getLocalAddress().toString());
                    if (!message.getSendToUser().contains(",")){//发给单人用户，只要转发一个人
                        transferMessage.setBelongToChat(message.getSentByUser());
                        transferMessage.setBelongToUser(message.getSendToUser());
                        if (this.mainServer.clientPoor.get(message.getSendToUser()) != null){
                            this.mainServer.clientPoor.get(message.getSendToUser()).out.writeObject(transferMessage);
                        }
                        this.mainServer.HistoryMessage.add(transferMessage);
                    }else {//群聊消息，转发给多个人
                        String[] sendToUserArr = message.getSendToUser().split(",");
                        System.out.println("sendToUserArr:" + Arrays.toString(sendToUserArr));

                        for (String targetUser :sendToUserArr){
                            List<String> list = Arrays.stream(sendToUserArr).collect(Collectors.toList());
                            list.remove(targetUser);
                            list.add(message.getSentByUser());

                            String s =list.stream().sorted().collect(Collectors.toList()).toString();
                            s = s.substring(1,s.length()-1).replace(", ",",");
                            transferMessage.setBelongToChat(s);
                            transferMessage.setSendToUser(targetUser);
                            if (this.mainServer.clientPoor.get(targetUser) != null){
                                this.mainServer.clientPoor.get(targetUser).out.writeObject(transferMessage);
                            }
                            transferMessage.setBelongToUser(targetUser);
                            this.mainServer.HistoryMessage.add(transferMessage);
                        }
                    }

                }
                else if (message.getType()== 0){ //用户发给用户的文字消息，服务器负责转发
                    System.out.println(Thread.currentThread().getName()+" | 服务端收到客户端发给"+ message.getSendToUser() +"的信息：" + message.getData());
                    Message transferMessage = new Message(message);
                    this.mainServer.HistoryMessage.add(message);
//                    transferMessage.setSendToUrl(this.mainServer.clientPoor.get(message.getSendToUser()).socket.getLocalAddress().toString());
                    if (!message.getSendToUser().contains(",")){//发给单人用户，只要转发一个人
                        transferMessage.setBelongToChat(message.getSentByUser());
                        transferMessage.setBelongToUser(message.getSendToUser());
                        if (this.mainServer.clientPoor.get(message.getSendToUser()) != null){
                            this.mainServer.clientPoor.get(message.getSendToUser()).out.writeObject(transferMessage);
                        }
                        this.mainServer.HistoryMessage.add(transferMessage);
                    }else {//群聊消息，转发给多个人
                        String[] sendToUserArr = message.getSendToUser().split(",");
                        System.out.println("sendToUserArr:" + Arrays.toString(sendToUserArr));

                        for (String targetUser :sendToUserArr){
                            List<String> list = Arrays.stream(sendToUserArr).collect(Collectors.toList());
                            list.remove(targetUser);
                            list.add(message.getSentByUser());

                            String s =list.stream().sorted().collect(Collectors.toList()).toString();
                            s = s.substring(1,s.length()-1).replace(", ",",");
                            transferMessage.setBelongToChat(s);
                            transferMessage.setSendToUser(targetUser);
                            if (this.mainServer.clientPoor.get(targetUser) != null){
                                this.mainServer.clientPoor.get(targetUser).out.writeObject(transferMessage);
                            }
                            transferMessage.setBelongToUser(targetUser);
                            this.mainServer.HistoryMessage.add(transferMessage);
                        }
                    }

                }
//                System.out.println(Thread.currentThread().getName()+" | 服务端收到客户端发给"+ message.getSendToUser() +"的信息：" + message.getData());

            }catch(SocketException e){
                this.mainServer.clientPoor.remove(Thread.currentThread().getName());
            }
            catch (EOFException e){
                System.out.println("EOF e");
            } catch (IOException | ClassNotFoundException e ) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    public void sendBroadcastMessage() throws IOException {
        Message broadcastMessage = new Message();
        String userListString = this.mainServer.clientPoor.keySet().stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new)).toString();
        userListString = userListString.substring(1,userListString.length()-1).replace(", ",",");
        broadcastMessage.setData(userListString);
        broadcastMessage.setType(-5);
        broadcastMessage.setSendToUser("ALL USER");
        broadcastMessage.setSentByUser("server");

        for (ClientThread clientThread : this.mainServer.clientPoor.values()){
//            if (clientThread != currentThread()){
//                clientThread.out.writeObject(broadcastMessage);
//            }
            clientThread.out.writeObject(broadcastMessage);
        }
    }




}

class MyServerSocket extends Thread {

    ServerSocket serverSocket;

    Map<String,ClientThread> clientPoor;

    Map<String,String> Authentication;

    List<Message> HistoryMessage;

    Connection connection;
    public MyServerSocket(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.clientPoor = new HashMap<>();
        //设置20s内无客户端连接，则抛出SocketTimeoutException异常
//        serverSocket.setSoTimeout();
    }

    public void run() {
        String path1 = "chatting-server/src/main/resources/Authentication.txt";
        String path2 = "chatting-server/src/main/resources/HistoryMessage.txt";
        Thread shutdownHook = new Thread(() -> {//关闭主线程之前，我要先保存所有的身份信息和历史记录
            try {
                FileOutputStream fos1 = new FileOutputStream(path1,false);
                ObjectOutputStream oos1 = new ObjectOutputStream(fos1);
                oos1.writeObject(this.Authentication);
                oos1.close();
                fos1.close();
                FileOutputStream fos2 = new FileOutputStream(path2,false);
                ObjectOutputStream oos2 = new ObjectOutputStream(fos2);
                oos2.writeObject(this.HistoryMessage);
                oos2.close();
                fos2.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

//            System.out.println("Map has been written to file: " + filename);
            System.out.println("Authentication has been written to file1: "+ this.Authentication);
            System.out.println("HistoryMessage has been written to file2: "+ this.HistoryMessage);
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        File file1 = new File(path1);
        File file2 = new File(path2);

        try {
            if (!file1.exists()){
                boolean created1 = file1.createNewFile();
                this.Authentication = new HashMap<>();
            }
            if (!file2.exists()){
                boolean created2 = file2.createNewFile();
                this.HistoryMessage = new ArrayList<>();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            FileInputStream fis = new FileInputStream(path1);
            ObjectInputStream ois = new ObjectInputStream(fis);
            this.Authentication = (Map<String, String>) ois.readObject();
            ois.close();
            fis.close();
//            System.out.println( + filename);
            System.out.println("Authentication has been read from file: "+this.Authentication);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            this.Authentication = new HashMap<>();

        }

        try {
            FileInputStream fis = new FileInputStream(path2);
            ObjectInputStream ois = new ObjectInputStream(fis);
            this.HistoryMessage = (List<Message>) ois.readObject();
            ois.close();
            fis.close();
//            System.out.println( + filename);
            System.out.println("HistoryMessage has been read from file.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            this.HistoryMessage = new ArrayList<>();

        }



        System.out.println(Thread.currentThread().getName()+" | 服务端启动中...对应的端口号为：" + serverSocket.getLocalPort());
        while (true){

            //监听来自客户端的消息
            try {
                Socket socket = serverSocket.accept();
                System.out.println("服务端监听到客户端连接：" + socket.getRemoteSocketAddress());
                ClientThread clientThread = new ClientThread(this,socket);
                clientThread.start();

            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);

            }
            //通过socket向客户端发送信
        }


    }
}


public class Main {

    public static void main(String[] args) throws IOException {
        MyServerSocket myServerSocket = new MyServerSocket(9090);
        myServerSocket.start();
        System.out.println("Starting server");
    }
}


