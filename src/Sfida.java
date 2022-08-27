import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Sfida implements Runnable {
	private String user;
	private String friend;
	private Server WQ;
	private Selector sel1;
	private SocketChannel sockUser;
	private SocketChannel sockFriend;
	private LinkedList<String> ita;
	private LinkedList<String> eng;	
	
	public Sfida(Server WQ, Selector sel1, String user, String friend, SocketChannel sockUser, SocketChannel sockFriend) {
		this.WQ = WQ;
		this.friend = friend;
		this.sel1 = sel1;
		this.user = user;
		this.sockUser = sockUser;
		this.sockFriend = sockFriend;
		ita = new LinkedList<String>();
		eng = new LinkedList<String>();
		
		//Le parole da tradurre vengono lette dal file "Dizionario.txt" e messe in una LinkedList
		ArrayList<String> list = new ArrayList<String>();
		File file = new File(new File("Dizionario.txt").getAbsolutePath()); 
		Scanner sc;
		try {
			sc = new Scanner(file);
			while (sc.hasNextLine()) 
			      list.add(sc.nextLine()); 
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		Random rand = new Random();
		for(int i = 0; i < 13; i++) 
			ita.add(list.get(rand.nextInt(list.size())));
		
		System.out.println("Parole tradotte :");
		
		//Traduzioni inserite in una LinkedList
		for(int i = 0; i < 13; i++) {
			eng.add(itaToEng(ita.get(i)));	
			System.out.println(eng.get(i));
		}
		
		System.out.println();
	}

	//Metodo che restituisce parola tradotta
	public String itaToEng (String itaWord) {
		String line = null;
		try {
			
			//Connessione HTTP per tradurre le parole (richieste GET)
			URL url = new URL("https://api.mymemory.translated.net/get?q=" + itaWord + "&langpair=it|en");
			URLConnection urlConn = url.openConnection();
			urlConn.connect();
			BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			while((line = in.readLine()) == null) { 
				
			}
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch(IOException e) {
			System.out.println("Connessione ad Internet non riuscita:");	
			e.printStackTrace();
		}
		
		//Deserializzazione della traduzione
		JsonElement el = JsonParser.parseString(line);
		JsonObject obj = el.getAsJsonObject();
        line = obj.getAsJsonObject("responseData").get("translatedText").getAsString();
		return line;
	}

	@Override
	public void run() {
		SelectionKey key1 = sockUser.keyFor(sel1);
		SelectionKey key2 = sockFriend.keyFor(sel1);
		
		//Attesa rimozione chiavi dal selettore principale
		while(key1.interestOps() != 0 || key2.interestOps() != 0) { 
			
		}
		
		//Sfidanti aggiunti alla lista degli utenti occupati
		WQ.getStatus().add(user);
		WQ.getStatus().add(friend);
		
		Selector sel2;
		boolean stop = false;
		int inGame = 2;
		int i = 0;
		int j = 0;
		int puntUser = 0;
		int puntFriend = 0;
		int tradotteUser = 0;
		int tradotteFriend = 0;
		int biancheUser = 0;
		int biancheFriend = 0;
		
		try {
			
			//Registrazione chiavi in READ
			sel2 = Selector.open();
			SelectionKey keyUser = sockUser.register(sel2, SelectionKey.OP_READ);
			SelectionKey keyFriend = sockFriend.register(sel2, SelectionKey.OP_READ);
			
			//Attachment contenente buffer e variabili per gestire il timer
			keyUser.attach(new Attachment());
			keyFriend.attach(new Attachment());
			
			//Finché c'è almeno un utente in gioco
			while(!stop) {
				try {
					sel2.select();
				}
				catch(IOException e) {
					e.printStackTrace();
					break;
				}
				Set <SelectionKey> readyKeys = sel2.selectedKeys();
				Iterator <SelectionKey> iterator = readyKeys.iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();
					Attachment att = (Attachment) key.attachment();
					
					//Una volta scaduti 60 secondi viene eseguito il task per settare la variabile contenuta nell'attachment
					att.time.schedule(new TimerSfida(att.t), 60000);
					try {
						if(key.isReadable()) {
							
							//Se att.t.get() == true, il timer è scaduto e la chiave viene messa in WRITE
							if(att.t.get()) 
								key.interestOps(SelectionKey.OP_WRITE);
							
							//Altrimenti viene letta la traduzione inserita dall'utente (o la parola di inizio sfida)
							else {
								String itaWord = null;
								SocketChannel client = (SocketChannel) key.channel();
								att = (Attachment) key.attachment();
								client.read(att.buf);
								String msgRead = (new String(att.buf.array(), StandardCharsets.UTF_8));
								
								//"\n" indica fine lettura
								if(msgRead.contains("\n")) {
									att.buf.flip();
									msgRead = msgRead.split("\n")[0];
									
									//Ricevuto "Inizio": invio della prima parola
									if(msgRead.contains("Inizio")) {
										if(key == keyUser) 
											itaWord = new String(ita.get(i).concat("\n"));
										else if(key == keyFriend)
											itaWord = new String(ita.get(j).concat("\n"));
									}
									
									/*Ricevuta traduzione: controllo correttezza, aggiornamento del punteggio e invio nuova parola
									(o "Fine", in caso siano finite le parole da tradurre)*/
									else {
										try {
											if(key == keyUser) {
												if(msgRead.contains("Vuoto")) 
													biancheUser++;
												else if(msgRead.equalsIgnoreCase(eng.get(i))) {
													tradotteUser++;
													puntUser+=3;
													WQ.setPunteggio(user, puntUser);
												}
												i++;
												itaWord = new String(ita.get(i).concat("\n"));
											}
											else if(key == keyFriend) {
												if(msgRead.contains("Vuoto")) 
													biancheFriend++;
												if(msgRead.equalsIgnoreCase(eng.get(j))) {
													tradotteFriend++;
													puntFriend+=3;
													WQ.setPunteggio(friend, puntFriend);
												}
												j++;
												itaWord= new String(ita.get(j).concat("\n"));
											}
										}
										catch(IndexOutOfBoundsException e) {
											itaWord = new String("Fine\n");
										}
									}
									att.buf = ByteBuffer.wrap(itaWord.getBytes());
									
									//Chiave messa in WRITE per mandare la parola da tradurre al client
									key.interestOps(SelectionKey.OP_WRITE);
								}
							}	
						}
						
						if(key.isWritable()) {
							
							/*Se att.t.get() == true, il timer è scaduto: viene mandato un messaggio di tempo scaduto al client 
							e si cancella la chiave*/
							if(att.t.get()) {
								SocketChannel client = (SocketChannel) key.channel();
								att = (Attachment) key.attachment();
								String tScaduto = new String("Tempo scaduto\n");
								att.buf = ByteBuffer.wrap(tScaduto.getBytes());
								while(att.buf.hasRemaining())
									client.write(att.buf);
								inGame--;
								if(inGame == 0)
									stop = true;
								key.cancel();
							}
							
							/*Altrimenti si analizza il messaggio da scrivere al client: se contiene la parola "Fine" si cancella 
							la chiave prima di mandare il messaggio di fine sfida, in caso contrario si invia la nuova parola da
							tradurre e la chiave viene rimessa a READ*/
							else {
								SocketChannel client = (SocketChannel) key.channel();
								att = (Attachment) key.attachment();
								String msgWritten = new String(att.buf.array(), StandardCharsets.UTF_8);
								while(att.buf.hasRemaining())
									client.write(att.buf);
								if(msgWritten.contains("Fine")) {
									inGame--;
									if(inGame == 0)
										stop = true;
									key.cancel();
								}
								else {
									key.interestOps(SelectionKey.OP_READ);
									att.buf = ByteBuffer.allocate(256);
									key.attach(att);
								}
							}
						}
					}
					catch(IOException e) {
						key.cancel();
						key.channel().close();
					}
				}
				sel2.selectedKeys().clear();
			}
			
			//Vengono aggiunti 5 punti al vincitore della sfida
			if(puntUser > puntFriend) {
				WQ.setPunteggio(user, 5);
				puntUser+=5;
			}
			if(puntUser < puntFriend) {
				WQ.setPunteggio(friend, 5);
				puntFriend+=5;
			}
			
			//Invio del punteggio ai due sfidanti
			String punteggioUser = null;
			String punteggioFriend = null;
			punteggioUser = new String("Hai tradotto bene " + tradotteUser + " parole "
					+ "e ne hai lasciate " + biancheUser + " in bianco, per un totale di " + puntUser + " punti");			
			punteggioFriend = new String("Hai tradotto bene " + tradotteFriend + " parole "
					+ "e ne hai lasciate " + biancheFriend + " in bianco, per un totale di " + puntFriend + " punti");
			int diff = puntUser - puntFriend;
			String sfidaVinta = new String("Vittoria! ");
			String sfidaPersa = new String("Sconfitta: ");
			String sfidaPareggiata = new String("Pareggio: ");
			ByteBuffer esitoSfida;
			if(diff > 0) {
				esitoSfida = ByteBuffer.wrap((sfidaVinta + punteggioUser + "\n").getBytes()); 
				while(esitoSfida.hasRemaining())
					sockUser.write(esitoSfida);
				esitoSfida = ByteBuffer.wrap((sfidaPersa + punteggioFriend + "\n").getBytes()); 
				while(esitoSfida.hasRemaining())
					sockFriend.write(esitoSfida);
			}
			else if (diff < 0) {
				esitoSfida = ByteBuffer.wrap((sfidaPersa + punteggioUser + "\n").getBytes()); 
				while(esitoSfida.hasRemaining())
					sockUser.write(esitoSfida);
				esitoSfida = ByteBuffer.wrap((sfidaVinta + punteggioFriend + "\n").getBytes()); 
				while(esitoSfida.hasRemaining())
					sockFriend.write(esitoSfida);
			}
			else if (diff == 0) {
				esitoSfida = ByteBuffer.wrap((sfidaPareggiata + punteggioUser + "\n").getBytes()); 
				while(esitoSfida.hasRemaining()) 
					sockUser.write(esitoSfida);
				esitoSfida = ByteBuffer.wrap((sfidaPareggiata + punteggioFriend + "\n").getBytes()); 
				while(esitoSfida.hasRemaining()) 
					sockFriend.write(esitoSfida);
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		//Sfidanti rimossi dalla lista di utenti impegnati in una sfida
		WQ.getStatus().remove(user);
		WQ.getStatus().remove(friend);
			
		//Le chiavi del selector principale vengono rimesse a READ e tale selector viene risvegliato
		key1 = sockUser.keyFor(sel1);
		key1.interestOps(SelectionKey.OP_READ);
		ByteBuffer bufUser = ByteBuffer.allocate(256);
		key1.attach(bufUser);
		key2 = sockFriend.keyFor(sel1);
		key2.interestOps(SelectionKey.OP_READ);
		ByteBuffer bufFriend = ByteBuffer.allocate(256);
		key2.attach(bufFriend);
		sel1.wakeup();
	}
}