
package wordquizzleclient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;


public class ClientReceiverUdp implements Runnable {
    private int udpPortClient;
    private int udpPortServer;
    private WordQuizzleClient clientReference;
    private DatagramSocket listener;
    private DatagramPacket receivedPacket;
    private byte[] bufferR = new byte[100];
    
    public ClientReceiverUdp(WordQuizzleClient clientReference, int udpPortClient) throws SocketException{ //riceve solo richieste udp e le stampa
        this.clientReference=clientReference;
        this.udpPortClient=udpPortClient;
        listener = new DatagramSocket(this.udpPortClient);
        receivedPacket = new DatagramPacket(bufferR, bufferR.length);
    }

    public void run() {
       String request="";
       System.out.println("ClientReceivedUDP online, waiting for requests");
       try {
                while(true){
                    request="";
                    listener.setSoTimeout(3000);
                    try{
                        listener.receive(receivedPacket); //ricevo messaggio
                        request = new String(receivedPacket.getData()); //otterngo contenuto
                        System.out.println("ServerUDP: "+request);
                    }catch(SocketTimeoutException e){
                       if(Thread.currentThread().isInterrupted())
                           break;
                       else
                           continue;
                    }
                                      
                }
                System.out.println("ServerUDP: shuttingDown");
                listener.close();
            }catch (IOException ex) {
                System.out.println("ClientReceiverUDP: error while retireving messege on socket udp");
            }
    }
    
    
    
    
}
