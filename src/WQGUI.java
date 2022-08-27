import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Font;
import java.awt.Color;
import java.awt.SystemColor;

public class WQGUI extends JFrame implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private JFrame frame;
	private JTextField txtUsername;
	private JPasswordField txtPassword;
	private JButton btnEntry;
	private JButton btnLogin;
	private JLabel lblWordquizzle;
	private JLabel lblUsername;
	private JLabel lblPassword;
	private JCheckBox checkPass;
	private SocketChannel client;
	private ServiceInterface Entry;
	private int port;
	
	public WQGUI(SocketChannel client, ServiceInterface Entry) {
		this.client = client;	
		this.Entry = Entry;		
	}
	
	public void actionPerformed(ActionEvent evt) {
		
		String s = evt.getActionCommand();
		
		//Registrazione
		if(s.equals("ENTRY")) {
			
			String user = txtUsername.getText();
			String password = String.valueOf(txtPassword.getPassword());
			if(user.trim().isEmpty())
				JOptionPane.showMessageDialog(frame, "Inserisci username", "Error", JOptionPane.ERROR_MESSAGE);
			else if(password.trim().isEmpty())
				JOptionPane.showMessageDialog(frame, "Inserisci password", "Error", JOptionPane.ERROR_MESSAGE);
			else {
				int esito = -1;
				try {
					
					//Viene invocato il metodo remoto entryUser()
					esito = Entry.entryUser(user.trim(), password.trim());
					if(esito == 1)
						JOptionPane.showMessageDialog(frame, "Nome utente già in uso", "Error", JOptionPane.ERROR_MESSAGE);
					else if(esito == 0)
						JOptionPane.showMessageDialog(frame, "Registrazione eseguita con successo");
					else
						JOptionPane.showMessageDialog(frame, "", "Error", JOptionPane.ERROR_MESSAGE);
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		//Login
		if(s.equals("LOGIN")) {
			
			String user = txtUsername.getText();
			String password = String.valueOf(txtPassword.getPassword());
			if(user.trim().isEmpty())
				JOptionPane.showMessageDialog(frame, "Inserisci username", "Error", JOptionPane.ERROR_MESSAGE);
			else if(password.trim().isEmpty())
				JOptionPane.showMessageDialog(frame, "Inserisci password", "Error", JOptionPane.ERROR_MESSAGE);
			else {	
				
				//Ricerca di una porta disponibile, sulla quale attendere messaggi UDP di richiesta di sfida
				DatagramSocket clientUDP = null;
				boolean trovato = false;
				port = 6780;
				while(!trovato) {
					try {
						clientUDP = new DatagramSocket(port);
						trovato = true;
						System.out.println("BIND SU PORTA " + port);
					}
					catch (BindException e) {
						port++;
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				//Invio richiesta al server e lettura esito 
				String msg = s.concat(" ").concat(user).concat(" ").concat(password).concat(" ").concat(Integer.toString(port)).concat("\n");
				ByteBuffer output = ByteBuffer.wrap(msg.getBytes());
				int esito = -1;
				String res = null;
				try {
					while(output.hasRemaining())
						client.write(output);
					BufferedReader in = new BufferedReader(new InputStreamReader(client.socket().getInputStream()));
					res = in.readLine();
					esito = Integer.parseInt(res);
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
				
				//Valutazione esito
				if(esito == 1) {
					JOptionPane.showMessageDialog(frame, "Password errata", "Error", JOptionPane.ERROR_MESSAGE);
					txtPassword.setText("");
				}
				if(esito == 2 || esito == 3) 
					JOptionPane.showMessageDialog(frame, "Utente non registrato", "Error", JOptionPane.ERROR_MESSAGE);
				if(esito == 4) {
					JOptionPane.showMessageDialog(frame, "Utente già loggato", "Error", JOptionPane.ERROR_MESSAGE);
					txtUsername.setText("");
					txtPassword.setText("");
				}
				if(esito == 0) {
					JOptionPane.showMessageDialog(frame, "Login eseguito con successo");
					frame.dispose();
					try {
						//Inizializzazione schermata home
						LoggedGUI log = new LoggedGUI(client, clientUDP, Entry, user);
						log.initialize();
						} 
					catch (IOException e) {
						e.printStackTrace();
						}
					}
				}
			}
		}
	
	public void initialize() {
		
		frame = new JFrame();
		frame.getContentPane().setBackground(Color.WHITE);
		frame.setBounds(100, 100, 769, 533);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		txtUsername = new JTextField();
		txtUsername.setBackground(SystemColor.menu);
		txtUsername.setFont(new Font("Tahoma", Font.PLAIN, 20));
		txtUsername.setBounds(405, 156, 291, 43);
		frame.getContentPane().add(txtUsername);
		txtUsername.setColumns(15);
		
		txtPassword = new JPasswordField();
		txtPassword.setBackground(SystemColor.menu);
		txtPassword.setFont(new Font("Tahoma", Font.PLAIN, 20));
		txtPassword.setBounds(405, 261, 291, 43);
		frame.getContentPane().add(txtPassword);
		txtPassword.setColumns(15);
		
		lblUsername = new JLabel("Username");
		lblUsername.setFont(new Font("Tahoma", Font.PLAIN, 25));
		lblUsername.setBounds(141, 154, 146, 43);
		frame.getContentPane().add(lblUsername);
		
		lblWordquizzle = new JLabel("WordQuizzle");
		lblWordquizzle.setFont(new Font("Tw Cen MT", Font.PLAIN, 50));
		lblWordquizzle.setBounds(251, 10, 365, 98);
		frame.getContentPane().add(lblWordquizzle);
		
		lblPassword = new JLabel("Password");
		lblPassword.setFont(new Font("Tahoma", Font.PLAIN, 25));
		lblPassword.setBounds(141, 262, 146, 36);
		frame.getContentPane().add(lblPassword);
		
		checkPass = new JCheckBox("Show Password");
		checkPass.addActionListener(new ActionListener () {
			public void actionPerformed(ActionEvent e) {
				if(checkPass.isSelected())
					txtPassword.setEchoChar((char) 0);
				else 
					txtPassword.setEchoChar('*');
			}
		});
		checkPass.setBounds(400, 310, 120, 30);
		checkPass.setBackground(Color.WHITE);
		frame.getContentPane().add(checkPass);
		
		btnEntry = new JButton("ENTRY");
		btnEntry.setForeground(Color.WHITE);
		btnEntry.setBackground(Color.BLUE);
		btnEntry.setFont(new Font("Tahoma", Font.BOLD, 25));
		btnEntry.setBounds(110, 383, 200, 57);
		btnEntry.addActionListener(this);
		frame.getContentPane().add(btnEntry);
		
		btnLogin = new JButton("LOGIN");
		btnLogin.setForeground(Color.WHITE);
		btnLogin.setBackground(Color.BLUE);
		btnLogin.setFont(new Font("Tahoma", Font.BOLD, 25));
		btnLogin.setBounds(445, 383, 200, 57);
		btnLogin.addActionListener(this);
		frame.getContentPane().add(btnLogin);
		
		frame.setVisible(true);
	}
}