package Main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

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
        DataInputStream inputStream = null;
        DataOutputStream outputStream = null;
        boolean login;
        try {
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());

            outputStream.writeUTF("Username:");

            String name;
            do {
                name = inputStream.readUTF();
                login = true;

                if (!(name.isEmpty() || name.equals(""))) {
                    String[] name2 = name.split("\\s");
                    if (name2.length == 1) {
                        for (Peer peer : peers) {
                            if (peer.checkUsername(name)) {
                                outputStream.writeUTF(name + " already exists.");
                                login = false;
                                break;
                            }
                        }
                        if (login) {
                            this.name = name;
                            outputStream.writeUTF("Logged in as " + name);
                            user = new Peer(socket, name);
                            peers.add(user);
                        }
                    } else {
                        outputStream.writeUTF("Your username should not contain any spaces.");
                        login = false;
                    }
                } else {
                    outputStream.writeUTF("Your username can not be blank.");
                    login = false;
                }
            } while (!login);

            while (true) {
                String string = inputStream.readUTF();
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
                                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                                        out.writeUTF(name + " posted: " + message);
                                        break;
                                    }
                                }
                            }
                            outputStream.writeUTF("Posted a message to your followers.");
                        } else {
                            outputStream.writeUTF("There's no message to be posted.");
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
                                    outputStream.writeUTF("Sent a message to " + stringarray[1]);
                                    messagesent = true;
                                    break;
                                }
                            }
                            if (!messagesent) {
                                outputStream.writeUTF(stringarray[1] + " does not exist.");
                            }
                        } else if (stringarray.length == 2) {
                            outputStream.writeUTF("There's no message to be sent.");
                        } else {
                            outputStream.writeUTF("There's no username.");
                        }
                        break;
                    case "\"FOLLOW\"":
                        boolean follow = false;
                        for (Peer peer : peers) {
                            if (peer.checkUsername(stringarray[1])) {

                                if (peer.getFollowers().contains(name)) {
                                    outputStream.writeUTF("You are already following this user.");
                                } else if (peer.getRequest().contains(name)) {
                                    outputStream.writeUTF("Request already sent.");
                                } else {
                                    peer.getRequest().add(name);
                                    Socket s = peer.getSocket();
                                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                                    out.writeUTF(name + " wants to follow you.");
                                    outputStream.writeUTF("Follow request sent.");
                                }
                                follow = true;
                                break;
                            }
                        }
                        if (!follow) {
                            outputStream.writeUTF("User does not exist");
                        }
                        break;
                    case "\"APPROVE\"":
                        if (user.getRequest().contains(stringarray[1])) {
                            user.getRequest().remove(stringarray[1]);
                            user.getFollowers().add(stringarray[1]);

                            for (Peer peer : peers) {
                                if (peer.checkUsername(stringarray[1])) {
                                    Socket s = peer.getSocket();
                                    DataOutputStream out = new DataOutputStream(s.getOutputStream());

                                    out.writeUTF("You are now following " + name);
                                    outputStream.writeUTF(stringarray[1] + " is now following you.");
                                    break;
                                }
                            }
                        } else {
                            outputStream.writeUTF(stringarray[1] + " did not send you a follow request.");
                        }
                        break;
                    case "\"UNFOLLOW\"":
                        boolean unfollow = false;
                        for (Peer peer : peers) {
                            if (peer.checkUsername(stringarray[1])) {

                                if (peer.getFollowers().contains(name)) {
                                    peer.getFollowers().remove(name);

                                    Socket s = peer.getSocket();
                                    DataOutputStream out = new DataOutputStream(s.getOutputStream());

                                    out.writeUTF(name + " unfollowed you.");

                                    outputStream.writeUTF("You have unfollowed " + stringarray[1]);

                                } else {
                                     outputStream.writeUTF("You are not following " + stringarray[1]);
                                }
                                unfollow = true;
                                break;
                            }
                        }
                        if (!unfollow) {
                            outputStream.writeUTF("User does not exist");
                        }
                        break;
                    default:
                        outputStream.writeUTF("Wrong syntax.");
                }
            }
        } catch (IOException ex) {
            System.out.println("Client: " + socket.getInetAddress().getHostAddress() + " disconnected.");
        } finally {
            try {
                inputStream.close();
                outputStream.close();

                for (Iterator<Peer> it = peers.iterator(); it.hasNext();) {
                    Peer peer = it.next();
                    if (peer.checkUsername(name)) {
                        it.remove();
                    }
                }
            } catch (IOException ex) {

            }
        }
    }

}
