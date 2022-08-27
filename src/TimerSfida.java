import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimerSfida extends TimerTask {

	AtomicBoolean t;
	
	public TimerSfida(AtomicBoolean t) {
		this.t = t;
	}
	
	//Task da eseguire una volta scaduto il timer per terminare la sfida
	@Override
	public void run() {
		t.set(true);
	}
}