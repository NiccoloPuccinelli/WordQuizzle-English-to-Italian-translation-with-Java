import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class Server extends RemoteServer implements ServiceInterface {
	
	private static final long serialVersionUID = 1L;
	
	private Gson gson;
	private File f;
	private ConcurrentHashMap<String, Utente> UserDB;
	private ConcurrentHashMap<String, SocketChannel> Sockets;
	private LinkedBlockingQueue<String> busyUsers;
	
	public Server() throws IOException {
		
		//Controllo esistenza del file, se non esiste viene creato
		f = new File("JsonDB.json");
		if(!f.exists())
			f.createNewFile();
		
		//Database utenti
		UserDB = new ConcurrentHashMap<String,Utente>();
		
		//Struttura dati in cui mantenere SocketChannel degli utenti
		Sockets = new ConcurrentHashMap<String,SocketChannel>();
		
		//Struttura dati in cui mantenere utenti attualmente occupati in una sfida
		busyUsers = new LinkedBlockingQueue<String>();
		
		//Lettura file json e copia dei dati nel DB degli utenti
		gson = new GsonBuilder().setPrettyPrinting().create();
		FileReader reader = new FileReader("JsonDB.json");
		//listType definisce in che tipo di dato deserializzare il json
		Type listType = new TypeToken<ConcurrentHashMap<String, Utente>>() {}.getType();
		UserDB = gson.fromJson(reader, listType);
		reader.close();
		if(UserDB != null) {
			for(ConcurrentHashMap.Entry<String, Utente> entry : UserDB.entrySet()) {
			    String name = entry.getKey();
			    UserDB.get(name).setLogged(false);
			}
		}
	}
	
	public ConcurrentHashMap<String,Utente> getDB(){
		return UserDB;
	}
	
	public synchronized LinkedBlockingQueue<String> getStatus(){
		return busyUsers;
	}
	
	public synchronized void setSock(String nickUtente, SocketChannel sock) {
		Sockets.put(nickUtente, sock);
	}
	
	public SocketChannel getSock(String user) {
		return Sockets.get(user);
	}

	public synchronized int entryUser(String nickUtente, String password) throws IOException {
		
		//Creazione nuovo utente, aggiornamento del DB e del json
		if(UserDB == null) 
			UserDB = new ConcurrentHashMap<String,Utente>();
		if(UserDB.containsKey(nickUtente))
			return 1;
		Utente User = new Utente(nickUtente, password);
		UserDB.put(nickUtente, User);
		f.delete();
		f.createNewFile();
		FileWriter writer = new FileWriter(f);
		gson.toJson(UserDB, writer);
		writer.close();
		return 0;
	}
	
	public synchronized void setPunteggio(String nickUtente, int punteggio) throws IOException {
		
		//Aggiornamento del punteggio e del json
		UserDB.get(nickUtente).setPunt(punteggio);
		f.delete();
		f.createNewFile();
		FileWriter writer = new FileWriter(f);
		gson.toJson(UserDB, writer);
		writer.close();		
	}
	
	//Metodo per controllare la presenza di friend nella lista amici di nickUtente
	public int checkFriend(String friend, String nickUtente) {
		
		if(UserDB.containsKey(friend)) {
			if(UserDB.get(nickUtente).getList().contains(friend)) {
				return 0;
				}
			return 1;
			}
		return 2;
	}
	
	public int login(String nickUtente, String password, int port) throws IOException {
		
   		if(UserDB == null) 
   			return 3;
		if(UserDB.containsKey(nickUtente)) {
			if(UserDB.get(nickUtente).getPass().trim().compareTo(password) == 0) {
				if(UserDB.get(nickUtente).getLogged() == true)
					return 4;
				
				//Assegnamento porta UDP
				UserDB.get(nickUtente).setPort(port);	
				
				UserDB.get(nickUtente).setLogged(true);
				return 0;
			}
			return 1;
		}
		return 2;
	}
	
	public synchronized int addFriend(String friend, String nickUtente) throws IOException {
		
		//Aggiornamento della lista amici e del json
		if(!friend.equals(nickUtente)) {
			if(UserDB.containsKey(friend)) {
				if(!UserDB.get(nickUtente).getList().contains(friend)) {
					UserDB.get(nickUtente).getList().add(friend);
					UserDB.get(friend).getList().add(nickUtente);
					f.delete();
					f.createNewFile();
					FileWriter writer = new FileWriter(f);
					gson.toJson(UserDB, writer);
					writer.close();
					return 0;
				}
				return 1;
			}
			return 2;
		}
		return 3;
	}
	
	public ArrayList<String> friendList(String nickUtente) {
		return UserDB.get(nickUtente).getList();
	}
	
	public int punteggio(String nickUtente) {
		return UserDB.get(nickUtente).getPunt();
	}
	
	public Map<String, Integer> classifica(String nickUtente) {
			
		//Nomi e relativi punteggi inseriti in un HashMap
		HashMap<String, Integer> unsortedMap = new HashMap<String, Integer>();
		unsortedMap.put(nickUtente, UserDB.get(nickUtente).getPunt());
		for (String key : UserDB.get(nickUtente).getList()) {
			if(key != null)
				unsortedMap.put(key, UserDB.get(key).getPunt());
		}
		
		//Ordinamento della classifica
		final Map<String, Integer> sortedMap = unsortedMap.entrySet().stream()
                .sorted((Map.Entry.<String, Integer>comparingByValue().reversed()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		return sortedMap;
	}
			
	public static void main (String[] args) throws IOException {
				
		Server WQ = new Server();
		
		//Connessione remota: creazione registro e bind
		try {
			ServiceInterface stub = (ServiceInterface) UnicastRemoteObject.exportObject(WQ, 0);
			LocateRegistry.createRegistry(7777);
			Registry r = LocateRegistry.getRegistry(7777);
			r.rebind(ServiceInterface.REMOTE_OBJECT_NAME, stub);
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
		
		//CachedThreadPool per gestire richieste
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
		ServerSocketChannel serverChannel;
		Selector selector = null;
		
		try {
			
			//Apertura channel di comunicazione col client
			serverChannel = ServerSocketChannel.open();
			ServerSocket sock = serverChannel.socket();
			InetSocketAddress address = new InetSocketAddress(7890);
			sock.bind(address);
			serverChannel.configureBlocking(false);
			
			//Apertura selector e registrazione chiave in ACCEPT
			selector = Selector.open();
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			while (true) {
				 selector.select();
				 Set <SelectionKey> readyKeys = selector.selectedKeys();
				 Iterator <SelectionKey> iterator = readyKeys.iterator();
				 while (iterator.hasNext()) {
					 SelectionKey key = iterator.next();
					 iterator.remove();
					 try {
						 
						 if(key.isAcceptable()) {
							 
							 //Viene associato un buffer ad ogni chiave
							ServerSocketChannel server = (ServerSocketChannel) key.channel();
							SocketChannel client = server.accept();
							client.configureBlocking(false);
							key = client.register(selector, SelectionKey.OP_READ);
							ByteBuffer buf = ByteBuffer.allocate(256);
							key.attach(buf); 
						 }
						 
						 if(key.isReadable()) {
							 
							 //Lettura della richiesta dal channel nel buffer
							SocketChannel client = (SocketChannel) key.channel();
							ByteBuffer buf = (ByteBuffer) key.attachment();
							client.read(buf);
							String m = new String(buf.array(), StandardCharsets.UTF_8);
							
							//"\n" indica fine lettura
							if(m.contains("\n")) {
								
								//Messaggio letto passato a thread incaricato di eseguire l'operazione richiesta
								OperazioniServer op = new OperazioniServer(WQ, selector, key, m);
								executor.execute(op);
							}
						 }
						
						 if(key.isWritable()) {
							 
							 //Scrittura della risposta dal buffer nel channel
							SocketChannel client = (SocketChannel) key.channel();
							ByteBuffer buf = (ByteBuffer) key.attachment();
							while(buf.hasRemaining())
								client.write(buf);
							String risposta = new String(buf.array(), StandardCharsets.UTF_8);
							
							//Se la risposta contiene "Accettata" significa che deve iniziare la sfida
							if(risposta.contains("Accettata"))
								key.interestOps(0);
							else {
								
								//Richiesta esaurita, key rimessa a READ
								key.interestOps(SelectionKey.OP_READ);
								ByteBuffer buf2 = ByteBuffer.allocate(256);
								key.attach(buf2);
							}
					 	}
						 
					 }
					 catch (IOException e) { 
							key.cancel();
							key.channel().close(); 
						}	
				 	}
				 selector.selectedKeys().clear();
				}
			}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
}