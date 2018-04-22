package activitystreamer.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import activitystreamer.util.Message;
import activitystreamer.util.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import activitystreamer.util.Settings;

public class Control extends Thread {
    private static final Logger log = LogManager.getLogger();
    private static List<Connection> clientConnections;
    private static boolean term = false;
    private static Listener listener;

    protected static Control control = null;
    private static Connection parentServer, lChildServer, rChildServer;

    public static Control getInstance() {
        if (control == null) {
            control = new Control();
        }
        return control;
    }

    private Control() {
        // initialize the clientConnections array
        clientConnections = new ArrayList<>();
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
                Connection c = outgoingConnection(new Socket(Settings.getRemoteHostname(), Settings.getRemotePort()));

                Message.authenticate(c);

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
     * @param msg result JSON string
     * @return
     */
    public synchronized boolean process(Connection con, String msg) {
        JSONObject request;
        try {
            request = (JSONObject) new JSONParser().parse(msg);
        } catch (Exception e) {
            return Message.invalidMsg(con, "the received message is not in valid format");
        }

        if (request.get("command") == null) {
            return Message.invalidMsg(con, "the received message did not contain a command");
        }

        String command = (String) request.get("command");

        switch (command) {
            case Message.INVALID_MESSAGE:
                return true;
            case Message.AUTHENTICATE:
                if (request.get("secret") == null) {
                    return Message.invalidMsg(con, "the received message did not contain a secret");
                }
                String secret = (String) request.get("secret");
                if (!secret.equals(Settings.serverSecret)) {
                    // if the secret is incorrect
                    return Message.authenticationFail(con, "the supplied secret is incorrect: " + secret);
                } else if (Settings.isRemoteAuthenticated()) {
                    return Message.invalidMsg(con, "the server has already successfully authenticated");
                }
                // No reply if the authentication succeeded.
                Settings.setRemoteAuthenticated(true);

//TODO
//                if (lChildServer == null) {
//                    lChildServer = con;
//                } else if (rChildServer == null) {
//                    rChildServer = con;
//                }


                return false;
            case Message.AUTHENTICATION_FAIL:
                return true;
            case Message.REGISTER:
                return register(con, request);
            case Message.LOGIN:
                return login(con, request);
            case Message.LOGOUT:
                logout(con);
                return true;
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
                        return broadcastActivity(con, (JSONObject) request.get("activity"));
                    }
                } else {
                    if (!activityUsername.equals(Settings.getUsername())
                            || !activityPassword.equals(Settings.getUserSecret())) {
                        return Message.authenticationFail(con, "the supplied secret is incorrect: " + activityPassword);
                    }
                    return broadcastActivity(con, (JSONObject) request.get("activity"));
                }
                return true;
        }
        return true;
    }

    private synchronized boolean register(Connection con, JSONObject request) {
        if (!request.containsKey("username") || !request.containsKey("secret")) {
            return true;
        }
        String username = (String) request.get("username");
        String secret = (String) request.get("secret");
        if (!isUserRegistered()) {
            return Message.registerFailed(con, username + " is already registered with the system"); // true
        } else {
            User user = new User(username, secret);
            user.setHostname(con.getSocket().getLocalAddress().toString()); //TODO check???
            user.setPort(con.getSocket().getLocalPort()); // TODO check getLocalPort getPort
            user.setLogin(false);
            Settings.getClientList().add(user);
            return Message.registerSuccess(con, "register success for " + username); // false
        }
    }

    private boolean isUserRegistered() {
        //TODO determine whether the user has registered before
        return false;
    }

    private synchronized boolean login(Connection con, JSONObject request) {
        if (request.containsKey("username") && request.containsKey("secret")) {
            String username = (String) request.get("username");
            String secret = (String) request.get("secret");

            boolean foundUser = false;

            for (User user : Settings.getClientList()) {
                if (user.getUserName().equals(username)) {
                    foundUser = true;
                    if (user.getPassword().equals(secret)) {
                        user.setLogin(true);
                        return Message.loginSuccess(con, "logged in as user " + username);
                    }
                }
            }
            if (!foundUser) {
                return Message.loginFailed(con, "attempt to login with wrong secret");
            }
        } else {
            return Message.invalidMsg(con, "missed username or secret");
        }
        return false;
    }

    private void logout(Connection con) {
        for (User user : Settings.getClientList()) {
            if (user.getHostname().equals(con.getSocket().getLocalAddress().toString()) && user.getPort() == con.getSocket().getLocalPort()) {
                user.setLogin(false);
            }
        }
    }


    private boolean broadcastActivity(Connection sourceConnection, JSONObject activity) {
        for (Connection c : this.getClientConnections()) {
            Message.activityBroadcast(c, activity);
        }
        // broadcast activity to other servers except which it comes from
        if (parentServer != sourceConnection) {
            Message.activityBroadcast(parentServer, activity);
        }
        if (lChildServer != sourceConnection) {
            Message.activityBroadcast(lChildServer, activity);
        }
        if (rChildServer != sourceConnection) {
            Message.activityBroadcast(rChildServer, activity);
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
            clientConnections.remove(con);
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
        clientConnections.add(c);
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
        clientConnections.add(c);
        return c;
    }


    @Override
    public void run() {
        log.info("using activity interval of " + Settings.getActivityInterval() + " milliseconds");
        while (!term) {
            // do something with 5 second intervals in between
            if (parentServer != null) {
                Message.serverAnnounce(parentServer, clientConnections.size());
            }
            if (lChildServer != null) {
                Message.serverAnnounce(lChildServer, clientConnections.size());
            }
            if (rChildServer != null) {
                Message.serverAnnounce(rChildServer, clientConnections.size());
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
        log.info("closing " + clientConnections.size() + " clientConnections");
        // clean up
        for (Connection connection : clientConnections) {
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

    public final List<Connection> getClientConnections() {
        return clientConnections;
    }

}
