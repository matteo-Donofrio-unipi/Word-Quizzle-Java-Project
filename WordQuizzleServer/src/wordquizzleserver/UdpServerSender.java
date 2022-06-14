
package wordquizzleserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class UdpServerSender implements Runnable{
    private int portServerUdp;
    private int portClientUdp;
    private String requestClientName;
    private byte[] sendBuff = new byte[128];
    
    public UdpServerSender( int portServerUdp, int portClientUdp , String requestClientName){
        //riceve in ingresso la porta UDP del server su cui si binda, la porta UDP del client a cui inviare richiesta, e il nome del Client sfidante
        this.portClientUdp=portClientUdp;
        this.portServerUdp=portServerUdp;
        this.requestClientName=requestClientName;
    }

    @Override
    public void run() {
       try {
            //invia la richiesta contenente il nome dello sfidante, in udp
            String invite=requestClientName+" ti ha lanciato una sfida, vuoi accettarla ? Rispondi si o no";
            DatagramSocket server = new  DatagramSocket(portServerUdp);
            sendBuff=invite.getBytes();
            DatagramPacket DatiDaInviare = new DatagramPacket(sendBuff,sendBuff.length, InetAddress.getByName("localhost"), portClientUdp);
            server.send(DatiDaInviare);//INVIO
            System.out.println("INVIATO: "+invite+" destinato a : "+portClientUdp);
            
            
            server.close();
            
            
        } catch (SocketException ex) {
            System.out.println("UdpServerSender: error while opening udp socket ");
        } catch (UnknownHostException ex) {
            System.out.println("UdpServerSender: error while retrieving Udp client socket");
        }   catch (IOException ex) {
            System.out.println("UdpServerSender: error while sending Udp request");
        }
    }
    
}
