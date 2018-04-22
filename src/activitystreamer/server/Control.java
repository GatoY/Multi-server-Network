package activitystreamer.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import activitystreamer.util.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import activitystreamer.util.Settings;

public class Control extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static ArrayList<Connection> connections;
	private static boolean term = false;
	private static Listener listener;

	protected static Control control = null;
	private static Connection interact;

	public static Control getInstance() {
		if (control == null) {
			control = new Control();
		}
		return control;
	}

	private Control() {
		// initialize the connections array
		connections = new ArrayList<>();
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
				Connection interact = outgoingConnection(
						new Socket(Settings.getRemoteHostname(), Settings.getRemotePort()));
				Message.authenticate(interact);
			} catch (IOException e) {
				log.error("failed to make connection to " + Settings.getRemoteHostname() + ":"
						+ Settings.getRemotePort() + " :" + e);
				System.exit(-1);
			}
		}
	}

	/**
	 * Processing incoming messages from the connection. Return true if the
	 * connection should close.
	 *
	 * @param con
	 * @param msg
	 *            result JSON string
	 * @return
	 */
	public synchronized boolean process(Connection con, String msg) {
		JSONObject request;
		try {
			request = (JSONObject) new JSONParser().parse(msg);
		} catch (Exception e) {
			Message.invalidMsg(con, "the received message is not in valid format");
			return true;
		}

		if (request.get("command") == null) {
			Message.invalidMsg(con, "the received message did not contain a command");
			return true;
		}

		String command = (String) request.get("command");

		switch (command) {
		case Message.INVALID_MESSAGE:
			return true;
		case Message.AUTHENTICATE:
			if (request.get("secret") == null) {
				Message.invalidMsg(con, "the received message did not contain a secret");
			}
			String secret = (String) request.get("secret");
			if (!secret.equals(Settings.serverSecret)) {
				// if the secret is incorrect
				Message.authenticationFail(con, "the supplied secret is incorrect: " + secret);
			} else if (Settings.isIsRemoteAuthenticated()) {
				Message.invalidMsg(con, "the server has already successfully authenticated");
			} else {
				Settings.setIsRemoteAuthenticated(true);
				// No reply if the authentication succeeded.
			}
			break;
		case Message.AUTHENTICATION_FAIL:
			return true;
		case Message.LOGIN:
			return !login(con, request);
		case Message.LOGOUT:
			return true;
		case Message.REGISTER:
			return !register(con, request);
		case Message.ACTIVITY_MESSAGE:
			if (!request.containsKey("username")) {
				return Message.authenticationFail(con, "the message did not contain a username");
			}
			if (!request.containsKey("secret")) {
				return Message.authenticationFail(con, "the message did not contain a secret");
			}
			String activityUsername = (String) request.get("username");
			String activityPassword = (String) request.get("secret");
			if (activityUsername.equals("anonymous")) {
				if (activityUsername.equals(Settings.getUsername())) {
					return broadcastActivity(this.getConnections(), (JSONObject) request.get("activity"));
				}
			} else {
				if (!activityUsername.equals(Settings.getUsername())
						|| !activityPassword.equals(Settings.getSecret())) {
					return Message.authenticationFail(con, "the supplied secret is incorrect: " + activityPassword);
				}
				return broadcastActivity(this.getConnections(), (JSONObject) request.get("activity"));
			}
			return true;
		}
		return true;
	}

	public synchronized boolean login(Connection con, JSONObject request) {
		JSONObject response = new JSONObject();
		if (request.containsKey("username") && request.containsKey("password")) {
			String username = (String) request.get("username");
			String password = (String) request.get("password");
			if (validate(username, password)) {
				Settings.setUsername(username);
				Settings.setSecret(password);
				response.put("command", Message.LOGIN_SUCCESS);
				con.writeMsg(response.toJSONString());
				return true;
			} else {
				response.put("command", Message.LOGIN_FAILED);
				response.put("info", "attempt to login with wrong secret");
			}

		} else {
			response.put("command", Message.INVALID_MESSAGE);
		}
		con.writeMsg(response.toJSONString());
		return false;
	}

	public boolean validate(String username, String password) {
		return true;
	}

	public synchronized boolean register(Connection con, JSONObject request) {
		JSONObject response = new JSONObject();
		if (request.containsKey("username") && request.containsKey("password")) {
			String username = (String) request.get("username");
			String password = (String) request.get("password");
			if (lockRequest(username, password)) {
				response.put("command", Message.REGISTER_SUCCESS);
				response.put("info", "register success for "+username);
				con.writeMsg(response.toJSONString());
				//success
				return true;
			} else {
				response.put("command", Message.REGISTER_FAILED);
				response.put("info", username+" is already registered with the system");
				con.writeMsg(response.toJSONString());
				//failed
				return false;
			}

		} else {
			response.put("command", Message.INVALID_MESSAGE);
		}
		return false;
	}
	
	public synchronized boolean lockRequest(String username, String password) {
		JSONObject broadcast = new JSONObject();
		broadcast.put("command", "LOCK_REQUEST");
		broadcast.put("username", username);
		broadcast.put("secret", password);
		return true;
	}
	
	private boolean broadcastActivity(List<Connection> connections, JSONObject activity) {
		for (Connection c : connections) {
			Message.activityBroadcast(c, activity);
		}
		return false;
	}

	/**
	 * The connection has been closed by the other party.
	 *
	 * @param con
	 */
	public synchronized void connectionClosed(Connection con) {
		if (!term) {
			connections.remove(con);
		}
	}

	/**
	 * A new incoming connection has been established, and a reference is returned
	 * to it
	 *
	 * @param s
	 * @return
	 * @throws IOException
	 */
	public synchronized Connection incomingConnection(Socket s) throws IOException {
		log.debug("incoming connection: " + Settings.socketAddress(s));
		Connection c = new Connection(s);
		connections.add(c);
		return c;

	}

	/**
	 * A new outgoing connection has been established, and a reference is returned
	 * to it
	 *
	 * @param s
	 * @return
	 * @throws IOException
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
		if (interact != null) {
			while (!term) {
				// do something with 5 second intervals in between
				Socket tem = interact.getSocket();
				JSONObject announce = new JSONObject();
				announce.put("command", "SERVER_ANNOUNCE");
				announce.put("id", Settings.getServerId());
				announce.put("load", connections.size());
				announce.put("hostname", tem.getLocalAddress());
				announce.put("port", tem.getLocalPort());
				interact.writeMsg(announce.toJSONString());
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
