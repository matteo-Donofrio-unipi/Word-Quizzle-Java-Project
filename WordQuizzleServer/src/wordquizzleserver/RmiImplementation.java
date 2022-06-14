
package wordquizzleserver;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class RmiImplementation extends RemoteServer implements IntRegistration {
    
    JSONObject jsonObject = null; 
    JSONArray DB = null ;
    Dbhandler DBH;
    
    public RmiImplementation(){
        this.DBH=Dbhandler.getIstance();
    }
    
    
    //implemento metodo del RMI che gestisce la registrazione utente
    public synchronized String RegisterNewUser(String nickName, String password) throws RemoteException {
        String response="KO 300";
        try {
            //variabili per scorrere DataBase
            JSONParser parser = new JSONParser();
            JSONArray amici = null;
            JSONObject User;
            
            String contentOfFile=null;
            boolean existingYet=false;
            boolean correctWrite=false;
            
            //accedo al DB e leggo dati
            contentOfFile=DBH.ReadFromDB();
            
            if(contentOfFile!=null){
                System.out.println("CONTENTS: "+contentOfFile);
                jsonObject = (JSONObject) parser.parse(contentOfFile); //DB completo
                DB = (JSONArray) jsonObject.get("DataBase"); //Db castato ad array
            
                System.out.println("DB"+DB);
                //scandisco il DB
                 for(int i=0;i<DB.size();i++){
                    System.out.println("Analizzo : "+(String)((JSONObject)DB.get(i)).get("nickUtente")); //notifico l'utente analizzato 
                    User=(JSONObject) DB.get(i);
                    if(nickName.equals((String)(User.get("nickUtente")))){
                        existingYet=true;
                        break;
                    }       
                }
            }
            else{
                System.out.println("RegisterNewUser: error, file content is null");
                return "KO 300";
            }
                
            
            if(!existingYet){ //se utente non esiste già
                //creo e inserisco il nuovo utente
                JSONObject UserToBeAdded =  new JSONObject();
                UserToBeAdded.put("nickUtente",nickName);
                UserToBeAdded.put("password",password);
                UserToBeAdded.put("Amici",new JSONArray());
                UserToBeAdded.put("PunteggioUtente","0");
                //aggiungo il nuovo utente al db presente
                DB.add(UserToBeAdded);
                //salvo il nuovo file
                correctWrite=DBH.WriteToDB(jsonObject.toString());
            }
            if(correctWrite)
                response=nickName+" registered";
            if(existingYet)
                response=nickName+" existing yet";
        } catch (ParseException ex) {
           // Logger.getLogger(WQServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("RMI RISPONDE: "+response);
        return response;
    }
    
}
