
package wordquizzleclient2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ClientReceiver2 implements Runnable  {

    private SocketChannel S;
    private WordQuizzleClient2 clientReference;
    
    public ClientReceiver2(SocketChannel S, WordQuizzleClient2 clientReference){
        //riceve il riferimento all'oggetto client, riferimento al socket cui scrivere
        this.S=S;
        this.clientReference=clientReference;
    }
    
    private String PrepareMessage(String received){
        //se riceve JSON analizza ed estrae risultati
                
       JSONParser parser = new JSONParser();
       JSONArray List = null; //classifica || lista amici in base alla modalità
       JSONObject Object; //oggetto per fare parsing
       JSONObject Single; //rappresenta il singolo utente nella classifica ottenuta 
       
       if(received!=null && !received.equals("Devi prima effettuare il login")){
           
           try {
               
               //CASO IN CUI ASPETTO LISTA AMICI
               if(clientReference.getWaitFriendsList()==true){
                   if(received.contains("No friends found")){
                      clientReference.setWaitFriendsList(false);//resetto booleano 
                   }
                   else{
                        //System.out.println("ClientReceiver: prepare to receive Friends list ");
                        Object = (JSONObject) parser.parse(received);
                        received="Lista amici: \n"; //preparo risposta
                        List = (JSONArray) Object.get("ListaAmici");

                        for(int i=0;i<List.size();i++) //scandisco e stampo la lista
                         received+=List.get(i)+", "; 

                        clientReference.setWaitFriendsList(false);//resetto booleano
                   }

               }
               //CASO IN CUI ASPETTO CLASSIFICA
               if(clientReference.getWaitRankList()==true){
                   //System.out.println("ClientReceiver: prepare to receive rank list ");
                 Object = (JSONObject) parser.parse(received);
                 received="Classifica: \n";  //preparo risposta  
                 List=(JSONArray) Object.get("RankList");//ottengo classifica
                 
                for(int i=0;i<List.size();i++){ // scandisco la lista
                   Single=(JSONObject) List.get(i); //prendo i singoli utenti nella classifica
                   received+=Single.get("name")+" : "+Single.get("score")+"\n"; //stampo nome e valore in ordine
                }
                
                clientReference.setWaitRankList(false); //resetto booleano
                
               }
               
           } catch (ParseException ex) {
               System.out.println("ClientReceiver: error on PrepareMessagge ");
           }
           return received;
       }
       else
           return received; //se non effettuo operazioni
    }
    
    public void run() {
        String received="";
        ByteBuffer input = ByteBuffer.allocate(1024);
        
        try {
            while(true){
                received="";
                
                
                input.clear();
                S.read(input);
                input.flip();
                while (input.hasRemaining()) {
                    received+=(char) input.get();
                }
                input.clear();                
                
                received=PrepareMessage(received);
                
                System.out.println("Server sent: "+received);
                
                if(this.clientReference.logoutRequest==true){
                    System.out.println("ClientReceiver: QUIT");
                    this.S.close();
                    break;
                }
                    
                

            }
        } catch (IOException ex) {
                System.out.println("ClientSender: error while communicating with server");
            }
        
    }
}
