package activitystreamer.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import activitystreamer.util.Settings;

public class Control extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static ArrayList<Connection> connections;
	private static boolean term = false;
	private static Listener listener;

	protected static Control control = null;

	public static Control getInstance() {
		if (control == null) {
			control = new Control();
		}
		return control;
	}

	public Control() {
		// initialize the connections array
		connections = new ArrayList<Connection>();
		// start a listener
		try {
			listener = new Listener();
		} catch (IOException e1) {
			log.fatal("failed to startup a listening thread: " + e1);
			System.exit(-1);
		}
	}

	public void initiateConnection() {
		// make a connection to another server if remote hostname is supplied
		if (Settings.getRemoteHostname() != null) {
			try {
				outgoingConnection(new Socket(Settings.getRemoteHostname(), Settings.getRemotePort()));
			} catch (IOException e) {
				log.error("failed to make connection to " + Settings.getRemoteHostname() + ":"
						+ Settings.getRemotePort() + " :" + e);
				System.exit(-1);
			}
		}
	}

	/*
	 * Processing incoming messages from the connection. Return true if the
	 * connection should close.
	 */
	public synchronized boolean process(Connection con, String msg) {
		JSONObject request = (JSONObject) JSONValue.parse(msg);

		String commandFromClient = (String) request.get("command");

		if (commandFromClient.equals("INVALID_MESSAGE")) {
			return true;
		} else if (commandFromClient.equals("AUTHENTICATION_FAIL")) {
			return true;
		} else if (commandFromClient.equals("LOGIN")) {
			return !login(con, request);
		} else if (commandFromClient.equals("LOGOUT")) {
			return true;
		} else if (commandFromClient.equals("ACTIVITY_MESSAGE")) {
			if (request.containsKey("username")) {
				String activityUsername = (String) request.get("username");
				if (activityUsername.equals("anonymous")) {
					if (activityUsername.equals(Settings.getUsername())) {
						broadcast();
						return false;
					}
				} else {
					// no password.
					if (!request.containsKey("password")) {
						authentication_fail(con);
						return true;
					}
					String activityPassword = (String) request.get("password");
					if (!activityUsername.equals(Settings.getUsername())
							|| !activityPassword.equals(Settings.getSecret())) {
						authentication_fail(con);
						return true;
					}
					broadcast();
					return false;
				}
			} else {
				authentication_fail(con);
				return true;
			}
		}
		return true;
	}

	public synchronized void authentication_fail(Connection con) {
		JSONObject response = new JSONObject();
		response.put("command", "AUTHENTICATION_FAIL");
		con.writeMsg(response.toJSONString());
	}

	public synchronized void broadcast() {
		return;
	}

	public synchronized boolean login(Connection con, JSONObject request) {
		JSONObject response = new JSONObject();
		if (request.containsKey("username") && request.containsKey("password")) {
			String username = (String) request.get("username");
			String password = (String) request.get("password");
			if (validate(username, password)) {
				Settings.setUsername(username);
				Settings.setSecret(password);
				response.put("command", "LOGIN_SUCCESS");
				con.writeMsg(response.toJSONString());
				return true;
			} else {
				response.put("command", "LOGIN_FAILED");
				response.put("info", "attempt to login with wrong secret");
			}

		} else {
			response.put("command", "INVALID_MESSAGE");
		}
		con.writeMsg(response.toJSONString());
		return false;
	}

	public boolean validate(String username, String password) {
		return true;
	}

	/*
	 * The connection has been closed by the other party.
	 */
	public synchronized void connectionClosed(Connection con) {
		if (!term)
			connections.remove(con);
	}

	/*
	 * A new incoming connection has been established, and a reference is returned
	 * to it
	 */
	public synchronized Connection incomingConnection(Socket s) throws IOException {
		log.debug("incomming connection: " + Settings.socketAddress(s));
		Connection c = new Connection(s);
		connections.add(c);
		return c;

	}

	/*
	 * A new outgoing connection has been established, and a reference is returned
	 * to it
	 */
	public synchronized Connection outgoingConnection(Socket s) throws IOException {
		log.debug("outgoing connection: " + Settings.socketAddress(s));
		Connection c = new Connection(s);
		connections.add(c);
		return c;

	}

	@Override
	public void run() {
		log.info("using activity interval of " + Settings.getActivityInterval() + " milliseconds");
		while (!term) {
			// do something with 5 second intervals in between
			for (Connection connection : connections) {
				if(connection.getSocket())
				JSONObject announce = new JSONObject();
				announce.put("command", "SERVER_ANNOUNCE");
				announce.put("id", Settings.getServerId());
				announce.put("load", connections.size());
				connection.writeMsg(announce.toJSONString());
			}
			try {
				Thread.sleep(Settings.getActivityInterval());
			} catch (InterruptedException e) {
				log.info("received an interrupt, system is shutting down");
				break;
			}
			if (!term) {
				log.debug("doing activity");
				term = doActivity();
			}

		}
		log.info("closing " + connections.size() + " connections");
		// clean up
		for (Connection connection : connections) {
			connection.closeCon();
		}
		listener.setTerm(true);
	}

	public boolean doActivity() {
		return false;
	}

	public final void setTerm(boolean t) {
		term = t;
	}

	public final ArrayList<Connection> getConnections() {
		return connections;
	}
}
