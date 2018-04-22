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
	//msw9527 ib8sj7htw34gcmgqc183cbmpo2
    private static final Logger log = LogManager.getLogger();
    private static ClientSkeleton clientSolution;
    private TextFrame textFrame;
    private Socket socket;
    private DataOutputStream dos;
    private PrintWriter out;
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
            dos = new DataOutputStream(socket.getOutputStream());
            out = new PrintWriter(dos, true);
            initMsg();
        } catch (IOException e) {
            e.printStackTrace();
        }

        textFrame = new TextFrame();
        start();
    }

    @SuppressWarnings("unchecked")
    public void sendActivityObject(JSONObject activityObj) {
		out.write(activityObj.toJSONString() + "\n");
		out.flush();
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
        } finally {
        	System.exit(0);
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
                JSONObject jo = process(msg);
                textFrame.setOutputText(jo);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * send initial message to server: LOGIN or REGISTER when socket established
     * @throws IOException
     */
	private void initMsg() throws IOException {
		//dos.wirteUTF fail to write \n as normal
		if (Settings.getUserSecret() != null) {
			// login
			out.write(initJsonString(Settings.getUsername()) + "\n");
			out.flush();
		} else if (Settings.getUsername().equals("anonymous")) {
			// login as anonymous
			out.write(initJsonString() + "\n");
			out.flush();
		} else {
			// register
			Settings.setUserSecret(Settings.genRandomString());
			System.out.println("ur secret is: " + Settings.getUserSecret());
			out.write(initJsonString(Settings.getUsername(), Settings.getUserSecret()) + "\n");
			out.flush();
		}

	}
	
	/**
	 * generate JSON string for anonymous login
	 * @return anonymous login JSONString
	 */
	private String initJsonString() {
		// anonymous login
		JSONObject log = new JSONObject();
		log.put("command", "LOGIN");
		log.put("username", Settings.getUsername());
		return log.toJSONString();
	}
	
	/**
	 * overwrite iniJsonString(), specifically for normal login
	 * @param userName
	 * @return normal login JSONString
	 */
	private String initJsonString(String userName) {
		// normal login
		JSONObject log = new JSONObject();
		log.put("command", "LOGIN");
		log.put("username", userName);
		log.put("secret", Settings.getUserSecret());
		return log.toJSONString();
	}
	
	/**
	 * overwrite iniJsonString(), specifically for register
	 * @param userName
	 * @param secret
	 * @return register JSONString
	 */
	private String initJsonString(String userName, String secret) {
		// register
		JSONObject log = new JSONObject();
		log.put("command", "REGISTER");
		log.put("username", userName);
		log.put("secret", secret);
		return log.toJSONString();
	}
	
	private JSONObject process(String msg) throws ParseException {
		JSONParser jp = new JSONParser();
		JSONObject jo = (JSONObject) jp.parse(msg);
		String cmd = (String) jo.get("command");
		if(cmd.equals(Message.REGISTER_SUCCESS)) {
			out.write(initJsonString(Settings.getUsername()) + "\n");
			out.flush();
		}
		
		
		return jo;
	}
}
