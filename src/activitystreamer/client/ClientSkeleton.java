package activitystreamer.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.util.Settings;

public class ClientSkeleton extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static ClientSkeleton clientSolution;
	private TextFrame textFrame;
	private Socket socket;
	DataOutputStream dos;
	DataInputStream dis;
	InputStreamReader isr;
	BufferedReader br;
	public static ClientSkeleton getInstance(){
		if(clientSolution==null){
			clientSolution = new ClientSkeleton();

		}
		return clientSolution;
	}
	
	public ClientSkeleton(){
		try {
			socket = new Socket(Settings.getRemoteHostname(), Settings.getRemotePort());
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		textFrame = new TextFrame();		
		start();
	}
	
	
	
	@SuppressWarnings("unchecked")
	public void sendActivityObject(JSONObject activityObj){
		try {
			dos.writeUTF(activityObj.toJSONString() + "\n");
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public void disconnect(){
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void run(){	
		String msg = "";
		
		try {

			isr = new InputStreamReader(dis);
			br = new BufferedReader(isr);
			StringBuffer sb = new StringBuffer();
			
			while (true) {
				msg = br.readLine();
				if (msg == null) {
					break;
				}
				sb.append(msg);
			}
			
			JSONParser jp = new JSONParser();
			JSONObject jo = new JSONObject();
			jo = (JSONObject) jp.parse(sb.toString());
			
			textFrame.setOutputText(jo);
			
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
			
			
	}

	
}
