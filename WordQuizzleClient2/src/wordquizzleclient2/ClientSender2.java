/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wordquizzleclient2;




import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientSender2 implements Runnable {
    //variabili per funzionare
    private SocketChannel S;
    public int udpPort=4000;
    private WordQuizzleClient2 clientReference;
    //variabili per sfida
    
    public ClientSender2(SocketChannel S, WordQuizzleClient2 clientReference, int udpPort){
        this.S=S;
        this.clientReference= clientReference;
        this.udpPort=udpPort; //prende il parametro per inviarlo durante il login
    }
    
     private String HandleRegistration(String nickName, String password){ //invia richiseta e ottiene risposta dal RMI
        wordquizzleserver.IntRegistration serverObject;
        Remote remoteObject;
        String response="KO 303";
        try {
            //mi collego e ottengo il riferimento
            Registry r = LocateRegistry.getRegistry(9200);
            remoteObject = r.lookup("Registration-Server");
            serverObject = (wordquizzleserver.IntRegistration) remoteObject;
            //invio richiesta registrazione nuovo utente
            response=serverObject.RegisterNewUser(nickName,password);
            
        } catch (RemoteException ex) {
           System.out.println("ClientSender: error while registrating");
        } catch (NotBoundException ex) {
            System.out.println("ClientSender: error while registrating");
        }
        return response;
    }//gestisce la richiesta e interazione con il servizio RMI
     
     private String PrepareMessage(String ToBeSent){
         String [] splitted = ToBeSent.split(" ");
         
        if(splitted.length==3 && splitted[0].equals("login")){ //aggiungo il parametro porta udp
             ToBeSent+=" "+udpPort;
        }
        
        if(splitted.length==1 && splitted[0].equals("lista_amici")){
             clientReference.setWaitFriendsList(true);
        }
        
        if(splitted.length==1 && splitted[0].equals("mostra_classifica")){
             clientReference.setWaitRankList(true);
        }

        
        
        
        return ToBeSent; 
     } //gestisce oprazioni da fare prima di inviare il messaggio, in funzione del suo contenuto
    
    
    public void run() {
        String ToBeSent="";
        String [] splitted;
        ByteBuffer output = ByteBuffer.allocate(1024);
        InputStreamReader ingresso= new InputStreamReader(System.in);
        BufferedReader in=new BufferedReader(ingresso);
        try {
            while(true){
                ToBeSent="";
                
                ToBeSent=in.readLine();
                
                //ANALIZZO E GESTISCO IN CASO REGISTRAZIONE RICHIESTA
                splitted=ToBeSent.split(" ");
                if(splitted.length==3 && splitted[0].equals("registra_utente")){ 
                    String response="";
                    response=HandleRegistration(splitted[1],splitted[2]);
                    System.out.println(response); //stampo contenuto inviato dal server
                    continue;
                }
                
                ToBeSent=PrepareMessage(ToBeSent);
                
                output.clear();
                output.put(ToBeSent.getBytes());
                output.flip();
                while (output.hasRemaining()) {
                    S.write(output);
                }
                
                if(ToBeSent.equals("logout")){
                    this.clientReference.Shutdown();
                    this.clientReference.logoutRequest=true;
                    break;
                }

            }
            return;
        } catch (IOException ex) {
                System.out.println("ClientSender: error while communicating with server");
            }
        
    }
    
}
