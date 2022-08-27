import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Map;
import com.google.gson.Gson;

public class OperazioniServer implements Runnable {

	private Server WQ;
	private Selector selector;
	private SelectionKey key;
	private String m;
	
	public OperazioniServer(Server WQ, Selector selector, SelectionKey key, String m) {
		this.WQ = WQ;
		this.key = key;
		this.selector = selector;
		this.m = m;
	}

	@Override
	public void run() {
		ArrayList<String> amici;
		Map<String, Integer> classifica;
		Gson gson = new Gson();
		String[] msg = m.split(" ");
		try {
			
			//Elaborazione richiesta e risposta inserita nel buffer dell'attachment
			ByteBuffer output = (ByteBuffer) key.attachment();
			int esito = -1;
			String res = null;
			switch(msg[0]) {
				case "LOGIN": {
					esito = WQ.login(msg[1].trim(), msg[2].trim(), Integer.parseInt(msg[3].trim()));
					res = new String(esito + "\n");
					//SocketChannel di comunicazione inserito nell'HashMap
					WQ.setSock(msg[1], (SocketChannel) key.channel());
					output = ByteBuffer.wrap(res.getBytes());
					key.attach(output); 
					break;
				}
				case "LOGOUT": {
					WQ.getDB().get(msg[1].trim()).setLogged(false);
					res = new String("0\n");
					output = ByteBuffer.wrap(res.getBytes());
					key.attach(output); 
					break;
				}
				case "ADD": {
					esito = WQ.addFriend(msg[2].trim(), msg[3].trim());
					res = new String(esito + "\n");
					output = ByteBuffer.wrap(res.getBytes());
					key.attach(output); 				
					break;
				}
				case "FRIENDS": {
					amici = WQ.friendList(msg[2].trim());
					//Serializzazione attraverso gson
					String GList = gson.toJson(amici);
					res = new String(GList + "\n");
					output = ByteBuffer.wrap(res.getBytes());
					key.attach(output); 
					break;
				}
				case "SCORE": {
					esito = WQ.punteggio(msg[1].trim());
					res = new String(esito + "\n");
					output = ByteBuffer.wrap(res.getBytes());
					key.attach(output); 
					break;
				}
				case "RANKING": {
					classifica = WQ.classifica(msg[1].trim());
					//Serializzazione attraverso gson
					String GList = gson.toJson(classifica);
					res = new String(GList + "\n");
					output = ByteBuffer.wrap(res.getBytes());
					key.attach(output); 
					break;
				}
				case "SEND!": {
					String friend = msg[1].trim();
					String user = msg[2].trim();
					//Controllo amicizia
					esito = WQ.checkFriend(friend, user);
					if(esito == 0) {
						//Controllo se l'amico è loggato
						if(WQ.getDB().get(friend).getLogged()) {
							//Controllo se l'amico non sia già impegnato in un'altra sfida
							if(!WQ.getStatus().contains(friend)){
								
								//Invio richiesta di sfida
								InetAddress address = InetAddress.getByName("localhost");
								DatagramSocket serverSock = new DatagramSocket();
								byte[] buf = new byte[128];
								DatagramPacket send = new DatagramPacket(user.getBytes(), user.length(), address, WQ.getDB().get(friend).getPort());
								DatagramPacket receive = new DatagramPacket(buf, buf.length);
								serverSock.send(send);
								
								//Timeout entro cui ricevere risposta
								serverSock.setSoTimeout(15000);
								try {
									
									//Ricezione risposta
									serverSock.receive(receive); 
									esito = 6; //esito == 6 -> msg ricevuto
								}
								catch(SocketTimeoutException e) {
									esito = 3; //esito == 3 -> tempo scaduto
								}
								if(esito != 3) {
									buf = receive.getData();
									String risposta = new String(buf);
									if(risposta.contains("rifiutata"))
										esito = 5; //esito == 5 -> richiesta rifiutata
								}
								serverSock.close();							
							}
							else 
								esito = 4; //esito == 4 -> utente già occupato
						}
						else
							esito = 7; //esito == 7 -> utente offline
					}
					if(esito != 6) {
						res = new String(esito + "\n");
						output = ByteBuffer.wrap(res.getBytes());
						key.attach(output);
					}
					if(esito == 6) {
						res = new String("Accettata\n");
						output = ByteBuffer.wrap(res.getBytes());
						key.attach(output);
						
						//Se la richiesta è stata accettata viene messo a 0 l'interestOps della chiave dell'amico
						WQ.getSock(user).keyFor(selector).interestOps(0);
						WQ.getSock(friend).keyFor(selector).interestOps(0);
						
						//Avvio thread per gestire la sfida
						Sfida sfida = new Sfida(WQ, selector, user, friend, WQ.getSock(user), WQ.getSock(friend));
						Thread t = new Thread(sfida);
						t.start();
					}
					break;
				}
			}
			
			//Key messa a WRITE e risveglio del selector
			key.interestOps(SelectionKey.OP_WRITE);
			selector.wakeup();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
}