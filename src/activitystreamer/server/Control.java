package activitystreamer.server;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;

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
    private static Connection parentConnection, lChildConnection, rChildConnection;
    private static Map<Connection, Integer> loadMap = new HashMap<>();
    private static List<User> userList = new ArrayList<>(); // the global registered users TODO
    private static Map<Connection, Boolean> loginOrNot = new HashMap<>();

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
                return authenticateIncomingConnection(con, request);
            case Message.AUTHENTICATION_FAIL:
                return authenticationFail();
            case Message.REGISTER:
                return register(con, request);
            case Message.LOCK_REQUEST:
                return dealLockRequest(con, request);
            case Message.LOCK_DENIED:
                dealLockDenied(con, request);
                return false;
            case Message.LOCK_ALLOWED:
                if (dealLockAllowed(con, request)) {
                    return true;
                }
                addUser(con, (String) request.get("username"), (String) request.get("secret"));
                return Message.registerSuccess(con, "register success for " + request.get("username"));
            case Message.LOGIN:
                return login(con, request);
            case Message.LOGOUT:
                return logout(con);
            case Message.ACTIVITY_MESSAGE:
                return onReceiveActivityMessage(con, request);
            case Message.SERVER_ANNOUNCE:
                return onReceiveServerAnnounce(con, request);
        }
        return true;
    }

    private synchronized boolean authenticateIncomingConnection(Connection con, JSONObject request) {
        if (request.get("secret") == null) {
            return Message.invalidMsg(con, "the received message did not contain a secret");
        }
        String secret = (String) request.get("secret");
        if (!secret.equals(Settings.getServerSecret())) {
            // if the secret is incorrect
            return Message.authenticationFail(con, "the supplied secret is incorrect: " + secret);
        } else if (lChildConnection == con || rChildConnection == con) {
            return Message.invalidMsg(con, "the server has already successfully authenticated");
        }
        // No reply if the authentication succeeded.
        clientConnections.remove(con);
        if (lChildConnection == null) {
            lChildConnection = con;
        } else if (rChildConnection == null) {
            rChildConnection = con;
        } else {
            log.debug("the connection was refused");
        }

        return false;
    }

    private synchronized boolean authenticationFail() {
        if (parentConnection != null && parentConnection.isOpen()) {
            parentConnection.closeCon();
            parentConnection = null;
        }
        return true;
    }

    private synchronized boolean register(Connection con, JSONObject request) {
        if (!request.containsKey("username") || !request.containsKey("secret")) {
            Message.invalidMsg(con, "The message is incorrect");
            return true;
        }
        String username = (String) request.get("username");
        String secret = (String) request.get("secret");

        if (loginOrNot.containsKey(con)) {
            Message.invalidMsg(con, "You have already logged in.");
            return true;
        }

        if (isUserRegisteredLocally(username)) {
            return Message.registerFailed(con, username + " is already registered with the system"); // true
        } else {
            if (parentConnection != null || lChildConnection != null || rChildConnection != null) {
                userList.add(new User(username, secret));
                if (parentConnection != null) {
                    Message.lockRequest(parentConnection, username, secret);
                }
                if (lChildConnection != null) {
                    Message.lockRequest(lChildConnection, username, secret);
                }
                if (rChildConnection != null) {
                    Message.lockRequest(rChildConnection, username, secret);
                }
                return false;
            } else {
                return Message.registerSuccess(con, "register success for " + username);
            }
        }
    }

    private boolean dealLockAllowed(Connection con, JSONObject request) {
        if (!(con.equals(parentConnection) || con.equals(lChildConnection) || con.equals(rChildConnection))) {
            return Message.invalidMsg(con, "The connection has not authenticated");
        }
        String username = (String) request.get("username");
        String secret = (String) request.get("secret");
        if (con.equals(parentConnection)) {
            if (lChildConnection != null) {
                Message.lockAllowed(lChildConnection, username, secret);
            }
            if (rChildConnection != null) {
                Message.lockAllowed(rChildConnection, username, secret);
            }
        } else {
            if (parentConnection != null) {
                Message.lockAllowed(parentConnection, username, secret);
            }
        }
        return false;
    }

    private void dealLockDenied(Connection con, JSONObject request) {
        if (!(con.equals(parentConnection) || con.equals(lChildConnection) || con.equals(rChildConnection))) {
            Message.invalidMsg(con, "The connection has not authenticated");
        }
        String username = (String) request.get("username");
        String secret = (String) request.get("secret");
        for (User user : userList) {
            if (user.getUserName().equals(username) & user.getPassword().equals(secret)) {
                userList.remove(user);
            }
        }
        if (con.equals(parentConnection)) {
            if (lChildConnection != null) {
                Message.lockDenied(lChildConnection, username, secret);
            }
            if (rChildConnection != null) {
                Message.lockDenied(rChildConnection, username, secret);
            }
        } else {
            if (parentConnection != null) {
                Message.lockDenied(parentConnection, username, secret);
            }
        }

    }

    private boolean dealLockRequest(Connection con, JSONObject request) {
        if (!(con.equals(parentConnection) || con.equals(lChildConnection) || con.equals(rChildConnection))) {
            return Message.invalidMsg(con, "The connection has not authenticated");
        }
        String username = (String) request.get("username");
        String secret = (String) request.get("secret");
        if (isUserRegisteredLocally(username)) {
            for (User user : userList) {
                if (user.getUserName().equals(username) & user.getPassword().equals(secret)) {
                    userList.remove(user);
                }
            }
            if (lChildConnection != null) {
                Message.lockDenied(lChildConnection, username, secret);
            }
            if (rChildConnection != null) {
                Message.lockDenied(rChildConnection, username, secret);
            }
            if (parentConnection != null) {
                Message.lockDenied(parentConnection, username, secret);
            }
        } else {
            addUser(con, username, secret);
            if (con.equals(parentConnection)) {
                if (lChildConnection == null & rChildConnection == null) {
                    Message.lockAllowed(parentConnection, username, secret);
                    return false;
                }
                if (lChildConnection != null) {
                    Message.lockRequest(lChildConnection, username, secret);
                }
                if (rChildConnection != null) {
                    Message.lockRequest(rChildConnection, username, secret);
                }

            } else {
                if (parentConnection != null) {
                    Message.lockRequest(parentConnection, username, secret);
                } else {
                    Message.lockAllowed(lChildConnection, username, secret);
                    Message.lockAllowed(rChildConnection, username, secret);
                }
            }
        }
        return false;
    }

    private void addUser(Connection con, String username, String secret) {
        User user = new User(username, secret);
        user.setLocalSocketAddress(con.getSocket().getLocalSocketAddress());
        user.setLogin(false);
        userList.add(user);
    }

    private boolean isUserRegisteredLocally(String username) {
        boolean flag = false;
        for (User user : userList) {
            if (user.getUserName().equals(username)) {
                flag = true;
            }
        }
        return flag;
    }


    private boolean onReceiveServerAnnounce(Connection con, JSONObject request) {
        loadMap.put(con, (Integer) request.get("load"));
        if (con != parentConnection) {
            parentConnection.writeMsg(request.toJSONString());
        }
        if (con != lChildConnection) {
            lChildConnection.writeMsg(request.toJSONString());
        }
        if (con != rChildConnection) {
            rChildConnection.writeMsg(request.toJSONString());
        }
        return false;

    }

    private Connection checkOtherLoads() {
        Iterator<Map.Entry<Connection, Integer>> it = loadMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Connection, Integer> entry = it.next();
            if (clientConnections.size() - entry.getValue() >= 2) {
                return entry.getKey();
            }
        }
        return null;
    }

    private synchronized boolean login(Connection con, JSONObject request) {
        if (request.containsKey("username") && request.containsKey("secret")) {
            String username = (String) request.get("username");
            String secret = (String) request.get("secret");

            boolean foundUser = false;

            for (User user : userList) {
                if (user.getUserName().equals(username)) {
                    foundUser = true;
                    if (user.getPassword().equals(secret)) {
                        user.setLogin(true);
                        Message.loginSuccess(con, "logged in as user " + username);
                        if (checkOtherLoads() != null) {
                            return Message.redirect(Objects.requireNonNull(checkOtherLoads()));
                        }
                        return false;
                    }
                }
            }
            if (!foundUser) {
                return Message.loginFailed(con, "attempt to login with wrong secret");
            }
        } else {
            return Message.invalidMsg(con, "missed username or secret");
        }
        loginOrNot.put(con, true);
        return false;
    }

    private synchronized boolean logout(Connection con) {
        boolean logout = false;
        for (User user : userList) {
            if (user.getLocalSocketAddress().equals(con.getSocket().getLocalSocketAddress())) {
                user.setLogin(false);
                logout = true;
            }
        }
        if (logout) {
            con.closeCon();
        }
        return logout;
    }

    private boolean isUserLoggedInLocally(String username, String secret) {
        boolean flag = false;
        for (User user : userList) {
            if (user.getUserName().equals(username) && user.getPassword().equals(secret) && user.isLogin()) {
                flag = true;
            }
        }
        return flag;
    }

    private synchronized boolean onReceiveActivityMessage(Connection con, JSONObject request) {
        if (!request.containsKey("username")) {
            return Message.invalidMsg(con, "the message did not contain a username");
        }
        if (!request.containsKey("secret")) {
            return Message.invalidMsg(con, "the message did not contain a secret");
        }
        if (!request.containsKey("activity")) {
            return Message.invalidMsg(con, "the message did not contain an activity");
        }
        String username = (String) request.get("username");
        String secret = (String) request.get("secret");
        JSONObject activity = (JSONObject) request.get("activity");
        activity.put("authenticated_user", username);

        if (!username.equals("anonymous") && !isUserLoggedInLocally(username, secret)) {
            return Message.authenticationFail(con, "the username and secret do not match the logged in the user, " +
                    "or the user has not logged in yet");
        }
        return broadcastActivity(con, activity);
    }


    private boolean broadcastActivity(Connection sourceConnection, JSONObject activity) {
        for (Connection c : this.getClientConnections()) {
            Message.activityBroadcast(c, activity);
        }
        // broadcast activity to other servers except the one it comes from
        if (parentConnection != sourceConnection) {
            Message.activityBroadcast(parentConnection, activity);
        }
        if (lChildConnection != sourceConnection) {
            Message.activityBroadcast(lChildConnection, activity);
        }
        if (rChildConnection != sourceConnection) {
            Message.activityBroadcast(rChildConnection, activity);
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
     * A new incoming connection has been established, and a reference is returned
     * to it. 1. remote server -> local server 2. client -> local server
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
     * A new outgoing connection has been established, and a reference is returned
     * to it. Only local server -> remote server remote server will be the parent of
     * local server
     *
     * @param s
     * @return
     * @throws IOException
     */
    public synchronized Connection outgoingConnection(Socket s) throws IOException {
        log.debug("outgoing connection: " + Settings.socketAddress(s));
        Connection c = new Connection(s);
        parentConnection = c;
        Message.authenticate(c);
        return c;
    }

    @Override
    public void run() {
        log.info("using activity interval of " + Settings.getActivityInterval() + " milliseconds");
        while (!term) {
            // do something with 5 second intervals in between
            if (parentConnection != null) {
                Message.serverAnnounce(parentConnection, clientConnections.size());
            }
            if (lChildConnection != null) {
                Message.serverAnnounce(lChildConnection, clientConnections.size());
            }
            if (rChildConnection != null) {
                Message.serverAnnounce(rChildConnection, clientConnections.size());
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
        log.info("closing " + clientConnections.size() + " client connections");
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
