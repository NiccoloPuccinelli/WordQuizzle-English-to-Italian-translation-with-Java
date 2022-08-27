import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

public class Attachment {
	ByteBuffer buf;
	Timer time;
	AtomicBoolean t;
	
	public Attachment() {
		
		//Buffer per comunicazione client-server
		this.buf = ByteBuffer.allocate(256);
		
		//Timer sfida, uno per ogni partecipante alla sfida
		this.time = new Timer();
		
		//Variabile booleana che verrà settata a true una volta scaduto il timer
		this.t = new AtomicBoolean(false);
	}
}