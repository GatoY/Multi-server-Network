package activitystreamer.util;

import java.net.SocketAddress;

public class User {
    private SocketAddress localSocketAddress;
    private String userName;
    private String password;
    private boolean isLogin;

    public User(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public SocketAddress getLocalSocketAddress() {
        return localSocketAddress;
    }

    public void setLocalSocketAddress(SocketAddress localSocketAddress) {
        this.localSocketAddress = localSocketAddress;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isLogin() {
        return isLogin;
    }

    public void setLogin(boolean login) {
        isLogin = login;
    }
}
