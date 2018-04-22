package activitystreamer.client;

import activitystreamer.util.Message;
import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;

public class ClientSkeleton extends Thread {

    //-u msw9527 -s ib8sj7htw34gcmgqc183cbmpo2 ,  already registered in Aaron's server
    //-u msw9526 -s snhu0mn2kliffptwzxts96q7fv ,  already registered in Aaron's server
    private static final Logger log = LogManager.getLogger();
    private static ClientSkeleton clientSolution;
    private TextFrame textFrame;
    private Socket socket;
    private DataOutputStream dos;
    private PrintWriter out;
    DataInputStream dis;
    private InputStreamReader isr;
    private BufferedReader br;
    private JSONParser jp;

    public static ClientSkeleton getInstance() {
        if (clientSolution == null) {
            clientSolution = new ClientSkeleton();
        }
        return clientSolution;
    }

    public ClientSkeleton() {
        try {
            socket = new Socket(Settings.getRemoteHostname(), Settings.getRemotePort());
            jp = new JSONParser();
            dos = new DataOutputStream(socket.getOutputStream());
            out = new PrintWriter(dos, true);
            dis = new DataInputStream(socket.getInputStream());
            isr = new InputStreamReader(dis);
            br = new BufferedReader(isr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        textFrame = new TextFrame();
        start();
    }

    public void sendActivityObject(JSONObject activityObj) {
        out.write(activityObj.toJSONString() + "\n");
        out.flush();
    }

    public void disconnect() {
        textFrame.dispose();
        System.exit(0);
    }

    public void run() {
        String msg;
        try {
            initMsg();
            while (true) {
                if (socket.isClosed()) {
                    break;
                }
                msg = br.readLine();
                System.out.println(msg);
                if (msg == null) {
                    break;
                }
                JSONObject jo = process(msg);

            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * send initial message to server: LOGIN or REGISTER when socket established
     *
     * @throws IOException
     */
    private void initMsg() {
        //dos.wirteUTF fail to write \n as normal
        if (Settings.getUserSecret() != null) {
            // login
            out.write(Message.login(Settings.getUsername()) + "\n");
            out.flush();
        } else if (Settings.getUsername().equals("anonymous")) {
            // login as anonymous
            out.write(Message.login() + "\n");
            out.flush();
        } else {
            // register
            Settings.setUserSecret(Settings.genRandomString());
            System.out.println("ur secret is: " + Settings.getUserSecret());
            out.write(Message.register(Settings.getUsername(), Settings.getUserSecret()) + "\n");
            out.flush();
        }

    }


    private JSONObject process(String msg) throws ParseException, IOException {
        JSONObject jo = (JSONObject) jp.parse(msg);
        textFrame.setOutputText(jo);
        String cmd = (String) jo.get("command");
        if (cmd.equals(Message.REGISTER_SUCCESS)) {
            out.write(Message.login(Settings.getUsername()) + "\n");
            out.flush();
        } else if (cmd.equals(Message.REDIRECT)) {
            redirect(jo);
        }
        return jo;
    }

    private void redirect(JSONObject jo) throws IOException {
        String hostname = (String) jo.get("hostname");
        int port = ((Long) jo.get("port")).intValue();
        if (!socket.isClosed()) {
            socket.close();
        }
        Settings.setRemoteHostname(hostname);
        Settings.setRemotePort(port);

        try {
            socket = new Socket(Settings.getRemoteHostname(), Settings.getRemotePort());
            dos = new DataOutputStream(socket.getOutputStream());
            out = new PrintWriter(dos, true);
            isr = new InputStreamReader(socket.getInputStream());
            br = new BufferedReader(isr);
//            textFrame.dispose();
            textFrame = new TextFrame();
            initMsg();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}


