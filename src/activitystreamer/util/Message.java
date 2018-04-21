package activitystreamer.util;

import activitystreamer.server.Connection;
import org.json.simple.JSONObject;

import java.net.Socket;

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


    public synchronized static boolean invalidMsg(Connection con, String info) {
        JSONObject json = new JSONObject();
        json.put("command", Message.INVALID_MESSAGE);
        json.put("info", info);
        con.writeMsg(json.toJSONString());
        return true;
    }

    public synchronized static void authenticate(Connection con) {
        JSONObject json = new JSONObject();
        json.put("command", Message.AUTHENTICATE);
        json.put("secret", Settings.serverSecret);
        con.writeMsg(json.toJSONString());
    }

    public synchronized static boolean authenticationFail(Connection con, String info) {
        JSONObject json = new JSONObject();
        json.put("command", Message.AUTHENTICATION_FAIL);
        json.put("info", info);
        con.writeMsg(json.toJSONString());
        return true;
    }

    public synchronized static void serverAnnounce(Connection con, int load) {
        JSONObject json = new JSONObject();
        json.put("command", Message.SERVER_ANNOUNCE);
        json.put("id", Settings.getServerId());
        json.put("load", load);
        json.put("hostname", Settings.getLocalHostname());
        json.put("port", Settings.getLocalPort());
        con.writeMsg(json.toJSONString());
    }

    public synchronized static boolean registerFailed(Connection con, String info) {
        JSONObject json = new JSONObject();
        json.put("command", Message.REGISTER_FAILED);
        json.put("info", info);
        con.writeMsg(json.toJSONString());
        return true;
    }

    public synchronized static boolean register(Connection con, String info) {
        JSONObject json = new JSONObject();
        json.put("command", Message.REGISTER_SUCCESS);
        json.put("info", info);
        con.writeMsg(json.toJSONString());
        return false;
    }

    public synchronized static boolean loginSuccess(Connection con, String info) {
        JSONObject json = new JSONObject();
        json.put("command", Message.LOGIN_SUCCESS);
        json.put("info", info);
        con.writeMsg(json.toJSONString());
        return false;
    }


    public synchronized static boolean loginFailed(Connection con, String info) {
        JSONObject json = new JSONObject();
        json.put("command", Message.LOGIN_FAILED);
        json.put("info", info);
        con.writeMsg(json.toJSONString());
        return false;
    }

    public synchronized static boolean logout(Connection con) {
        JSONObject json = new JSONObject();
        json.put("command", Message.LOGOUT);
        con.writeMsg(json.toJSONString());
        return true;

    }

    public synchronized static void redirect(Connection con) {
        JSONObject json = new JSONObject();
        json.put("command", Message.REDIRECT);
        json.put("hostname", Settings.getRemoteHostname());
        json.put("port", Settings.getRemotePort());
        con.writeMsg(json.toJSONString());
    }

    public synchronized static boolean activityBroadcast(Connection con, JSONObject activity) {
        JSONObject json = new JSONObject();
        json.put("command", Message.ACTIVITY_BROADCAST);
        json.put("activity", activity);
        con.writeMsg(json.toJSONString());
        return false;
    }


}
