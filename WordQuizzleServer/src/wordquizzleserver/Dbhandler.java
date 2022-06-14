
package wordquizzleserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class Dbhandler {
    private static Dbhandler istance=null;
    
    
    public synchronized static Dbhandler getIstance(){ 
    //metodo che verifica che venga istanziato un solo oggetto di questa classe
        if(istance==null)
            istance=new Dbhandler();
        return istance;
    }// genera una sola istanza di tale oggetto
    
    public Dbhandler(){
        //quando istanziato verifica la presenza del DB, in caso negativo lo genera
       String resultOfDbCheck=this.ReadFromDB();
       if(resultOfDbCheck==null)
           System.out.println("Dbhandler: Error on constructor");
       else
           System.out.println("Dbhandler: OK Db works");
    }
    
    
    public synchronized String ReadFromDB(){ //legge dati dal db
        System.out.println("DBH: inizio lettura ");  
        
        FileReader FReader = null;
        BufferedReader BuffReader;
        String contentOfDB=null; //valore null necessario al controllo fatto in createDB(se null allora crea il db)
        String temp="";
        try {
            FReader = new FileReader("DataBase.json");
        } catch (FileNotFoundException ex) {
            System.out.println("Dbhandler: File not existing yet, creating...");
            createDB();
            try {
                FReader = new FileReader("DataBase.json");
            } catch (FileNotFoundException ex1) {
                System.out.println("Dbhandler: error despite file has been created");
                return null;
            }
        }
        BuffReader = new BufferedReader(FReader);
        try {
            temp=BuffReader.readLine();
            if(temp!=null)//se esiste il db
                contentOfDB=""; //allora preparo la variabile per essere scritta
            while(temp!=null){
                contentOfDB+=temp;
                temp=BuffReader.readLine();
            }
            FReader.close();
        } catch (IOException ex) {
            System.out.println("Dbhandler: Error while reading from DB");
        }
        
        System.out.println("DBH: fine lettura ");  
        return contentOfDB;
    }
    
    public synchronized boolean WriteToDB(String contentOfFile){ //scrive dati sul db
        
        System.out.println("DBH: inizio scrittura ");  
        System.out.println("CF: "+contentOfFile);
        FileWriter FWriter = null;
        BufferedWriter BuffWriter = null;
        String contentOfDB="";
        String temp="";
        boolean correctCheck=false;
        try {
            FWriter = new FileWriter("DataBase.json");
            BuffWriter = new BufferedWriter(FWriter);
        } catch (FileNotFoundException ex) {
            System.out.println("Dbhandler: Error while opening DB");
        } catch (IOException ex) {
        }
        try {
            BuffWriter.write(contentOfFile);
            System.out.println("CF: "+contentOfFile);
            BuffWriter.flush();
            correctCheck=true;
            FWriter.close();
        } catch (IOException ex) {
            System.out.println("Dbhandler: Error while writing on DB");
        }
        
        System.out.println("DBH: fine scrittura");  
        return correctCheck;
    }
    
    private synchronized void createDB(){ 
        //variabili per scorrere e creare DataBase
        String contentOfFile=null;
        JSONObject jsonObject = new JSONObject(); 
        JSONArray DB = null;

        jsonObject.put("DataBase", new JSONArray());
        DB = (JSONArray) jsonObject.get("DataBase"); //Db castato ad array
        
        WriteToDB(jsonObject.toString());
        System.out.println("Dbhandler: Db created"+DB);
    }//controlla se esiste il db, in caso negativo lo crea
}
