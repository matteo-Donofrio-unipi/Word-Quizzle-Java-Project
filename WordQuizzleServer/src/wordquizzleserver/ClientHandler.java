
package wordquizzleserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import static java.net.SocketOptions.SO_TIMEOUT;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class ClientHandler implements Runnable{

    //variabili per funzionare
    private final SocketChannel socket;
    private WordQuizzleServer serverReference;
    private ByteBuffer output = ByteBuffer.allocate(1024);
    private ByteBuffer input = ByteBuffer.allocate(1024);
    private String received="";
    private Dbhandler DBH;
    private TranslationService TS;
    private int myId;
    //variabili PER INTERAZIONE CON CLIENT
    private String ToBeSent="";
    private int udpPort;
    public String nickNameClient="";
    private  boolean loginDone=false;
    private  boolean logoutRequested=false;
    //variabili per sfida
    private ConcurrentHashMap<Integer, Integer> ChallengeHandlerMap;
    public boolean sendUdpRequestToClient=false; //usato se vengo invitato
    public String ChallengeOpponentName=""; //usato se vengo invitato
    private boolean waitForNewsAboutChallenge=false; //se ho invitato, setto qeusta a true e aspetto finche il server non modifica il valore di outcome
    private long Tstart=0;
    private long Tend=0;
    

    
    public ClientHandler (SocketChannel socket, WordQuizzleServer serverReference, int myId, ConcurrentHashMap<Integer, Integer> ChallengeHandlerMap) throws IOException{
        //riceve il socketChannel con cui comunicare con Client, il riferimento al server centrale, e il riferimento alla tabella dei ClientHandler(posseduta dal server centrale)
        this.socket=socket;
        this.serverReference=serverReference;
        this.DBH=Dbhandler.getIstance();
        this.TS=TranslationService.getIstance();
        this.myId=myId;
        this.ChallengeHandlerMap=ChallengeHandlerMap;
    }
    

    private String HandleLogin(String received){
      //quando Client fa login, verifica sul DB e comunicando con server centrale, in caso affermativo salva le credenziali
      String [] splitted=received.split(" ");
      
      JSONParser parser = new JSONParser();
      JSONArray amici = null;
      JSONObject User;
      String contentOfFile="";
      

      String password="";
      String UdpPort="";
      String response="KO missing arguments";  
      boolean verify1=false;
      boolean verify2=false;
      if(splitted.length==4){
          
        this.nickNameClient=splitted[1];
        password=splitted[2];
        UdpPort=splitted[3];
        this.udpPort=Integer.parseInt(splitted[3]); //mi prendo il valore della porta
            
        //accedo e leggo dati
        contentOfFile=DBH.ReadFromDB();    
        if(contentOfFile!=null){
            try {
                JSONObject jsonObject = (JSONObject) parser.parse(contentOfFile); //DB completo
                JSONArray DB = (JSONArray) jsonObject.get("DataBase"); //Db castato ad array 
                for(int i=0;i<DB.size();i++){
                    
                     User=(JSONObject) DB.get(i);
                     
                     if(this.nickNameClient.equals((String)(User.get("nickUtente"))) && password.equals((String)(User.get("password")))){
                         this.loginDone=true;
                         verify1=serverReference.putClientName(this.myId, this.nickNameClient); //prima aggiungo nome in base a posizione del mio Ch
                         verify2=serverReference.putSocketUDP(this.myId, Integer.parseInt(UdpPort)); //aggiungo porta udp del Client in base a posizione nome
                         if((verify1&&verify2))
                             response="OK, benvenuto "+this.nickNameClient;
                         else
                             response="Error during login ";
                         break;
                     }       
                }
            
            } catch (ParseException ex) {
                System.out.println("CLientHandler: error on Handle login, parsing JSON");
            }
        
        }
               
      }
          System.out.println("CLientHandler: HANDLELOGIN DICE: "+(verify1&&verify2)); 
          
          return response;
    }
    
    private void HandleLogout(){
        //rimuove riferimenti sul server centrale che gestivano tale Client e ClientHandler
        this.serverReference.LogoutClient(this.myId);
    }
    
    private String HandleFriendsList(){
        try {
            //variabili per scorrere DataBase
            JSONParser parser = new JSONParser();
            JSONArray amici = null;
            JSONObject User;
            JSONObject ObjectToReturn = null; //JSON object contenente la lista amici           
            String contentOfFile="";
            String response="";
            contentOfFile=DBH.ReadFromDB();
            JSONObject jsonObject = (JSONObject) parser.parse(contentOfFile); //DB completo
            JSONArray DB = (JSONArray) jsonObject.get("DataBase"); //Db castato ad array
            
            for(int i=0;i<DB.size();i++){
                
                User=(JSONObject) DB.get(i);
                if(this.nickNameClient.equals((String)(User.get("nickUtente"))) ){ //accedo alla mia lista amici
                    amici=((JSONArray)((JSONObject)DB.get(i)).get("Amici")); //acquisisco la lista amici 
                    ObjectToReturn = new JSONObject();                  // creo nuovo oggetto JSON da restituire
                    ObjectToReturn.put("ListaAmici", amici);                //ci inserisco la lista amici
                }       
            }
            if(amici.size()>0)
                return ObjectToReturn.toJSONString();
            else
                return "No friends found";          
  
        } catch (ParseException ex) {
            System.out.println("ClientHandler: error parsing file on handleFriendsList");
            return "KO 300";
        }
    }//calcola e restituisce la lista amici in JSON
    
    private boolean HandleAddFriend(String received){
        String response="";
        String [] splitted=received.split(" ");
        boolean completed1=false; //newFriend aggiunto alla lista di nickName
        boolean completed2=false; //nickName aggiunto alla lista di newFriend
        boolean completedBoth=false; //aggiunti reciprocamente
        boolean alreadyFriends=false; //gia amici, non faccio niente
        boolean newFriendExists = false; //verifica se newFriend esiste
        
        if(splitted.length==2){
            String newFriend=splitted[1];
            if(newFriend!=null){
                try {
                    //accedo al db 

                    //variabili per scorrere DataBase
                    JSONParser parser = new JSONParser();
                    JSONArray amici = null;
                    JSONObject User;
                    String contentOfFile="";

                    //accedo e leggo dati
                    contentOfFile=DBH.ReadFromDB();
                    System.out.println("CONTENTS addFriendOnDB: "+contentOfFile);
                    JSONObject jsonObject = (JSONObject) parser.parse(contentOfFile); //DB completo
                    JSONArray DB = (JSONArray) jsonObject.get("DataBase"); //Db castato ad array

                    for(int i=0;i<DB.size();i++){ //verifico se esiste e se siamo gia amici
                        User=(JSONObject) DB.get(i);
                        if(newFriend.equals((String)(User.get("nickUtente")))){ //newFriend esiste ed è registrato
                            newFriendExists=true;
                            amici=((JSONArray)((JSONObject)DB.get(i)).get("Amici")); //accedo agli amici di newFirend e controllo che non sia gia amico di nickName(client richiedente)
                            for(int j=0;j<amici.size();j++)
                                if(this.nickNameClient.equals((String)amici.get(j))){ // nickName e newFriend sono gia amici, non faccio nulla
                                    alreadyFriends=true;
                                    break;
                                }
                        }       
                    }
                    
                    if(!alreadyFriends && newFriendExists){  //condizioni tali da aggiungere amico
                        for(int i=0;i<DB.size();i++){
                            User=(JSONObject) DB.get(i);
                            if(this.nickNameClient.equals((String)(User.get("nickUtente")))){ //se e' l'utente richiedente loggato
                                amici=((JSONArray)((JSONObject)DB.get(i)).get("Amici"));
                                amici.add(newFriend); //aggiungo newFriend agli amici di nickName
                                completed1=true;
                                System.out.println("Clienthandler HandleAddFriend 1 SAYS: "+completed1);
                            }
                            if(newFriend.equals((String)(User.get("nickUtente")))){ //se e' l'amico da aggiungere
                                amici=((JSONArray)((JSONObject)DB.get(i)).get("Amici")); 
                                amici.add(this.nickNameClient); //aggiungo nickName agli amici di newFriend 
                                completed2=true;
                                System.out.println("Clienthandler HandleAddFriend 2 SAYS: "+completed2);
                            }
                        }
                    }
                    completedBoth=completed1&&completed2;
                    System.out.println("Clienthandler HandleAddFriend SAYS: "+completedBoth);
                    if(alreadyFriends)
                        System.out.println("Clienthandler HandleAddFriend SAYS: Already Friends");
                    if(!newFriendExists)
                        System.out.println("Clienthandler HandleAddFriend SAYS: "+newFriend+" doesen't exist");
                    if(completedBoth) //se ho aggiunto amicizie, le salvo sul db
                        DBH.WriteToDB(jsonObject.toString());

                    //gestione errori
                }catch (ParseException ex) {
                    System.out.println("error on parsing json on AddFriendOnDB");
                }
            }
        
        }
        return completedBoth;
    }
    
    
    private String HandleShowScore(String nickNameChosen){ //se invocata da user -> nickNameChosen=nickNameClient. se invocata da HandleShowRank -> nickNameChosen= nickName valutato nello scorrimento amici
        String response="";
        try {
            //variabili per scorrere DataBase
            JSONParser parser = new JSONParser();
            JSONArray amici = null;
            JSONObject User;
            String contentOfFile="";
            boolean correctCheck=false;
            
            //accedo e leggo dati
            contentOfFile=DBH.ReadFromDB();
           // System.out.println("CONTENTS: "+contentOfFile);
            JSONObject jsonObject = (JSONObject) parser.parse(contentOfFile); //DB completo
            JSONArray DB = (JSONArray) jsonObject.get("DataBase"); //Db castato ad array
            
            for(int i=0;i<DB.size();i++){
                User=(JSONObject) DB.get(i);
                if(nickNameChosen.equals((String)(User.get("nickUtente")))){
                    response=(String)User.get("PunteggioUtente"); //accedo e prendo punteggio utente
                    correctCheck=true;
                    break;
                }       
            }
            System.out.println("ClientHandler HandleShowScore DICE: "+correctCheck);
        } catch (ParseException ex) {
            System.out.println("ClientHandler HandleShowScore error while parsing json ");
        }
        
        return response;
    }//calcola e restituisce il punteggio utente
     
    private String HandleShowRank(){ //calcola e restituisce la classifica
        JSONObject response = null;//JSON restituito contenente classifica
        JSONArray RankList=null;
        JSONObject Single=null;
        try {
            //variabili per scorrere DataBase
            JSONParser parser = new JSONParser();
            JSONArray amici = null;
            JSONObject User;
            String contentOfFile="";
            boolean correctCheck=false;
            int dimRankList;
            //variabili per generare JSON risposta
            HashMap <String, Integer> Rank = new HashMap<String, Integer>();
            String userName;
            int score;
            
            //accedo e leggo dati
            contentOfFile=DBH.ReadFromDB();
            System.out.println("CONTENTS: "+contentOfFile);
            JSONObject jsonObject = (JSONObject) parser.parse(contentOfFile); //DB completo
            JSONArray DB = (JSONArray) jsonObject.get("DataBase"); //Db castato ad array
            
            for(int i=0;i<DB.size();i++){
                User=(JSONObject) DB.get(i);
                if(this.nickNameClient.equals((String)(User.get("nickUtente"))) ){
                    amici=((JSONArray)((JSONObject)DB.get(i)).get("Amici")); //ottengo lista amici 
                    int j;
                    
                    for(j=0;j<amici.size();j++){ //Per ogni elemento dell'array amici
                        userName=(String)amici.get(j); //inserisco i nomi degli amici
                        try{
                        score=Integer.parseInt(HandleShowScore((String)amici.get(j)));
                        }catch(NumberFormatException e){
                            score=0;
                        }
                        Rank.put(userName, score);
                    }
                    
                    userName=this.nickNameClient; //aggiungo me stesso nell'ultima riga
                    try{
                        score=Integer.parseInt(HandleShowScore(this.nickNameClient));
                    }catch(NumberFormatException e){
                        score=0;
                    }
                    Rank.put(userName, score); //inserisco me stesso
                    
                    Map<String, Integer> SortedRank = sortByValue(Rank);
                    

                    //genero JSON da restituire
                    response = new JSONObject(); 
                    response.put("RankList", new JSONArray()); //aggiungo array all'oggetto
                    RankList= (JSONArray) response.get("RankList");
                    //riempio JSON                    
                    
                    for (Map.Entry<String, Integer> en : SortedRank.entrySet()) { 
                        System.out.println("Key = " + en.getKey() +", Value = " + en.getValue()); 
                        Single=new JSONObject();
                        Single.put("name", en.getKey());
                        Single.put("score", en.getValue());
                        RankList.add(Single);
                    }
                    correctCheck=true;
                    break;
                }       
            }
            System.out.println("ClientHandler HandleShowRank DICE: "+correctCheck);
      
            
        } catch (ParseException ex) {
            System.out.println("ClientHandler error parsing file on HandleShowRank"); 
        }
        return response.toJSONString();  //restituisco oggetto contenente classifica
    }//calcola e restituisce la classifica in JSON
     
    public static HashMap<String, Integer> sortByValue(HashMap<String, Integer> hm){ 
        // Create a list from elements of HashMap 
        List<Map.Entry<String, Integer> > list = 
               new LinkedList<Map.Entry<String, Integer> >(hm.entrySet()); 
  
        // Sort the list 
        Collections.sort(list, new Comparator<Map.Entry<String, Integer> >() { 
            public int compare(Map.Entry<String, Integer> o1,  
                               Map.Entry<String, Integer> o2) 
            { 
                return -(o1.getValue()).compareTo(o2.getValue()); 
            } 
        }); 
          
        // put data from sorted list to hashmap  
        HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>(); 
        for (Map.Entry<String, Integer> aa : list) { 
            temp.put(aa.getKey(), aa.getValue()); 
        } 
        return temp; 
    }
      
    private void HandleChallenge(String received){
        //metodo usato da Client Handler sfidante, per sfidare avversario
        String [] splitted = received.split(" ");
        if(splitted.length==2){
            //ottengo nome avversario
            this.ChallengeOpponentName=splitted[1]; 
            
            if(this.serverReference.checkClientOnline(ChallengeOpponentName)){ //controllo avversario sia online
                
               //tramite server centrale, esorto ClientHandler avverasrio a inviare richiesta UDP al suo client, mettendo i valori in ChallengeHandlerMap a 2 (WAIT STATUS)
               this.serverReference.WarningBeforeChallenge(this.nickNameClient,ChallengeOpponentName); 
               this.waitForNewsAboutChallenge=true; //mi metto in attesa di notizie (valore ChallengeHandlerMap diverso da 2 ) dal server centrale
            }
            else{
                try {
                    this.SendOutcomeChallenge(6); //notifico avversario offline
                } catch (IOException ex) {
                    System.out.println("CLient Handler: error on HandleChallenge on sending outcome");
                }
            }
        
        } 
    }
    
    public void SetsendUdpRequestToClient(String opponentName){
        //invocato da server centale per dirmi che devo inviare richesta udp al mio client, invocato da Client sfidante (opponentName)
        this.sendUdpRequestToClient=true;
        this.ChallengeOpponentName=opponentName;
    } 
    
    private void checkChallengeHandlerTableValue() throws InterruptedException, IOException{
        
        //finche avversario non risponde o scade il timer il valore rimane settato a 2, e CLientHandler dello sfidante aspetta
            synchronized(this.ChallengeHandlerMap){
                while(this.ChallengeHandlerMap.get(myId)==2)
                    this.ChallengeHandlerMap.wait();

                //quando cambia valore, se settato a 1 = partita accettata e iniziata, allora attende la terminazione
                while(this.ChallengeHandlerMap.get(myId)==1)
                    this.ChallengeHandlerMap.wait();
            }
         //nel caso in cui l'avversario non ha accettato o timer scaduto
            try {
                if(this.ChallengeHandlerMap.get(myId)==-1) //se ha rifiutato, il valore rimane a -1, e mando messaggio a client sfidante
                    SendOutcomeChallenge(-1);
                if(this.ChallengeHandlerMap.get(myId)==-2) //se timer scaduto, il valore va a -2 e mando messaggio a client sfidante
                    SendOutcomeChallenge(-2);

        } catch (IOException ex) {
            System.out.println("CLientHandler : checkChallengeHandlerTableValue error on sending results");
        }
        System.out.println("ESCO DA CHECKINTVALUE");
        
    }
    
    private void SendOutcomeChallenge(int mode) throws IOException{
        //metodo con cui il CLientHandler comunica al client l'esito della sfida
        // mode=-2 -> timer scaduto | mode=-1 -> rifiutato | mode=3 -> hai vinto | mode=4 -> hai perso | mode=5-> hai rifiutato | mode=6->avversario offline 
        String message="";
        if(mode==-1)
            message="Il tuo avversario ha rifiutato";
        if(mode==3)
            message="Hai vinto";
        if(mode==4)
            message="Hai perso";
        if(mode==5)
            message="Hai rifiutato la partita";
        if(mode==6)
            message="il tuo avversario e' offline";
        if(mode==-2)
            message="Timeout scaduto";
        
       output.clear();
       output.put(message.getBytes());
       output.flip();
       while (output.hasRemaining()) {
            socket.write(output);
        }
    }
    
    public void updateScore(int value, int Myscore){
            //metodo per modificare punteggio e notificare il risultato della sfida
            // value=1 -> hai vinto 10 punti extra | value=2 -> hai perso 
            String message="Hai totalizzato: "+Integer.toString(Myscore)+" punti";
            
            output.clear();
            output.put(message.getBytes());
            output.flip();
            while (output.hasRemaining()) {
                try {
                    socket.write(output);
                } catch (IOException ex) {
                    System.out.println("ClientHandler error on update score on sending result " );
                    continue;
                }
            }
            
            message="";
            
            
            if(value==1 || value==2){ //ho vinto
                
              //variabili per scorrere DataBase
            JSONParser parser = new JSONParser();
            JSONArray DB = null;
            JSONObject User;
            JSONObject jsonObject;
            int score=0;
            
            String contentOfFile=null;
            boolean existingYet=false;
            boolean correctWrite=false;
            
            //accedo e leggo dati
            contentOfFile=DBH.ReadFromDB();
            if(contentOfFile!=null){
                try {
                    jsonObject = (JSONObject) parser.parse(contentOfFile); //DB completo
                    DB = (JSONArray) jsonObject.get("DataBase"); //Db castato ad array
                    
                    System.out.println("DB"+DB);
                    for(int i=0;i<DB.size();i++){
                        System.out.println("Analizzo : "+(String)((JSONObject)DB.get(i)).get("nickUtente")); //notifico l'utente analizzato
                        User=(JSONObject) DB.get(i);
                        if(this.nickNameClient.equals((String)(User.get("nickUtente")))){
                          score= Integer.parseInt( (String) User.get("PunteggioUtente")) ;
                          score+=10;
                          User.put("PunteggioUtente",Integer.toString(score) );
                          DBH.WriteToDB(jsonObject.toString());
                          break;
                        }
                    }
                } catch (ParseException ex) {
                    System.out.println("ClientHandler error on update score on parsing JSON " );
                }
            }
            if(value==1)
                message="Hai vinto";
            else
                message="Hai pareggiato";
            }
            else //ho perso
                message="Hai perso";
            
            try{
            
                output.clear();
                output.put(message.getBytes());
                output.flip();
                while (output.hasRemaining()) {
                    socket.write(output);
                }
            }catch(IOException e){
                System.out.println("ClientHandler error on update score on sending result " );
            }
            
            
    }
    
    private String Dispatcher(String received){
        //riceve le richieste del cliente, le analizza e decide cosa fare
        String response="";
        String splitted[]= received.split(" ");
        
        if(splitted[0].equals("login")){
            if(loginDone)
                response="Sei gia loggato";
            else
                response=HandleLogin(received);
        }
            
        
        else if(splitted[0].equals("logout")){
            this.logoutRequested=true;
            HandleLogout();
            response="OK "+this.nickNameClient+" logout";
        }
            
        
        else if(splitted[0].equals("lista_amici")){
            if(loginDone)
                response=HandleFriendsList();
            else
                response="Devi prima effettuare il login";
        }
        
        else if(splitted[0].equals("aggiungi_amico")){
            if(loginDone){
                if(HandleAddFriend(received)==true)
                    response="OK, friend added";
                else
                    response="KO, error";
            } 
            else
                response="Devi prima effettuare il login";
        }
        
        else if(splitted[0].equals("mostra_punteggio")){
            if(loginDone)
                response=HandleShowScore(this.nickNameClient);
            else
                response="Devi prima effettuare il login";
        }
        
        else if(splitted[0].equals("mostra_classifica")){
            if(loginDone)
                response=HandleShowRank();
            else
                response="Devi prima effettuare il login";
        }
        
        else if(splitted[0].equals("help")){
            response="I comandi sono:\nregistra_utente Username password\nlogin Username password\nlogout\nlista_amici\nmostra_punteggio\nmostra_classifica\naggiungi_amico nickAmico\nsfida nickAmico";
        }
        
        else if(splitted[0].equals("sfida")){
            if(loginDone){
                HandleChallenge(received);
            }
                
            else
                response="Devi prima effettuare il login";
        }
                
        else
            response="Digita help per vedere i comandi disponibili";
        
        
        return response;
    }
    
    private void normalInteract(){
        //metodo con cui il Client Handler gestisce tutte le richieste (sfida esclusa), invoca il Dispatcher
        try {
            
            this.socket.configureBlocking(false);
            received="";
            ToBeSent="";
            
            //RICEVO RICHIESTE DA CLIENT
            
            input.clear();
            socket.read(input);
            input.flip();
            while (input.hasRemaining()) {
                received+=(char) input.get();
            }
            
            
            if(received!=null && !received.equals("")){
                //ANALIZZO
                ToBeSent=Dispatcher(received);
                
            //RISPONDO    
            output.clear();
            output.put(ToBeSent.getBytes());
            output.flip();
            while (output.hasRemaining()) {
                socket.write(output);
            }
            
            }

            
            this.socket.configureBlocking(true);
            
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    @Override
    public void run() {
        try {
            
        System.out.println("Client Handler online for: "+socket.getRemoteAddress());
        String received="";
        while(true){
           received="";
            
            
            //INIZIO caso in cui sono invitato e gestisco invito
            if(sendUdpRequestToClient==true){ 
                System.out.println("UDP STIMOLATO ");
                //wqServer mi ha detto che devo inviare richiesta udp al client
                Tstart=0;
                Tend=0;
                
                
                UdpServerSender UDPS = new UdpServerSender(2321, this.udpPort, this.ChallengeOpponentName);
                Thread t = new Thread(UDPS);
                t.start();
                Tstart=System.currentTimeMillis();
                try {
                    t.join();
                } catch (InterruptedException ex) {
                    System.out.println("Client Handler error while waiting UDP server sender ");
                }
                
                //resetto booleano
                this.sendUdpRequestToClient=false;
                
                
                
                input.clear(); //e risponde su tcp connection
                socket.read(input);
                Tend=System.currentTimeMillis();
                input.flip();
                while (input.hasRemaining()) {
                    received+=(char) input.get();
                }
                
                
                if( (Tend-Tstart)>5000 ){
                    this.serverReference.RefuseChallenge(this.ChallengeOpponentName,this.nickNameClient, -2 );
                    SendOutcomeChallenge(-2);
                    continue;
                }
                
                
                else if(received.equals("no")  ){
                    this.serverReference.RefuseChallenge(this.ChallengeOpponentName,this.nickNameClient, -1 );
                    SendOutcomeChallenge(5);
                    continue;
                } 
                
                
                   
                else if(received.equals("si")){//se ho risposto si, e non è scaduto il timer(valore ancora settato a 2==waiting), allora procedo ad accettare
                    this.serverReference.AcceptChallenge(this.ChallengeOpponentName, this.nickNameClient);//setta outcome a 1, e genera oggetti con riferimenti pronti per essere presi
                    System.out.println("CH di : "+this.nickNameClient+" GAME IS OVER");
                    continue;
                }
                else{ //caso in cui ricevo caratteri random
                    this.serverReference.RefuseChallenge(this.ChallengeOpponentName,this.nickNameClient, -1 );
                    SendOutcomeChallenge(5);
                    continue;
                }
                    
            } //FINE caso in cui sono invitato e gestisco invito
            
            //CASO IN CUI HO INVITATO, ASPETTO ESITO
            else if(waitForNewsAboutChallenge==true){
                try {
                    
                    checkChallengeHandlerTableValue();
                    //se sono qui, o partita finita o partita rifiutata
                    //resetto valore
                    waitForNewsAboutChallenge=false;
                } catch (InterruptedException ex) {
                    Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
            else 
                this.normalInteract(); //normale interazione
            

            if(this.logoutRequested==true){
                socket.close();
                System.out.println("User logout on: "+Thread.currentThread().getName());
                break;
            }
        }                          
            
        } catch (IOException ex) {
            System.out.println("Client Handler : error on retrieving remote address ");
        }
    }
    
}
