import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameGUI implements ActionListener {

	private JFrame frame;
	private JFrame log;
	private String parola;
	private JTextField textField;
	private SocketChannel client;
	private JLabel lblParola;
	private JLabel lblHaiSecondi;
	private JLabel lblParoleTradotte;
	private JButton btnStart;
	private JButton btnInvia;
	private JLabel lblTimer;
	private int nTradotte;
	private boolean pressed;
	
	public GameGUI(SocketChannel client, JFrame log) {	
		this.client = client;
		this.log = log;
		nTradotte = 0;
		
		//Variabile utile per segnalare la pressione del pulsante una volta scaduto il timer
		pressed = false;
	}
	
	public void initialize() {
		
		frame = new JFrame();
		frame.setBounds(100, 100, 394, 278);
		frame.getContentPane().setBackground(Color.WHITE);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		lblHaiSecondi = new JLabel("Translate in english!");
		lblHaiSecondi.setForeground(Color.DARK_GRAY);
		lblHaiSecondi.setFont(new Font("Tahoma", Font.PLAIN, 20));
		lblHaiSecondi.setHorizontalAlignment(SwingConstants.CENTER);
		lblHaiSecondi.setBounds(46, 38, 280, 53);
		frame.getContentPane().add(lblHaiSecondi);
		
		btnStart = new JButton("START");
		btnStart.setForeground(Color.WHITE);
		btnStart.setBackground(Color.BLUE);
		btnStart.setFont(new Font("Tw Cen MT", Font.BOLD, 30));
		btnStart.setBounds(123, 130, 125, 65);
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				
				//Invio prima parola di inizio sfida
				String parola = null;
				String inizio = new String("Inizio\n");
				BufferedReader in = null;
				ByteBuffer msg = ByteBuffer.wrap(inizio.getBytes());
				try {
					while(msg.hasRemaining())
						client.write(msg);
					
					//Ricezione prima parola da tradurre
					in = new BufferedReader(new InputStreamReader(client.socket().getInputStream()));
					parola = in.readLine();
					frame.dispose();
					Sfida(parola);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		frame.getContentPane().add(btnStart);

		frame.setVisible(true);
	}
	
	public void Sfida(String parola) {
		
		this.parola = parola;
		
		frame = new JFrame();
		frame.getContentPane().setBackground(Color.WHITE);
		frame.setBounds(100, 100, 440, 303);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		lblParola = new JLabel(this.parola);
		lblParola.setFont(new Font("Tahoma", Font.PLAIN, 20));
		lblParola.setBounds(36, 28, 247, 31);
		frame.getContentPane().add(lblParola);
		
		textField = new JTextField();
		textField.setBackground(SystemColor.menu);
		textField.setFont(new Font("Tahoma", Font.PLAIN, 17));
		textField.setBounds(36, 98, 262, 41);
		frame.getContentPane().add(textField);
		textField.setColumns(20);
		
		btnInvia = new JButton("SEND");
		btnInvia.setBackground(Color.BLUE);
		btnInvia.setForeground(Color.WHITE);
		btnInvia.setFont(new Font("Tahoma", Font.PLAIN, 30));
		btnInvia.setBounds(268, 172, 126, 55);
		btnInvia.setEnabled(true);
		btnInvia.addActionListener(this);
		frame.getContentPane().add(btnInvia);
		
		lblParoleTradotte = new JLabel("0/13");
		lblParoleTradotte.setFont(new Font("Tw Cen MT", Font.PLAIN, 30));
		lblParoleTradotte.setBounds(331, 100, 85, 33);
		frame.getContentPane().add(lblParoleTradotte);
		
		lblTimer = new JLabel("60 seconds left");
		lblTimer.setForeground(Color.DARK_GRAY);
		lblTimer.setFont(new Font("Tw Cen MT", Font.PLAIN, 25));
		lblTimer.setBounds(36, 188, 176, 31);
		frame.getContentPane().add(lblTimer);
		
		//Timer lato client per cliccare su "SEND!" una volta scaduti i 60 secondi
		new javax.swing.Timer(1000, new ActionListener() {
			Integer sec = 60;
			public void actionPerformed(ActionEvent evt) {
				if(!pressed) {
					if(sec > 0) {
						if(sec == 2) {
							sec--;
							lblTimer.setText(sec.toString() + " second left");
						}
						else {
							sec--;
							lblTimer.setText(sec.toString() + " seconds left");
						}
					}
					else {
						lblTimer.setText("Finished!");
						try {
							Thread.sleep(500);
						} 
						catch (InterruptedException e) {
							e.printStackTrace();
						}
						btnInvia.doClick();
						pressed = true;
					}
				}
			}
		}).start();		
		
		frame.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		
		if(!pressed) {
			nTradotte++;
			lblParoleTradotte.setText(new String(nTradotte + "/13"));
		}
		String itaWord = null;
		String engWord = new String(textField.getText().concat("\n"));
		if(engWord.isBlank())
			engWord = new String("Vuoto\n");
		ByteBuffer msg = ByteBuffer.wrap(engWord.getBytes());
		BufferedReader in;
		try {
			
			//Scrittura parola tradotta
			while(msg.hasRemaining())
				client.write(msg);
			
			//Lettura nuova parola da tradurre e analisi
			in = new BufferedReader(new InputStreamReader(client.socket().getInputStream()));
			itaWord = in.readLine();
			
			//Lettura risultato in caso di termine della sfida
			if(itaWord.contains("Fine") || itaWord.contains("scaduto")) {
				if(itaWord.contains("Fine")) {
					pressed = true;
					lblTimer.setText("Finished!");
					JOptionPane.showMessageDialog(frame, "Fine!");
				}
				else if(itaWord.contains("scaduto"))
					JOptionPane.showMessageDialog(frame, "Tempo scaduto!");
				in = new BufferedReader(new InputStreamReader(client.socket().getInputStream()));
				String risultato = in.readLine();
				JOptionPane.showMessageDialog(frame, risultato);
				
				//Ritorno alla schermata home
				frame.dispose();
				log.setVisible(true);
			}
			else {
				lblParola.setText(itaWord);
				textField.setText("");
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}