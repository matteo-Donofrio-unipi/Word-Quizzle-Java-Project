/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wordquizzleclient2;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;


public class WordQuizzleClient2 {
    
    //variabili per cominicazione
    private SocketAddress address;
    private static int SERVERPORT = 9000;
    private SocketChannel client;
    //variabili per gestire interazione con server(specifiche)
    private boolean waitFriendsList=false;
    private boolean waitRankList=false;
    public boolean logoutRequest=false;
    //variabili per sfida
    private boolean inviteArrived=false; //se true indica che deve passare il controllo alla classe udp
    private Thread t3;
    public WordQuizzleClient2() {
        try {
            address = new InetSocketAddress("localhost",SERVERPORT);
            client = SocketChannel.open();
            client.configureBlocking(true);
            client.connect(address);
            
            //mi connetto al server
            while(!client.finishConnect()){}
            System.out.println("client connected");
            
            
        } catch (IOException ex) {
            System.out.println("WordQuizzleClient: error while connecting with server");
        }
    }
    
    //setter e getter per gestione ottenimento JSON quando richiedo lista amici e classifica
    public synchronized void setWaitRankList(boolean value){
        waitRankList=value;
    }
    
    public boolean getWaitFriendsList(){
        return waitFriendsList;
    }
    
    public synchronized void setWaitFriendsList(boolean value){
        waitFriendsList=value;
    }
    
    public boolean getWaitRankList(){
        return waitRankList;
    }
    
    public void Shutdown(){
        t3.interrupt();
    }
    
    //interazione
    
    public void Interact(){
        ClientReceiver2 receiver = new ClientReceiver2(client, this);
        Thread t = new Thread(receiver);
        t.start();
        
        ClientSender2 sender = new ClientSender2(client, this, 4001);
        Thread t2 = new Thread(sender);
        t2.start();
        
        ClientReceiverUdp2 receivedUdp;
        try {
            receivedUdp = new ClientReceiverUdp2(this, 4001);
            t3 = new Thread(receivedUdp);
            t3.start();
        } catch (SocketException ex) {
            System.out.println("WQC: error on creating udp listener");
        }
        
    }
    
}
