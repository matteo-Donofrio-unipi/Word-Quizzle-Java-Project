
package wordquizzleserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class TranslationService {
    
   public ConcurrentHashMap<String, String> Dictionary;
   private boolean retrievingWordsCompleted=false;
   private boolean translated=false; //verifica se le parole sono gia state tradotte
   private static TranslationService istance=null; //gestione singleton
   
   private TranslationService(){}
   
   public synchronized static TranslationService getIstance(){
       //metodo che verifica che venga istanziato un solo oggetto di questa classe
        if(istance==null)
            istance=new TranslationService();
        return istance;
    }

   private ConcurrentHashMap getWordsFromDictionary(){
       //accede al file Dizionario.txt ed estrae le parole inserendole nel proprio dizionario
        Dictionary = new ConcurrentHashMap <String,String>(); //inizializzo
        int randomNumber;
        boolean correctCheck=false;
        FileReader FReader = null;
        BufferedReader BuffReader;
        String word=""; 
        
        randomNumber= (int)(System.currentTimeMillis()%9); //numero random
        int insertedWords=0;
        try {
            FReader = new FileReader("Dizionario.txt");
        } catch (FileNotFoundException ex) {
            System.out.println("Error while opening dictionary");
        }
        BuffReader = new BufferedReader(FReader);
        try {
            word=BuffReader.readLine();
            while(word!=null && insertedWords<4){ //mi assicuro di inserire al massimo 4 parole
                if(randomNumber%2==0){ //solo se l'iteratore è pari inserisco la parola
                    Dictionary.put(word, "a"); //inserisco parole in italiano, e per ora, traduzione=a, per tutte le parole
                    System.out.println("word: "+word);
                    insertedWords++;
                }
                randomNumber++;
                word=BuffReader.readLine();
            }
            System.out.println("SRT: ok words retrieved");
            correctCheck=true;
            FReader.close();
        } catch (IOException ex) {
            System.out.println("Error while reading from DB");
            correctCheck=false;
        }
        
       return Dictionary;
   }
   
   public synchronized ConcurrentHashMap getWords(){ //synch perche invocato da piu thread
       //RESTITUISCE IL DIZIONARIO CON PAROLE IN ITALIANO E RELATIVA TRADUZIONE
       boolean correctCheck=false;
       Dictionary=getWordsFromDictionary(); //creo istanza con parole random
       Dictionary=Translate(Dictionary); //la traduco
       return Dictionary;
       
   } //restituisce l'hash map contenente le parole e traduzioni al ClientHandler  
   

   
   private ConcurrentHashMap<String, String>  Translate(ConcurrentHashMap<String, String> DictionaryToBeTranslated){
       //SERVIZIO CHE TRADUCE LE PAROLE TRAMITE RICHIESTA HTTP
       System.out.println("Dentro Translate ");
       boolean correctCheck=false;
       String wordIta;
       //variabili per costruire url dinamico
       String urlBase1="https://api.mymemory.translated.net/get?q=";
       String urlBase2="&langpair=it|en";
       String urlComplete;
       
       //variabili JSON
       JSONObject jsonObject;      
       JSONObject responseData;
       JSONParser parser = new JSONParser();
       
       InputStream in = null;
       String received="";
       URL u;
       int byteLetti=0;
       try {
            for (Map.Entry<String,String> entry : DictionaryToBeTranslated.entrySet()){
                
                //prendo parole random
                wordIta=entry.getKey();
                //creo url
                received="";
                urlComplete=urlBase1+wordIta+urlBase2;
                u = new URL(urlComplete);
                
                //mi connetto e ottengo dati
                in = u.openStream();
                in = new BufferedInputStream(in); 
                Reader r = new InputStreamReader(in);
                //leggo byte per byte il contenuto della risposta http
                while((byteLetti=r.read())!=-1)
                    received+=(char)byteLetti;
                
                //apro il contenuto(JSON) e accedo al campo tradotto
                try {
                    jsonObject= (JSONObject) parser.parse(received);
                    responseData= (JSONObject) jsonObject.get("responseData");
                    entry.setValue((String) responseData.get("translatedText")); //inserisco la parola tradotta nel valore della entry
                } catch (ParseException ex) {
                      System.out.println("error while parsing JSON to String on SRT");
                }
            }
            System.out.println("SRT: ok translation completed");
            correctCheck=true;
            in.close();
            
        } catch (MalformedURLException ex) {
            System.out.println("error while creating URL on SRT");
        } catch (IOException ex) {
            System.out.println("error while translating words on SRT");
        }
       
       translated=true;
       
       
       return DictionaryToBeTranslated;
       
   } //traduce le parole, invocato prima di restituire le parole a CH
   


}
