package activitystreamer.util;

import activitystreamer.server.Connection;
import org.json.simple.JSONObject;

public class Message {
    public static final String AUTHENTICATE = "AUTHENTICATE";
    public static final String INVALID_MESSAGE = "INVALID_MESSAGE";
    public static final String AUTHENTICATION_FAIL = "AUTHENTICATION_FAIL";
    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String REDIRECT = "REDIRECT";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String ACTIVITY_MESSAGE = "ACTIVITY_MESSAGE";
    public static final String SERVER_ANNOUNCE = "SERVER_ANNOUNCE";
    public static final String ACTIVITY_BROADCAST = "ACTIVITY_BROADCAST";
    public static final String REGISTER = "REGISTER";
    public static final String REGISTER_FAILED = "REGISTER_FAILED";
    public static final String REGISTER_SUCCESS = "REGISTER_SUCCESS";
    public static final String LOCK_REQUEST = "LOCK_REQUEST";
    public static final String LOCK_DENIED = "LOCK_DENIED";
    public static final String LOCK_ALLOWED = "LOCK_ALLOWED";


    public synchronized static void invalidMsg(Connection con, String info) {
        JSONObject json = new JSONObject();
        json.put("command", Message.INVALID_MESSAGE);
        json.put("info", info);
        con.writeMsg(json.toJSONString());
    }

    public synchronized static void authenticationFail(Connection con, String info) {
        JSONObject json = new JSONObject();
        json.put("command", Message.AUTHENTICATION_FAIL);
        json.put("info", info);
        con.writeMsg(json.toJSONString());
    }

    public static void serverAnnounce(Connection connection, int load) {
        JSONObject json = new JSONObject();
        json.put("command", Message.SERVER_ANNOUNCE);
        json.put("id", Settings.getServerId());
        json.put("load", load);
        json.put("hostname", Settings.getLocalHostname());
        json.put("port", Settings.getLocalPort());
        connection.writeMsg(json.toJSONString());
    }

}
