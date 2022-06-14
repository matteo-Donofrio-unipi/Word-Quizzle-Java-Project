
package wordquizzleserver;

import java.rmi.*;
import java.rmi.RemoteException;
public interface IntRegistration extends Remote {
    //firma del metodo da implementare per RMI invocation
    String RegisterNewUser(String nickName, String password) throws RemoteException;
    
}
