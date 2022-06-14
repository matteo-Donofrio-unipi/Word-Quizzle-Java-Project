
package wordquizzleserver;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.*;

public class WordQuizzleServer {

    //variabili per funzionamento server
    private final ExecutorService Tpool;
    private final ServerSocketChannel serverSocketChannel;
    //variabili per funzionamento handler
    public SocketChannel S;
    public ClientHandler handler;
    public Dbhandler DBH;
    //variabili per funzionamento sfide
    public ConcurrentHashMap<Integer, SocketChannel> SocketTcpMap = new ConcurrentHashMap <Integer,SocketChannel>();
    public ConcurrentHashMap<Integer, Integer> SocketUdpMap = new ConcurrentHashMap <Integer,Integer>();
    public ConcurrentHashMap<Integer, ClientHandler> ClientHandlerMap = new ConcurrentHashMap <Integer,ClientHandler>();
    public ConcurrentHashMap<String, Integer> ClientNameMap = new ConcurrentHashMap <String, Integer>(); //associa nome all'id del Client
    public ConcurrentHashMap<Integer, Integer> ChallengeHandlerMap = new ConcurrentHashMap <Integer,Integer>();
    
    private int nextId=0; //posizione usata per inserire nomi dentro ClientNameTable
    
    public WordQuizzleServer(int port, int poolSize) throws IOException{
        //crea e apre socket su cui si binda, su di esso attende e accetta connessioni dai client
        serverSocketChannel= ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(true);
        
        //threadPool che gestisce i diversi client
        Tpool= Executors.newFixedThreadPool(poolSize);
        System.out.println("Thread pool created");
        
        //istanzio le tabelle definite su
        DBH=Dbhandler.getIstance();       
        
    }
    
    private void addNextId(){
        nextId++;
    }
    
    public synchronized void LogoutClient(int ClientId){
        //quando un utente effettua il logout, si rimuovono i suoi dati dalle tabelle
        try{
            ClientNameMap.remove(ClientId);
            SocketTcpMap.remove(ClientId);
            SocketUdpMap.remove(ClientId);
            ClientHandlerMap.remove(ClientId);
            ChallengeHandlerMap.remove(ClientId);
        }catch(NullPointerException e){
            System.out.println("WQ: Error while deleting client num: "+ClientId);
        }
        
    }
    
    public synchronized int getIndexByName(String ClientName){
        //prende come parametro il nome del client connesso, e restituisce la posizione di tale client nelle tabelle
        int position=-1;
        if(ClientName!=null){
            try{
               position= ClientNameMap.get(ClientName);
            }catch(NullPointerException e){
                position=-1;
                System.out.println("WQ: error on getIndexByName");
            }
        }
        return position;
    }
    
    private void putSocketTCP(SocketChannel S){
        
        SocketTcpMap.put(nextId, S);
        
    }
    
    public synchronized boolean putSocketUDP(int ClientId, int port){
        //prende come parametro il nome del client e la sua porta UDP
        //inserendolo nella stessa posizione destinata al client
        boolean result;
        if(port>1024){
            try{
                SocketUdpMap.put(ClientId, port);
                result=true;
            }catch(NullPointerException e){
                result=false;
            }
            
        }
        else
            result=false;
        return result;
    } //synch perche fatta da thread 
    
    private void putClientHandler(ClientHandler CH){
        //invocato dal costruttore, prende come parametro il riferimento al ClientHandler
        //inserendolo nella stessa posizione destinata al client
        ClientHandlerMap.put(nextId, CH);
    }
    
    public synchronized boolean putClientName(int ClientId, String ClientName){
        //prende come parametro il riferimento al ClientHandler e il nome del Client relativo
        //inserendolo (il nome) nella stessa posizione destinata al client
        boolean result;
        if(ClientName!=null){
           try{
               ClientNameMap.put(ClientName, ClientId);
               result=true;
           }catch(NullPointerException e){
               result=false;
               System.out.println("WQ: error on putClientName");
           }
        }
        else
            result=false;
        return result;
    } //synch perche invocata da clientHandler
    
    public synchronized boolean checkClientOnline(String ClientName){
        //verifica la presenza o meno di un Client online
        boolean response=false;
        if(ClientName!=null){
            if(ClientNameMap.containsKey(ClientName))
                response=true;
        }
        return response;
    } //synch perche invocata da clientHandler
    
    public void WarningBeforeChallenge(String richiedente, String avversario){
        //metodo invocato da ClientHandler per esortare il ClientHandler avversario a mandare richiesta udp al client da lui gestito
        
        //cerco l'handler dell'avversario, e su di lui invoco il metodo 
        if(richiedente!=null && avversario!=null){
            int positionAvversario=getIndexByName(avversario);
            int positionRichiedente=getIndexByName(richiedente);
            
            //SEGNALO ALL'AVVERSARIO DI INVIARE MESSAGGIO A CLIENT IN UDP
            System.out.println("WQS WARNING BEFORE: pos avversario= "+positionAvversario+" pos richiedente= "+positionRichiedente);
            if(positionAvversario!=-1){
                ClientHandlerMap.get(positionAvversario).SetsendUdpRequestToClient(richiedente);
            }
            else
                System.out.println("WQS: error on calling SetSendUdpRequest ");
            
            //SETTO VALORI A 2 NELLA TABELLA DI INTERI (2=WAIT STATUS)
            synchronized(ChallengeHandlerMap){
                ChallengeHandlerMap.put(positionAvversario, 2);
                ChallengeHandlerMap.put(positionRichiedente, 2);
            }
            
        }
    } //permette comunicazione da ClientHandler che vuole sfidare a ClientHandler sfidato, esortandolo a inviare invito UDP al suo client
    
    public void RefuseChallenge(String richiedente, String avversario, int mode){
        //se ClientHandler sfidato rifiuta, tramite questo metodo lo comunica al ClientHandler sfidante
        //settando valore in ChallengeHandlerTable a | mode = -1 = rifiutata | mode = -2 = timer scaduto 
        int posRichiedente;
        int posAvversario;
        posRichiedente=this.getIndexByName(richiedente);
        posAvversario=this.getIndexByName(avversario);
        System.out.println("WQS REFUSE: pos avversario= "+posAvversario+" pos richiedente= "+posRichiedente);
        if(posRichiedente!=-1 && posAvversario!=-1){
            synchronized(ChallengeHandlerMap){
                ChallengeHandlerMap.replace(posRichiedente, mode);
                ChallengeHandlerMap.replace(posAvversario, mode);
                ChallengeHandlerMap.notify();
                ChallengeHandlerMap.notify();
            }
        }
        else
            System.out.println("WQS: error on RefuseChallenge caused by position");
    }
    
    public void AcceptChallenge(String richiedente, String avversario){
            //se ClientHandler sfidato accetta, tramite questo metodo lo comunica al ClientHandler sfidante
            //settando valore in ChallengeHandlerTable a | mode = 1 = accettata | 
        
            int posRichiedente=this.getIndexByName(richiedente);
            int posAvversario=this.getIndexByName(avversario);
            int ScoreRichiedente=0;
            int ScoreAvversario=0;
            
            //SETTO INTERI A 1 (sfida accettata) NELLA TABELLA
            synchronized(ChallengeHandlerMap){
                ChallengeHandlerMap.replace(posAvversario, 1);
                ChallengeHandlerMap.replace(posRichiedente, 1);
                ChallengeHandlerMap.notify();
                ChallengeHandlerMap.notify();
            }
            
            //CREO GESTORI SFIDA
            ChallengeHandler CH1= new ChallengeHandler (this, richiedente, SocketTcpMap.get(posRichiedente));
            ChallengeHandler CH2= new ChallengeHandler (this, avversario, SocketTcpMap.get(posAvversario));
            FutureTask<Integer> task1= new FutureTask<>(CH1);
            FutureTask<Integer> task2= new FutureTask<>(CH2);
            Thread t = new Thread(task1);
            Thread t2 = new Thread(task2);
        try {
            t.start();
            t2.start();
            
            //ottengo punteggio di ogni sfida
            ScoreRichiedente=task1.get();  
            ScoreAvversario=task2.get();
            
            if(ScoreRichiedente > ScoreAvversario ){
                ClientHandlerMap.get(posRichiedente).updateScore(1, ScoreRichiedente);//richiedente ha vinto
                ClientHandlerMap.get(posAvversario).updateScore(0,ScoreAvversario);//avversario ha perso
            }
            else if (ScoreRichiedente < ScoreAvversario){
                ClientHandlerMap.get(posRichiedente).updateScore(0, ScoreRichiedente);//richiedente ha vinto
                ClientHandlerMap.get(posAvversario).updateScore(1, ScoreAvversario);//avversario ha perso
            }
            else{ //pareggio
                ClientHandlerMap.get(posRichiedente).updateScore(2, ScoreRichiedente);//richiedente ha vinto
                ClientHandlerMap.get(posAvversario).updateScore(2, ScoreAvversario);//avversario ha perso
            }

            CH1=null;
            CH2=null;
            //RESETTO INTERI A 0 (default) NELLA TABELLA
            synchronized(ChallengeHandlerMap){
                ChallengeHandlerMap.replace(posRichiedente, 0);
                ChallengeHandlerMap.replace(posAvversario, 0);
                ChallengeHandlerMap.notify();
                ChallengeHandlerMap.notify();
                 //notify per i ClientHandler interessati, i quali erano in wait 
            } 
            System.out.println("WQS: game finished");
        } catch (InterruptedException ex) {
            System.out.println("WQS: error caused by interrupt");
        } catch (ExecutionException ex) {
            System.out.println("WQS: error on execution by future tasks");
        }
        
    }
    
    public void StartService(){
        while(true){
            try {

                S=serverSocketChannel.accept(); //accetto richiesta
                handler= new ClientHandler(S, this, nextId, this.ChallengeHandlerMap); //genero ClientHandler per quel Client
                this.putSocketTCP(S);
                this.putClientHandler(handler);
                addNextId(); //incremento contatore
                
                Tpool.execute(handler);//ogni nuova richiesta viene accettata e aperto un thread per essa passandogli ClientHandler 
                
                               
            } catch (IOException ex) {
                System.out.println("Word quizzle server: error while inserting new incoming client");
            }
        }
    }

    
}
