package de.bos.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import de.ros.fux.model.FusionBoard;
import de.ros.fux.tools.*;

public class GameRoom extends Thread {

	private List<Socket> players = new ArrayList<Socket>();
	private boolean started = false;
	private boolean finished = false;
	private Config conf;
	private List<ObjectInputStream> ins = new ArrayList<ObjectInputStream>();
	private List<ObjectOutputStream> ous = new ArrayList<ObjectOutputStream>();
	private int playersTurn = 1;
	private long startTime;
	private final long TIMEOUT = 120000;
	FusionBoard board;

	public GameRoom(Config conf) {

		this.conf = conf;
		startTime = System.currentTimeMillis();
	}

	public void addPlayer(Socket player) {

		try {
			if (players.size() < conf.getMAX_PLAYER()) {

				ins.add(new ObjectInputStream(player.getInputStream()));
				ous.add(new ObjectOutputStream(player.getOutputStream()));

				players.add(player);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (players.size() == conf.getMAX_PLAYER())
			startGame();
	}

	public boolean isFull() {

		if (players.size() == conf.getMAX_PLAYER())
			return true;
		return false;
	}

	public boolean isStarted() {

		return started;
	}

	public boolean isFinished() {

		return finished;
	}

	private void startGame() {

		started = true;

		this.start();

	}

	private void broadcastBoard() throws IOException {

		for (int i = 0; i < conf.getMAX_PLAYER(); i++) {

			ous.get(i).writeObject(board);
			ous.get(i).flush();

		}

	}

	private int nextTurn() throws ClassNotFoundException, IOException {

		int won = 0;

		startTime = System.currentTimeMillis();

		while (true) {

			if (System.currentTimeMillis() <= (startTime + TIMEOUT)) {

				int[] coord = (int[]) (ins.get(0).readObject());
				if (!coord.equals(null) || coord.length == 2) {

					if (board.addToCell(coord[0], coord[1], playersTurn)) {

						broadcastBoard();
						if (playersTurn == conf.getMAX_PLAYER())
							playersTurn = 0;
						else
							playersTurn++;
						won = board.checkWin();
						// TODO check auf timeout
						break;

					}
				}
			} else {
				
				board.removePlayer(playersTurn);
				//TODO
				break;
			}
		}

		return won;
	}

	private void stopThread() {

		for (ObjectInputStream in : ins) {

			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		for (ObjectOutputStream ou : ous) {

			try {
				ou.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		for (Socket soc : players) {

			try {
				soc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		finished = true;
	}

	@Override
	public void run() {

		board = new FusionBoard(conf.getSTANDARD_BOARD_WIDTH(), conf.getSTANDARD_BOARD_HEIGTH(), conf.getMAX_PLAYER(),
				conf.getWIN_PERCENTAGE());

		try {

			broadcastBoard();

			while (true) {

				if (nextTurn() != 0)
					stopThread();

			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}
}
