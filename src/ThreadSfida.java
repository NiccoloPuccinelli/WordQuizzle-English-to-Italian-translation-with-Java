import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadSfida implements Runnable {

	private DatagramSocket clientUDP;
	private AtomicBoolean logged;
	private StartGUI chal;
	
	public ThreadSfida(DatagramSocket clientUDP, AtomicBoolean logged, StartGUI chal) {
		this.clientUDP = clientUDP;
		this.logged = logged;
		this.chal = chal;
	}
	
	@Override
	public void run() {
		byte[] buffer = new byte[128];
		DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
		
		//Attesa richiesta UDP finchè l'utente è loggato
		while (logged.get()) {
			 try {
				 
				 //Attesa pacchetto e, una volta ricevuto, avvio schermata di ricezione sfida
				 clientUDP.receive(receivedPacket);
				 String friend = new String(receivedPacket.getData(), receivedPacket.getOffset(), receivedPacket.getLength());
				 chal.sfidaRicevuta(friend, clientUDP, receivedPacket.getPort());
			} 
			catch (IOException e) {
			
			}
		}
	}
}