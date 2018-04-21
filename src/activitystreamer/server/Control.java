package activitystreamer.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

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
                outgoingConnection(new Socket(Settings.getRemoteHostname(), Settings.getRemotePort()));
            } catch (IOException e) {
                log.error("failed to make connection to " + Settings.getRemoteHostname() + ":"
                        + Settings.getRemotePort() + " :" + e);
                System.exit(-1);
            }
        }
    }


    /**
     * Processing incoming messages from the connection. Return true if the connection should close.
     *
     * @param con
     * @param msg
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

        if (command.equals(Message.INVALID_MESSAGE)) {
            return true;
        } else if (command.equals(Message.AUTHENTICATION_FAIL)) {
            return true;
        } else if (command.equals(Message.LOGIN)) {
            return !login(con, request);
        } else if (command.equals(Message.LOGOUT)) {
            return true;
        } else if (command.equals(Message.ACTIVITY_MESSAGE)) {
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
                        Message.authenticationFail(con, "");
                        return true;
                    }
                    String activityPassword = (String) request.get("password");
                    if (!activityUsername.equals(Settings.getUsername())
                            || !activityPassword.equals(Settings.getSecret())) {
                        Message.authenticationFail(con, "the supplied secret is incorrect: " + activityPassword);
                        return true;
                    }
                    broadcast();
                    return false;
                }
            } else {
                Message.authenticationFail(con, "the message did not contain a username"); //TODO
                return true;
            }
        } else if (command.equals(Message.AUTHENTICATE)) {
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
        }
        return true;
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
     * A new incoming connection has been established, and a reference is returned to it
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
     * A new outgoing connection has been established, and a reference is returned to it
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
        while (!term) {
            // do something with 5 second intervals in between
            for (Connection connection : connections) {
                Message.serverAnnounce(connection, connections.size());
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
