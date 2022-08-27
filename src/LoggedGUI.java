import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.SystemColor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class LoggedGUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private JFrame frame;
	private JButton btnFriendList;
	private JButton btnClassifica;
	private JButton btnPunteggio;
	private JButton btnAddFriend;
	private JButton btnLogout;
	private JButton btnPlay;
	private JLabel lblWelcome;
	private JLabel lblName;
	private JTextField txtAddFriend;
	private String user;
	private SocketChannel client;	
	private DatagramSocket clientUDP;
	private AtomicBoolean logged;
	private ServiceInterface Entry;
	private StartGUI chal;
	private Gson gson;
	
	public LoggedGUI(SocketChannel client, DatagramSocket clientUDP, ServiceInterface Entry, String user) throws IOException {
		
		this.user = user;
		this.client = client;
		this.clientUDP = clientUDP;
		this.Entry = Entry;
		this.logged = new AtomicBoolean(true);
		gson = new GsonBuilder().setPrettyPrinting().create();
	}
	
	public void actionPerformed(ActionEvent evt) {
		
		String s = evt.getActionCommand();
		
		//Logout
		if(s.equals("LOGOUT")) {
			
			//Invio richiesta al server e lettura esito 
			String msg = s.concat(" ").concat(user).concat("\n");
			ByteBuffer output = ByteBuffer.wrap(msg.getBytes());
			String res = null;
			int esito = -1;
			try {
				while(output.hasRemaining())
					client.write(output);
				InputStreamReader reader = new InputStreamReader(client.socket().getInputStream());
				BufferedReader in = new BufferedReader(reader);
				res = in.readLine().trim();
				esito = Integer.parseInt(res);
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			
			//Valutazione esito
			if(esito == 0) {
				
				//Chiusura socket UDP e ritorno alla schermata iniziale
				JOptionPane.showMessageDialog(frame, "Logout eseguito con successo");
				logged.set(false);
				clientUDP.close();
				frame.dispose();
				WQGUI WQ = new WQGUI(client, Entry);
				WQ.initialize();
			}
		}
		
		//Aggiungi amico
		if(s.equals("ADD FRIEND")) {
			
			String friend = txtAddFriend.getText();
			if(friend.trim().isEmpty()) 
				JOptionPane.showMessageDialog(frame, "Inserisci utente", "Error", JOptionPane.ERROR_MESSAGE);
			else {
				
				//Invio richiesta al server e lettura esito 
				String msg = s.concat(" ").concat(friend).concat(" ").concat(user).concat("\n");
				ByteBuffer output = ByteBuffer.wrap(msg.getBytes());
				int esito = -1;
				String res = null;
				try {
					while(output.hasRemaining())
						client.write(output);
					InputStreamReader reader = new InputStreamReader(client.socket().getInputStream());
					BufferedReader in = new BufferedReader(reader);
					res = in.readLine().trim();
					esito = Integer.parseInt(res);
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
				
				//Valutazione esito
				if(esito == 1)
					JOptionPane.showMessageDialog(frame, "Tu e " + friend + " siete già amici", "Error", JOptionPane.ERROR_MESSAGE);
				if(esito == 2)
					JOptionPane.showMessageDialog(frame, "Utente non registrato", "Error", JOptionPane.ERROR_MESSAGE);
				if(esito == 3)
					JOptionPane.showMessageDialog(frame, "Non puoi aggiungere te stesso!", "Error", JOptionPane.ERROR_MESSAGE);
				if(esito == 0) 
					JOptionPane.showMessageDialog(frame, "Tu e " + friend + " ora siete amici");
				txtAddFriend.setText("");
			}
		}
		
		//Lista di amici
		if(s.equals("FRIENDS LIST")) {
			
			//Invio richiesta al server e lettura json
			String msg = s.concat(" ").concat(user).concat("\n");
			ByteBuffer output = ByteBuffer.wrap(msg.getBytes());
			String GList = new String();
			try {
				while(output.hasRemaining())
					client.write(output);
				InputStreamReader reader = new InputStreamReader(client.socket().getInputStream());
				BufferedReader in = new BufferedReader(reader);
				GList = in.readLine().trim();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			
			//listType definisce in che tipo di dato deserializzare il json
			java.lang.reflect.Type listType = new TypeToken<ArrayList<String>>() {}.getType();
			ArrayList<String> list = gson.fromJson(GList, listType);
			
			//Miglioramento formato output
			String friends = "";
			for(String friend: list)
				friends+= "- " + friend + "\n";
			JOptionPane.showMessageDialog(frame, friends);
		}
		
		//Punteggio
		if(s.equals("SCORE")) {
			
			//Invio richiesta al server e lettura punteggio 
			String msg = s.concat(" ").concat(user).concat(" \n");
			ByteBuffer output = ByteBuffer.wrap(msg.getBytes());
			int esito = -1;
			String res = null;
			try {
				while(output.hasRemaining())
					client.write(output);
				InputStreamReader reader = new InputStreamReader(client.socket().getInputStream());
				BufferedReader in = new BufferedReader(reader);
				res = in.readLine().trim();
				esito = Integer.parseInt(res);
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			JOptionPane.showMessageDialog(frame, "Punteggio attuale: " + esito);
		}
		
		//Classifica
		if(s.equals("RANKING")) {
			
			//Invio richiesta al server e lettura json 
			String msg = s.concat(" ").concat(user).concat("\n");
			ByteBuffer output = ByteBuffer.wrap(msg.getBytes());
			String GList = new String();
			try {
				while(output.hasRemaining())
					client.write(output);
				InputStreamReader reader = new InputStreamReader(client.socket().getInputStream());
				BufferedReader in = new BufferedReader(reader);
				GList = in.readLine().trim();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			
			//listType definisce in che tipo di dato deserializzare il json
			java.lang.reflect.Type listType = new TypeToken<Map<String, Integer>>() {}.getType();
			Map<String, Integer> list = gson.fromJson(GList, listType);
			
			//Miglioramento formato output
			int posizione = 1;
			String classifica = "";
			Iterator<java.util.Map.Entry<String, Integer>> it = list.entrySet().iterator();
			while (it.hasNext()) {
				java.util.Map.Entry<String, Integer> entry = it.next();
				classifica+= new String(posizione + ". " + entry.getKey() + ", " + entry.getValue() + " punti\n");
				posizione++;
			}
			JOptionPane.showMessageDialog(frame, classifica);
		}

		//Invio richiesta di sfida
		if(s.equals("PLAY!")) {
			
			//Avvio interfaccia in cui inserire utente da sfidare
			this.chal = new StartGUI(this.client, this.user, frame);		
			chal.inviaSfida();	
			frame.setVisible(false);
		}
		
	}
	
	public void initialize() {
		
		frame = new JFrame();
		frame.getContentPane().setBackground(Color.WHITE);
		frame.setBounds(100, 100, 723, 365);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		lblWelcome = new JLabel("Welcome");
		lblWelcome.setBounds(25, 21, 88, 36);
		frame.getContentPane().add(lblWelcome);
		lblWelcome.setFont(new Font("Tahoma", Font.PLAIN, 20));
		
		lblName = new JLabel(user + "!");
		lblName.setBounds(115, 19, 180, 36);
		lblName.setFont(new Font("Tahoma", Font.PLAIN, 25));
		frame.getContentPane().add(lblName);
		lblWelcome.setFont(new Font("Tahoma", Font.PLAIN, 20));
		
		btnPlay = new JButton("PLAY!");
		btnPlay.setBounds(284, 97, 135, 135);
		btnPlay.setForeground(Color.WHITE);
		btnPlay.setBackground(Color.BLUE);
		btnPlay.setFont(new Font("Tahoma", Font.BOLD, 30));
		btnPlay.addActionListener(this);
		frame.getContentPane().add(btnPlay);
		
		btnLogout = new JButton("LOGOUT");
		btnLogout.setBackground(UIManager.getColor("Button.background"));
		btnLogout.setForeground(Color.BLACK);
		btnLogout.setBounds(561, 269, 117, 36);
		btnLogout.setFont(new Font("Tw Cen MT", Font.PLAIN, 20));
		btnLogout.addActionListener(this);
		frame.getContentPane().add(btnLogout);
		
		btnFriendList = new JButton("FRIENDS LIST");
		btnFriendList.setBounds(53, 207, 180, 43);
		btnFriendList.setBackground(UIManager.getColor("Button.background"));
		btnFriendList.setForeground(Color.BLACK);
		btnFriendList.setFont(new Font("Tw Cen MT", Font.PLAIN, 25));
		btnFriendList.addActionListener(this);
		frame.getContentPane().add(btnFriendList);
		
		txtAddFriend = new JTextField();
		txtAddFriend.setBounds(53, 138, 180, 43);
		txtAddFriend.setBackground(SystemColor.menu);
		txtAddFriend.setFont(new Font("Tahoma", Font.PLAIN, 17));
		frame.getContentPane().add(txtAddFriend);
		txtAddFriend.setColumns(15);
		
		btnAddFriend = new JButton("ADD FRIEND");
		btnAddFriend.setBounds(53, 86, 180, 43);
		btnAddFriend.setBackground(UIManager.getColor("Button.background"));
		btnAddFriend.setForeground(Color.BLACK);
		btnAddFriend.setFont(new Font("Tw Cen MT", Font.PLAIN, 25));
		btnAddFriend.addActionListener(this);
		frame.getContentPane().add(btnAddFriend);
		
		btnPunteggio = new JButton("SCORE");
		btnPunteggio.setBounds(461, 86, 180, 43);
		btnPunteggio.setBackground(UIManager.getColor("Button.background"));
		btnPunteggio.setForeground(Color.BLACK);
		btnPunteggio.setFont(new Font("Tw Cen MT", Font.PLAIN, 25));
		btnPunteggio.addActionListener(this);
		frame.getContentPane().add(btnPunteggio);
		
		btnClassifica = new JButton("RANKING");
		btnClassifica.setForeground(Color.BLACK);
		btnClassifica.setBackground(UIManager.getColor("Button.background"));
		btnClassifica.setBounds(461, 161, 180, 43);
		btnClassifica.setFont(new Font("Tw Cen MT", Font.PLAIN, 25));
		btnClassifica.addActionListener(this);
		frame.getContentPane().add(btnClassifica);

		frame.setVisible(true);
		
		//Viene fatto partire un thread che si mette in attesa delle richieste di sfida UDP
		this.chal = new StartGUI(this.client, this.user, frame);		
		Thread t = new Thread(new ThreadSfida(clientUDP, logged, chal));
		t.start();
	}
}