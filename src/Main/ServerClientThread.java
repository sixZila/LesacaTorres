package Main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
        DataInputStream dis = null;
        DataOutputStream dos = null;
        boolean login;
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            dos.writeUTF("Username:");
            String name;
            do {
                name = dis.readUTF();
                login = true;

                if (!(name.isEmpty() || name.equals(""))) {
                    String[] name2 = name.split("\\s");
                    if (name2.length == 1) {
                        for (Peer peer : peers) {
                            if (peer.checkUsername(name)) {
                                dos.writeUTF(name + " already exists.");
                                login = false;
                                break;
                            }
                        }
                        if (login) {
                            this.name = name;
                            dos.writeUTF("Logged in as " + name);
                            user = new Peer(socket, name);
                            peers.add(user);
                        }
                    } else {
                        dos.writeUTF("Your username should not contain any spaces.");
                        login = false;
                    }
                } else {
                    dos.writeUTF("Your username can not be blank.");
                    login = false;
                }
            } while (!login);

            while (true) {
                String string = dis.readUTF();
                String[] stringarray = string.split("\\s");
                String message;

                switch (stringarray[0]) {
                    case "\"POST\"":
                        if (stringarray.length > 1) {
                            message = combineMessage(stringarray, 1);
                            for (Peer follower : peers) {
                                Socket s = follower.getSocket();
                                DataOutputStream out = new DataOutputStream(s.getOutputStream());
                                out.writeUTF(name + " posted: " + message);
                                dos.writeUTF("Posted a message to your followers.");
                            }
                        } else {
                            dos.writeUTF("There's no message to be posted.");
                        }
                        break;
                    case "\"PM\"":
                        if (stringarray.length > 2) {
                            boolean messagesent = false;
                            message = combineMessage(stringarray, 2);
                            for (Peer peer : peers) {
                                if (peer.checkUsername(stringarray[1])) {
                                    Socket s = peer.getSocket();
                                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                                    out.writeUTF(name + " sent a message: " + message);
                                    dos.writeUTF("Sent a message to " + stringarray[1]);
                                    messagesent = true;
                                    break;
                                }
                                if(!messagesent) {
                                    dos.writeUTF(stringarray[1] + " does not exist.");
                                }
                            }
                        } else if(stringarray.length == 2){
                            dos.writeUTF("There's no message to be sent.");
                        } else {
                            dos.writeUTF("There's no username.");
                        }
                        break;

                    case "\"FOLLOW\"":
                        boolean follow = false;
                        for (Peer peer : peers) {
                            if (peer.checkUsername(stringarray[1])) {
                                if (peer.getFollowers().contains(name)) {
                                    dos.writeUTF("You are already following this user.");
                                } else if (peer.getRequest().contains(name)) {
                                    dos.writeUTF("Request already sent.");
                                } else {
                                    peer.getRequest().add(name);
                                    Socket s = peer.getSocket();
                                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                                    out.writeUTF(name + " wants to follow you.");
                                    dos.writeUTF("Follow request sent.");
                                }
                                follow = true;
                                break;
                            }
                        }
                        if (!follow) {
                            dos.writeUTF("User does not exist");
                        }
                        break;
                    case "\"ACCEPT\"":
                        if (user.getRequest().contains(stringarray[1])) {
                            user.getRequest().remove(stringarray[1]);
                            user.getFollowers().add(stringarray[1]);

                            for (Peer peer : peers) {
                                if (peer.checkUsername(stringarray[1])) {
                                    Socket s = peer.getSocket();
                                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                                    out.writeUTF("You are now following " + name);
                                    dos.writeUTF(stringarray[1] + " is now following you.");
                                    break;
                                }
                            }
                        } else {
                            dos.writeUTF(stringarray[1] + " did not send you a follow request.");
                        }
                        break;
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
