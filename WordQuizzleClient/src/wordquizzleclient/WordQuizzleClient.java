
package wordquizzleclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;


public class WordQuizzleClient {
    
    //variabili per cominicazione
    private SocketAddress address;
    private static int SERVERPORT = 9000;
    private SocketChannel client;
    //variabili per gestire interazione con server(specifiche)
    private boolean waitFriendsList=false;
    private boolean waitRankList=false;
    public boolean logoutRequest=false;
    public Thread t3;
    //variabili per sfida
    private boolean inviteArrived=false; //se true indica che deve passare il controllo alla classe udp
 
    public WordQuizzleClient() {
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
    
    //setter e getter per gestione ottenimento JSON quando richiedo lista amici e classifica (avverte il client che i dati che arriveranno saranno in formato JSON )
    public synchronized void setWaitFriendsList(boolean value){
        waitFriendsList=value;
    }
    
    public boolean getWaitFriendsList(){
        return waitFriendsList;
    }
    
    public synchronized void setWaitRankList(boolean value){
        waitRankList=value;
        
    }
    
    public boolean getWaitRankList(){
        return waitRankList;
    }
    
    public void Shutdown(){
        t3.interrupt();
    }
    
    
    //avvio client
    
    public void Interact(){
        ClientReceiver receiver = new ClientReceiver(client, this);
        Thread t = new Thread(receiver);
        t.start();
        
        ClientSender sender = new ClientSender(client, this, 4000);
        Thread t2 = new Thread(sender);
        t2.start();
        
        ClientReceiverUdp receivedUdp;
        try {
            receivedUdp = new ClientReceiverUdp(this, 4000);
            t3 = new Thread(receivedUdp);
            t3.start();
        } catch (SocketException ex) {
            System.out.println("WQC: error on creating udp listener");
        }
        
        
    }
    
}
