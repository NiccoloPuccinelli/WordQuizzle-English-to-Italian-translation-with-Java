import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Utente {
	
	private String user;
	private String password;
	private ArrayList<String> listaAmici;
	private int punteggio;
	
	//Variabili transient per non essere copiate in file json
	private transient int port;
	private transient AtomicBoolean logged;
	
	public Utente(String user, String password) {
		this.user = user;
		this.password = password;
		this.listaAmici = new ArrayList<String>();
		this.punteggio = 0;
		
		//logged settato a false una volta registrato il nuovo utente
		this.logged = new AtomicBoolean(false);
	}
	
	public boolean getLogged() {
		return this.logged.get();
	}
	
	public void setLogged(boolean b) {
		
		//All'avvio del server logged è uguale a null poiché non è presente nel file json
		if(this.logged == null)
			this.logged = new AtomicBoolean(b);
		else
			this.logged.set(b);
	}
	
	public String getName() {
		return this.user;
	}
	
	public String getPass() {
		return this.password;
	}
	
	public ArrayList<String> getList() {
		return this.listaAmici;
	}
	
	public void setPunt(int newPunt) {
		this.punteggio+=newPunt;
	}
	
	public int getPunt() {
		return this.punteggio;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public int getPort() {
		return this.port;
	}
}