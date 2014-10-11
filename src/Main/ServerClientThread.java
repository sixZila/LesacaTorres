package Main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

public class ServerClientThread implements Runnable {

    Socket socket;
    ArrayList<Socket> peers;

    public ServerClientThread(Socket socket, ArrayList<Socket> peers) {
        this.socket = socket;
        this.peers = peers;
    }

    public String combineMessage(String[] s, int i) {
        String cm = "";

        for (int j = i; j < s.length; j++) {
            cm += s[j];

            if (j < s.length - 1) {
                cm += " ";
            }
        }
        return cm;
    }

    public void run() {
        DataInputStream dis = null;
        DataOutputStream dos = null;
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            while (true) {
                String s = dis.readUTF();
                String[] stringarray = s.split("\\s");

                if (stringarray[0].equals("\"POST\"")) {
                    String message = combineMessage(stringarray, 1);
                    
                    for (Iterator<Socket> i = peers.iterator(); i.hasNext();) {
                        Socket peerSocket = i.next();

                        DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream());
                        out.writeUTF(socket.getInetAddress().getHostAddress() + " posted: " + message);
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Client: " + socket.getInetAddress().getHostAddress() + " disconnected.");
        } finally {
            try {
                dis.close();
                dos.close();
            } catch (IOException ex) {

            }
        }
    }

}
