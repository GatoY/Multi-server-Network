package activitystreamer.client;

import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;

public class ClientSkeleton extends Thread {
    private static final Logger log = LogManager.getLogger();
    private static ClientSkeleton clientSolution;
    private TextFrame textFrame;
    private Socket socket;
    private DataOutputStream dos;
    private InputStreamReader isr;
    private BufferedReader br;

    public static ClientSkeleton getInstance() {
        if (clientSolution == null) {
            clientSolution = new ClientSkeleton();
        }
        return clientSolution;
    }

    public ClientSkeleton() {
        try {
            socket = new Socket(Settings.getRemoteHostname(), Settings.getRemotePort());
        } catch (IOException e) {
            e.printStackTrace();
        }

        textFrame = new TextFrame();
        start();
    }

    @SuppressWarnings("unchecked")
    public void sendActivityObject(JSONObject activityObj) {
        try {
            dos = new DataOutputStream(socket.getOutputStream());
            PrintWriter out = new PrintWriter(dos, true);
            out.write(activityObj.toJSONString() + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (socket != null) {
                textFrame.dispose();
                br.close();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        String msg;
        try {
            isr = new InputStreamReader(socket.getInputStream());
            br = new BufferedReader(isr);

            while (true) {
                if (socket.isClosed()) {
                    break;
                }
                msg = br.readLine();
                if (msg == null) {
                    break;
                }
                JSONParser jp = new JSONParser();
                JSONObject jo = (JSONObject) jp.parse(msg);
                textFrame.setOutputText(jo);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

}
