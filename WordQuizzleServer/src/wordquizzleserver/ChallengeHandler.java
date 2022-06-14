
package wordquizzleserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;


public class ChallengeHandler implements Callable<Integer>{
    private WordQuizzleServer serverReference;
    private String NickNameClient;
    private ConcurrentHashMap<String, String> Dictionary;
    private TranslationService TS;
    private SocketChannel socket;
    private ByteBuffer output = ByteBuffer.allocate(1024);
    private ByteBuffer input = ByteBuffer.allocate(1024);
    String receivedMessage="";
    String sendedMessage="";
    private long Tstart=0;
    private long Tend=0;
    
    public ChallengeHandler(WordQuizzleServer serverReference, String NickNameClient, SocketChannel socket){
        //riceve il riferimento al server centrale, il nome del client a cui sottoporre la sfida, e il socketChannel per comunicare con esso (Client)
        this.NickNameClient=NickNameClient;
        this.serverReference=serverReference;
        this.socket=socket;
        this.TS=TranslationService.getIstance();
        
        Dictionary=TS.getWords();
    }
    
    public int CheckTrasnaltion(String receivedWord, String correctWord, int actualScore ){
        //analizza la correttezza o meno della parola inserita dal Client
        if(correctWord.contains(receivedWord))
            return actualScore+2; //se giusta +2
        else
            return actualScore-1; //se sbagliata -1
    }


    @Override
    public Integer call() {
        int score=0;
        Tstart=System.currentTimeMillis();//avvio timer
        for (Map.Entry<String,String> entry : Dictionary.entrySet()){
            try {
                receivedMessage="";
                sendedMessage="";
                
                //INVIO MESSAGGI
                sendedMessage=entry.getKey(); //mando parola in italiano
                output.clear();
                output.put(sendedMessage.getBytes());
                output.flip();
                while (output.hasRemaining()) {
                    socket.write(output);
                }
                
                Tend=System.currentTimeMillis(); //gestisco timer
                if((Tend-Tstart)>10000) 
                    break;
                
                //RICEVO
                input.clear();
                socket.read(input);
                input.flip();
                while (input.hasRemaining()) {
                    receivedMessage+=(char) input.get();
                }
                
                score=CheckTrasnaltion(receivedMessage, entry.getValue(), score); //verifico correttezza e modifico punteggio
                System.out.println("GESTORE ricevuto: "+receivedMessage+" richiesto: "+entry.getValue()+" score: "+score);
                Tend=System.currentTimeMillis(); //gestisco timer
                if((Tend-Tstart)>10000) 
                    break;
                
            } catch (IOException ex) {
                System.out.println("Error on CH while sending words ");
            }

        } 
        System.out.println("CH: di "+this.NickNameClient+" partita terminata con punti= "+score);
        return score;
    }
    
}
