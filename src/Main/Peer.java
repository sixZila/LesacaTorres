package Main;

import java.net.Socket;
import java.util.ArrayList;

public class Peer {

    private final Socket socket;
    private String username;
    private ArrayList<String> followers;
    private ArrayList<String> request;

    public Peer(Socket socket, String username) {
        this.socket = socket;
        this.username = username;
        followers = new ArrayList();
        request = new ArrayList();
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean checkUsername(String s) {
        return s.equals(username);
    }

    public ArrayList<String> getFollowers() {
        return followers;
    }

    public ArrayList<String> getRequest() {
        return request;
    }

}
