import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class BeatBox {
	JFrame theFrame;
	JPanel mainPanel;
	@SuppressWarnings("rawtypes")
	JList incomingList;
	JTextField userMessage;
	ArrayList<JCheckBox> checkBoxList;
	int nextNum;
	Vector<String> listVector = new Vector<String>();
	String userName;
	ObjectOutputStream out;
	ObjectInputStream in;
	HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();

	Sequencer sequencer;
	Sequence sequence;
	Sequence mySequence = null;
	Track track;

	String[] instrumentNames = { "Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal",
			"Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap",
			"Low-mid- Tom", "High Agogo", "Open Hi Conga" };

	int[] instruments = { 35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63 };

	public static void main(String[] args) {
		@SuppressWarnings("resource")
		Scanner s = new Scanner(System.in);
		System.out.println("이름을 입력하세요. :");
		new BeatBox().startUp(s.nextLine());

	}

	private void startUp(String userName) {
		this.userName = userName;

		try {
			@SuppressWarnings("resource")
			Socket socket = new Socket("127.0.0.1", 4242);
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			Thread thread = new Thread(new RemoteReader());
			thread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		setUpMidi();
		buildGUI();

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void buildGUI() {
		theFrame = new JFrame("Beat Box");
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel background = new JPanel(new BorderLayout());
		background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		checkBoxList = new ArrayList<JCheckBox>();
		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		JPanel buttonPanel = new JPanel();

		ButtonListener listener = new ButtonListener();
		JButton start = new JButton("Start");
		start.addActionListener(listener);
		start.setActionCommand("start");
		buttonPanel.add(start);

		JButton stop = new JButton("Stop");
		stop.addActionListener(listener);
		stop.setActionCommand("stop");
		buttonPanel.add(stop);

		JButton upTempo = new JButton("Tempo Up");
		upTempo.addActionListener(listener);
		upTempo.setActionCommand("up");
		buttonPanel.add(upTempo);

		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(listener);
		downTempo.setActionCommand("down");
		buttonPanel.add(downTempo);

		JButton serializeItBtn = new JButton("SerializeIt");
		serializeItBtn.addActionListener(listener);
		serializeItBtn.setActionCommand("serialize");
		buttonPanel.add(serializeItBtn);

		JButton restoreBtn = new JButton("Restore");
		restoreBtn.addActionListener(listener);
		restoreBtn.setActionCommand("restore");
		buttonPanel.add(restoreBtn);

		JButton sendBtn = new JButton("Send");
		sendBtn.addActionListener(listener);
		sendBtn.setActionCommand("send");
		buttonPanel.add(sendBtn);

		buttonBox.add(buttonPanel);

		userMessage = new JTextField();
		buttonBox.add(userMessage);

		incomingList = new JList();
		incomingList.addListSelectionListener(new MyListSelectionListener());
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane theList = new JScrollPane(incomingList);
		incomingList.setListData(listVector);
		buttonBox.add(theList);

		Box nameBox = new Box(BoxLayout.Y_AXIS);

		for (int i = 0; i < 16; i++) {
			nameBox.add(new Label(instrumentNames[i]));
		}

		theFrame.getContentPane().add(background);

		GridLayout grid = new GridLayout(16, 16);
		grid.setVgap(1);
		grid.setHgap(2);
		mainPanel = new JPanel(grid);

		for (int i = 0; i < 256; i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkBoxList.add(c);
			mainPanel.add(c);
		}

		background.add(BorderLayout.EAST, buttonBox);
		background.add(BorderLayout.WEST, nameBox);
		background.add(BorderLayout.CENTER, mainPanel);

		theFrame.setBounds(400, 100, 300, 300);
		theFrame.pack();
		theFrame.setVisible(true);

	}

	private void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@SuppressWarnings("static-access")
	public void buildTrackAndStart() {
		int[] trackList = null;

		sequence.deleteTrack(track);
		track = sequence.createTrack();

		for (int i = 0; i < 16; i++) {
			trackList = new int[16];

			int key = instruments[i];

			for (int j = 0; j < 16; j++) {
				JCheckBox jc = checkBoxList.get(j + (16 * i));
				if (jc.isSelected()) {
					trackList[j] = key;
				} else {
					trackList[j] = 0;
				}
			}

			makeTracks(trackList);
			track.add(makeEvent(176, 1, 127, 0, 16));

		}

		track.add(makeEvent(192, 9, 1, 0, 15));

		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}

	}

	private void makeTracks(int[] trackList) {
		for (int i = 0; i < 16; i++) {
			int key = trackList[i];

			if (key != 0) {
				track.add(makeEvent(144, 9, key, 100, i));
				track.add(makeEvent(128, 9, key, 100, i + 1));
			}
		}

	}

	private MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
		ShortMessage message = new ShortMessage();

		try {
			message.setMessage(comd, chan, one, two);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}

		MidiEvent midiEvent = new MidiEvent(message, tick);
		return midiEvent;

	}

	private void changeSequence(boolean[] b) {
		for (int i = 0; i < 256; i++) {
			JCheckBox jc = checkBoxList.get(i);
			if (b[i]) {
				jc.setSelected(true);
			} else {
				jc.setSelected(false);
			}
		}

	}

	class ButtonListener implements ActionListener {

		@SuppressWarnings("resource")
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("start")) {
				buildTrackAndStart();
			} else if (e.getActionCommand().equals("stop")) {
				sequencer.stop();
			} else if (e.getActionCommand().equals("up")) {
				float tempo = sequencer.getTempoFactor();
				sequencer.setTempoInBPM((float) (tempo * 1.3));
				sequencer.stop();
				buildTrackAndStart();
			} else if (e.getActionCommand().equals("down")) {
				float tempo = sequencer.getTempoFactor();
				sequencer.setTempoInBPM((float) (tempo * 0.97));
				sequencer.stop();
				buildTrackAndStart();
			} else if (e.getActionCommand().equals("serialize")) {

				boolean[] checkBeat = new boolean[checkBoxList.size()];

				for (int i = 0; i < checkBoxList.size(); i++) {
					JCheckBox jc = checkBoxList.get(i);
					if (jc.isSelected()) {
						checkBeat[i] = true;
					}
				}

				try {
					FileOutputStream f = new FileOutputStream(new File("beat.ser"));
					ObjectOutputStream o = new ObjectOutputStream(f);
					o.writeObject(checkBeat);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			} else if (e.getActionCommand().equals("restore")) {
				boolean[] b = null;
				try {
					FileInputStream f = new FileInputStream(new File("beat.ser"));
					ObjectInputStream o = new ObjectInputStream(f);
					b = (boolean[]) o.readObject();

					for (int i = 0; i < checkBoxList.size(); i++) {
						if (b[i]) {
							checkBoxList.get(i).setSelected(true);
						} else {
							checkBoxList.get(i).setSelected(false);
						}
					}

				} catch (Exception e1) {
					e1.printStackTrace();
				}
				sequencer.stop();
				buildTrackAndStart();
			} else if (e.getActionCommand().equals("send")) {
				boolean[] b = new boolean[256];

				for (int i = 0; i < 256; i++) {
					JCheckBox jc = checkBoxList.get(i);
					if (jc.isSelected()) {
						b[i] = true;
					}
				}
				try {
					out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
					out.writeObject(b);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				userMessage.setText("");
			}

		}

	}

	public class MyListSelectionListener implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (!e.getValueIsAdjusting()) {
				String selected = (String) incomingList.getSelectedValue();
				if (selected != null) {
					boolean[] b = otherSeqsMap.get(selected);
					changeSequence(b);
					sequencer.stop();
					buildTrackAndStart();
				}
			}

		}

	}

	public class RemoteReader implements Runnable {
		boolean[] checkBoxState = null;
		String nameToShow = null;
		Object obj = null;

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			try {
				while ((obj = in.readObject()) != null) {
					String nameToShow = (String) obj;
					checkBoxState = (boolean[]) in.readObject();
					otherSeqsMap.put(nameToShow, checkBoxState);
					listVector.add(nameToShow);
					incomingList.setListData(listVector);

				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}

		}

	}

}
