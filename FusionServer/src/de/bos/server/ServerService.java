package de.bos.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import de.ros.fux.tools.Config;

public class ServerService {

	private ServerSocket socket;
	private int port;

	private List<Socket> addresses = new ArrayList<Socket>();
	private List<GameRoom> gameRooms = new ArrayList<GameRoom>();

	public ServerService(int port, InetAddress address) {

		this.port = port;
	}

	private void deliverGameRoom(Socket client) {

		boolean added = false;

		for (GameRoom gr : gameRooms) {

			if (!gr.isStarted() && !gr.isFull()) {

				gr.addPlayer(client);
				added = true;
			}
		}

		if (!added) {

			try {

				ObjectInputStream in = new ObjectInputStream(client.getInputStream());
				Config conf;
				conf = (Config) (in.readObject());
				GameRoom game = new GameRoom(conf);
				game.addPlayer(client);
				gameRooms.add(game);
				
			} catch (ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private void cleanUpRoomList(){
		
		for(GameRoom gr : gameRooms){
			
			if(gr.isFinished()){
				
				gameRooms.remove(gr);
			}
		}
	}
	
	public void startServer() {

		try {
			socket = new ServerSocket(port);

			while (true) {

				Socket client = socket.accept();
				addresses.add(client);
				deliverGameRoom(client);
				
				cleanUpRoomList();

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
