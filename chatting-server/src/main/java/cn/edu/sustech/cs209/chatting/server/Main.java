package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.IOException;
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
        while(true){
            try {
                Message message = (Message) in.readObject();
                if (message.getType() == 0){
                    Message userOnlineMessage = new Message();
                    String userListString = this.mainServer.clientPoor.keySet().toString();
                    userListString = userListString.substring(1,userListString.length()-1).replace(", ",",");
                    userOnlineMessage.setData(userListString);
                    System.out.println(userListString);
                    userOnlineMessage.setType(-1);
                    out.writeObject(userOnlineMessage);
//                    out.writeObject(new HashSet<String>(new ArrayList<>(this.mainServer.clientPoor.keySet())));
                    System.out.println(Thread.currentThread().getName()+" | 服务端收到客户端获取在线列表的信息：" + message.getData());
                }else if (message.getType() == 1){
                    synchronized(this){
                        this.setName(message.getSentByUser());
                        this.mainServer.clientPoor.put(message.getSentByUser(),this);
                        System.out.println(Thread.currentThread().getName()+" | 服务端收到客户端登陆成功的信息：" + message.getData());

                    }
                }else if (message.getType()== 2){
                    System.out.println(Thread.currentThread().getName()+" | 服务端收到客户端发给"+ message.getSendToUser() +"的信息：" + message.getData());
                    Message transferMessage = new Message(message);
                    transferMessage.setSendToUrl(this.mainServer.clientPoor.get(message.getSendToUser()).socket.getLocalAddress().toString());
                    this.mainServer.clientPoor.get(message.getSendToUser()).out.writeObject(transferMessage);

                }
//                System.out.println(Thread.currentThread().getName()+" | 服务端收到客户端发给"+ message.getSendToUser() +"的信息：" + message.getData());

            } catch (EOFException e){
                System.out.println("EOF e");
            } catch (IOException | ClassNotFoundException e ) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }




}

class MyServerSocket extends Thread {

    ServerSocket serverSocket;

    Map<String,ClientThread> clientPoor ;

    public MyServerSocket(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.clientPoor = new HashMap<>();
        //设置20s内无客户端连接，则抛出SocketTimeoutException异常
//        serverSocket.setSoTimeout();
    }

    public void run() {
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


