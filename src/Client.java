import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

	public static void main(String[] args) {
		
		ServiceInterface Entry = null;
		Registry r;
		SocketAddress address;
		SocketChannel client = null;
		
		//Lookup dell'oggetto remoto
		try {
			r = LocateRegistry.getRegistry(7777);
			Entry = (ServiceInterface) r.lookup(ServiceInterface.REMOTE_OBJECT_NAME);
		} 
		catch (IOException e) {
			e.printStackTrace();
		} 
		catch (NotBoundException e) {
			e.printStackTrace();
		}	
		
		//Apertura connessione TCP
		address = new InetSocketAddress(7890);
		try {
			client  = SocketChannel.open(address);
		} 
		catch (IOException e) {
			e.printStackTrace();
		}

		//Inizializzazione schermata iniziale
		WQGUI WQ = new WQGUI(client, Entry);
		WQ.initialize();
	}
}