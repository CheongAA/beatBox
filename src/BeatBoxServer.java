import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

public class BeatBoxServer {
	ArrayList<ObjectOutputStream> clientOutputStreams;

	public static void main(String[] args) {
		new BeatBoxServer().go();

	}
	
	private void go() {
		clientOutputStreams = new ArrayList<ObjectOutputStream>();
		
		
		try {
			ServerSocket serverSocket = new ServerSocket(4242);
			
			while(true) {
				Socket clientSocket = serverSocket.accept();
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
				clientOutputStreams.add(out);
				
				Thread t = new Thread(new ClientHandler(clientSocket));
				t.start();
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

	public class ClientHandler implements Runnable{
		ObjectInputStream in;
		Socket clientSocket;
		
		public ClientHandler(Socket socket) {
			clientSocket = socket;
			try {
				in = new ObjectInputStream(clientSocket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		@Override
		public void run() {
			Object o2 = null;
			Object o1 = null;
			
			try {
				while((o1 = in.readObject())!=null) {
					o2 = in.readObject();
					
					tellEveryone(o1,o2);
				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
			
		}
	}

	public void tellEveryone(Object o1, Object o2) {
		Iterator it = clientOutputStreams.iterator();
		
		while(it.hasNext()) {
			ObjectOutputStream out = (ObjectOutputStream) it.next();
			try {
				out.writeObject(o1);
				out.writeObject(o2);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	}

}
