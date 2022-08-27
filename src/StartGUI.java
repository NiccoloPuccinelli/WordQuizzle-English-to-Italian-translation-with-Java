import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class StartGUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private JFrame frame;
	private JFrame log;
	private JLabel lblChal;
	private JLabel lblSfidaUnAmico;
	private JTextField textField;
	private JButton btnBack;
	private JButton btnSfida;
	private JButton btnAccetta;
	private JButton btnRifiuta;
	private String user;
	private SocketChannel client;	
	private static boolean ricevuta;
	
	public StartGUI(SocketChannel client, String user, JFrame log) {	
		this.user = user;
		this.client = client;
		this.log = log;
		
		/*Variabile booleana per evitare che un utente possa inviare una sfida prima 
		di aver deciso se accettare o rifiutare un'eventuale richiesta pendente*/
		ricevuta = false;
	}

	@Override
	public void actionPerformed(ActionEvent evt) { }
	
	public void sfidaRicevuta(String friend, DatagramSocket clientUDP, int port) {
		
		log.setVisible(false);
		
		ricevuta = true;
		
		frame = new JFrame();
		frame.setBounds(100, 100, 430, 248);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setBackground(Color.WHITE);
		frame.getContentPane().setLayout(null);
		
		lblChal = new JLabel(friend + " challenged you!");
		lblChal.setHorizontalAlignment(SwingConstants.CENTER);
		lblChal.setFont(new Font("Tahoma", Font.PLAIN, 20));
		lblChal.setBounds(85, 27, 250, 32);
		frame.getContentPane().add(lblChal);
		
		btnAccetta = new JButton("ACCEPT");
		btnAccetta.setBackground(UIManager.getColor("Button.background"));
		btnAccetta.setForeground(Color.BLACK);
		btnAccetta.setBounds(29, 116, 150, 43);
		btnAccetta.setFont(new Font("Tw Cen MT", Font.PLAIN, 25));
		btnAccetta.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				
				//Invio messaggio di accettazione della sfida tramite pacchetto UDP
				String msg = new String("SFIDA! accettata\n");
				InetAddress address = null;
				try {
					address = InetAddress.getByName("localhost");
					DatagramPacket send = new DatagramPacket(msg.getBytes(), msg.length(), address, port);
					clientUDP.send(send);
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
				
				frame.dispose();
				System.out.println("Sfida ricevuta");
				
				//Inizializzazione schermata di gioco
				GameGUI game = new GameGUI(client, log);
				game.initialize();				
			}
		});
		frame.getContentPane().add(btnAccetta);
		
		btnRifiuta = new JButton("REFUSE");
		btnRifiuta.setBackground(UIManager.getColor("Button.background"));
		btnRifiuta.setForeground(Color.BLACK);
		btnRifiuta.setFont(new Font("Tw Cen MT", Font.PLAIN, 25));
		btnRifiuta.setBounds(233, 116, 150, 43);
		btnRifiuta.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				
				//Invio messaggio di rifiuto della sfida tramite pacchetto UDP
				String msg = new String("SFIDA! rifiutata\n");
				InetAddress address = null;
				try {
					address = InetAddress.getByName("localhost");
					DatagramPacket send = new DatagramPacket(msg.getBytes(), msg.length(), address, port);
					clientUDP.send(send);
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
				
				//Ritorno alla schermata home
				frame.dispose();
				log.setVisible(true);
			}
		});		
		frame.getContentPane().add(btnRifiuta);
		
		frame.setVisible(true);
	}
	
	public void inviaSfida() {
		
		frame = new JFrame();
		frame.getContentPane().setBackground(Color.WHITE);
		frame.setBounds(100, 100, 448, 227);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		lblSfidaUnAmico = new JLabel("Challenge a friend");
		lblSfidaUnAmico.setHorizontalAlignment(SwingConstants.CENTER);
		lblSfidaUnAmico.setForeground(Color.DARK_GRAY);
		lblSfidaUnAmico.setFont(new Font("Tahoma", Font.PLAIN, 20));
		lblSfidaUnAmico.setBounds(32, 21, 174, 37);
		frame.getContentPane().add(lblSfidaUnAmico);
		
		textField = new JTextField();
		textField.setBackground(SystemColor.menu);
		textField.setFont(new Font("Tahoma", Font.PLAIN, 17));
		textField.setBounds(32, 105, 216, 37);
		frame.getContentPane().add(textField);
		textField.setColumns(15);
		
		btnBack = new JButton("BACK");
		btnBack.setForeground(Color.BLACK);
		btnBack.setFont(new Font("Tw Cen MT", Font.PLAIN, 20));
		btnBack.setBounds(320, 24, 85, 33);
		btnBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				
				//Ritorno alla schermata home
				frame.dispose();
				log.setVisible(true);
			}
		});
		frame.getContentPane().add(btnBack);
		
		btnSfida = new JButton("SEND!");
		btnSfida.setBackground(UIManager.getColor("Button.background"));
		btnSfida.setForeground(Color.BLACK);
		btnSfida.setFont(new Font("Tw Cen MT", Font.PLAIN, 25));
		btnSfida.setBounds(308, 104, 97, 37);
		btnSfida.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if(!ricevuta) {
					String s = evt.getActionCommand();
					BufferedReader in = null;
					int esito = 0;
					String res = null;
					if(user.equals(textField.getText())) {
						JOptionPane.showMessageDialog(frame, "Non puoi sfidare te stesso!", "Error", JOptionPane.ERROR_MESSAGE);
						textField.setText("");
					}
					else {
						
						//Invio richiesta di sfida lettura esito
						String richiesta = s.concat(" ").concat(textField.getText()).concat(" ").concat(user).concat("\n");
						ByteBuffer msg = ByteBuffer.wrap(richiesta.getBytes());
						try {
							while (msg.hasRemaining())
								client.write(msg);
							JOptionPane.showMessageDialog(frame, "Richiesta inviata");
							in = new BufferedReader(new InputStreamReader(client.socket().getInputStream()));
							res = in.readLine();
							if(res.contains("Accettata"))
								esito = 6;
							else
								esito = Integer.parseInt(res);
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
						
						//Valutazione esito
						if(esito == 6) {
							JOptionPane.showMessageDialog(frame, textField.getText() + " ha accettato la tua richiesta");
							frame.dispose();
							System.out.println("Sfida inviata");
							
							//Inizializzazione schermata di gioco
							GameGUI game = new GameGUI(client, log);
							game.initialize();
						}
						else if(esito == 1) {
							JOptionPane.showMessageDialog(frame, "Aggiungi " + textField.getText() + " alla tua lista amici per giocare", "Error", JOptionPane.ERROR_MESSAGE);
							textField.setText("");
						}
						else if(esito == 2) {
							JOptionPane.showMessageDialog(frame, "Utente non registrato", "Error", JOptionPane.ERROR_MESSAGE);
							textField.setText("");
						}
						else if(esito == 3) {
							JOptionPane.showMessageDialog(frame, "Timer scaduto", "Error", JOptionPane.ERROR_MESSAGE);
							textField.setText("");
						}
						else if(esito == 4) {
							JOptionPane.showMessageDialog(frame, "Utente occupato", "Error", JOptionPane.ERROR_MESSAGE);
							textField.setText("");
						}
						else if(esito == 5) {
							JOptionPane.showMessageDialog(frame, "Richiesta rifiutata", "Error", JOptionPane.ERROR_MESSAGE);
							textField.setText("");
						}
						else if(esito == 7) {
							JOptionPane.showMessageDialog(frame, textField.getText() + " è offline", "Error", JOptionPane.ERROR_MESSAGE);
							textField.setText("");
						}
					}
				}
				else {
					
					//Ritorno alla schermata home in caso di richiesta pendente
					frame.dispose();
					log.setVisible(true);
				}
			}
		});
		frame.getContentPane().add(btnSfida);
		
		frame.setVisible(true);
	}
}