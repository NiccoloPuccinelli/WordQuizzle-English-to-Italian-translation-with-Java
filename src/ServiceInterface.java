import java.io.IOException;
import java.rmi.Remote;

public interface ServiceInterface extends Remote {

	public final static String REMOTE_OBJECT_NAME = "USER_SERVER";
	
	int entryUser(String nickUtente, String password) throws IOException;

}