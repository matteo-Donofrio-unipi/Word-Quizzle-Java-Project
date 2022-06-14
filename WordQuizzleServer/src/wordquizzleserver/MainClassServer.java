
package wordquizzleserver;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.ParseException;


public class MainClassServer {
    
     public static void main(String[] args)throws IOException{
         
            //creo ed esporto il riferimento per la gestione registrazione 
            RmiImplementation RegisterObject = new RmiImplementation();
            IntRegistration stub= (IntRegistration) UnicastRemoteObject.exportObject(RegisterObject,0);
            LocateRegistry.createRegistry(9200);
            Registry r = LocateRegistry.getRegistry(9200);
            r.rebind("Registration-Server",stub);
         
            //avvio server centrale
            WordQuizzleServer server = new WordQuizzleServer(9000, 4);
            server.StartService();
     }
}
