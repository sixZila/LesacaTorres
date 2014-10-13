package Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Main {

    String serverIP = "127.0.0.1";
    Socket socket;

    public void start() {
        boolean connect = connectToServer();

        if (connect) {
            System.out.println("Connected to server.");
            Thread t = new Thread(new Input(socket));
            t.start();
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while (true) {
                    System.out.println(br.readLine());
                }
            } catch (IOException ex) {
                System.out.println("Server Disconnected.");
            }

        } else {
            ArrayList<Peer> peers = new ArrayList();
            System.out.println("Starting as a server using port 1234.");
            while (true) {
                try {
                    ServerSocket server = new ServerSocket(1234);

                    while (true) {
                        Socket peer = server.accept();

                        Thread t = new Thread(new ServerClientThread(peer, peers));
                        t.start();
                    }
                } catch (IOException ex) {
                    System.out.println("Server can not start. There must be another server running on this port");
                }
            }
        }
    }

    boolean connectToServer() {
        try {
            socket = new Socket(serverIP, 1234);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static void main(String[] args) {
        Main m = new Main();
        m.start();
    }
}
