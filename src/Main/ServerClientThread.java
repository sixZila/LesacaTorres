package Main;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

public class ServerClientThread implements Runnable {

    Socket socket;
    ArrayList<Peer> peers;
    String name;
    Peer user;

    public ServerClientThread(Socket socket, ArrayList<Peer> peers) {
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

    @Override
    public void run() {
        BufferedReader br = null;
        PrintWriter pw = null;
        boolean login;
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pw = new PrintWriter(socket.getOutputStream(), true);

            pw.println("Username:");
            String name;
            do {
                name = br.readLine();
                login = true;

                if (!(name.isEmpty() || name.equals(""))) {
                    String[] name2 = name.split("\\s");
                    if (name2.length == 1) {
                        for (Peer peer : peers) {
                            if (peer.checkUsername(name)) {
                                pw.println(name + " already exists.");
                                login = false;
                                break;
                            }
                        }
                        if (login) {
                            this.name = name;
                            pw.println("Logged in as " + name);
                            user = new Peer(socket, name);
                            peers.add(user);
                        }
                    } else {
                        pw.println("Your username should not contain any spaces.");
                        login = false;
                    }
                } else {
                    pw.println("Your username can not be blank.");
                    login = false;
                }
            } while (!login);

            while (true) {
                String string = br.readLine();
                String[] stringarray = string.split("\\s");
                String message;

                switch (stringarray[0]) {
                    case "\"POST\"":
                        if (stringarray.length > 1) {
                            message = combineMessage(stringarray, 1);
                            for (String follower : user.getFollowers()) {
                                for (Peer peer : peers) {
                                    if (peer.checkUsername(follower)) {
                                        Socket s = peer.getSocket();
                                        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                                        out.println(name + " posted: " + message);
                                        break;
                                    }
                                }
                            }
                            pw.println("Posted a message to your followers.");
                        } else {
                            pw.println("There's no message to be posted.");
                        }
                        break;
                    case "\"PM\"":
                        if (stringarray.length > 2) {
                            boolean messagesent = false;
                            message = combineMessage(stringarray, 2);
                            for (Peer peer : peers) {
                                if (peer.checkUsername(stringarray[1])) {
                                    Socket s = peer.getSocket();
                                    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                                    out.println(name + " sent a message: " + message);
                                    pw.println("Sent a message to " + stringarray[1]);
                                    messagesent = true;
                                    break;
                                }
                                if (!messagesent) {
                                    pw.println(stringarray[1] + " does not exist.");
                                }
                            }
                        } else if (stringarray.length == 2) {
                            pw.println("There's no message to be sent.");
                        } else {
                            pw.println("There's no username.");
                        }
                        break;

                    case "\"FOLLOW\"":
                        boolean follow = false;
                        for (Peer peer : peers) {
                            if (peer.checkUsername(stringarray[1])) {
                                if (peer.getFollowers().contains(name)) {
                                    pw.println("You are already following this user.");
                                } else if (peer.getRequest().contains(name)) {
                                    pw.println("Request already sent.");
                                } else {
                                    peer.getRequest().add(name);
                                    Socket s = peer.getSocket();
                                    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                                    out.println(name + " wants to follow you.");
                                    pw.println("Follow request sent.");
                                }
                                follow = true;
                                break;
                            }
                        }
                        if (!follow) {
                            pw.println("User does not exist");
                        }
                        break;
                    case "\"ACCEPT\"":
                        if (user.getRequest().contains(stringarray[1])) {
                            user.getRequest().remove(stringarray[1]);
                            user.getFollowers().add(stringarray[1]);

                            for (Peer peer : peers) {
                                if (peer.checkUsername(stringarray[1])) {
                                    Socket s = peer.getSocket();
                                    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                                    out.println("You are now following " + name);
                                    pw.println(stringarray[1] + " is now following you.");
                                    break;
                                }
                            }
                        } else {
                            pw.println(stringarray[1] + " did not send you a follow request.");
                        }
                        break;
                }
            }
        } catch (IOException ex) {
            System.out.println("Client: " + socket.getInetAddress().getHostAddress() + " disconnected.");
        } finally {
            try {
                br.close();
                pw.close();
            } catch (IOException ex) {

            }
        }
    }

}
