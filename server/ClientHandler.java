import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

class ClientHandler extends Thread {
    private String clientName = null;
    private Socket clientSocket = null;
    private DataInputStream is = null;
    private DataOutputStream os = null;
    private int maxClients;
    private final ClientHandler []clients;
    private Server server = null;

    public ClientHandler(Socket clientSocket, ClientHandler[] clients, Server server)
    {
        this.clientSocket = clientSocket;
        this.clients = clients;
        maxClients = clients.length;
        this.server = server;
    }

    @Override
    public void run() {
        int maxClients = this.maxClients;
        ClientHandler[] clients = this.clients;

        try {
            is = new DataInputStream(clientSocket.getInputStream());
            os = new DataOutputStream(clientSocket.getOutputStream());

            // enter name not have '@' character
            String name;
            while(true) {
                os.writeUTF("Enter your name: ");
                try {
                    name = is.readUTF().trim();
                    if (name.indexOf('@') == -1) {
                        break;
                    } else {
                        os.writeUTF("The name should not contain '@' character.\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //Welcome
            os.writeUTF("Welcome " + name + " to our chat room. To leave enter /quit in a new line.\n");

            synchronized (this) {
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] == this) {
                        clientName = "@" + name;
                        break;
                    }
                }

                // announce the list of users who are online
                String notification = "List User Online:\n";
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] != this) {
                        notification += (clients[i].clientName.substring(1) + " online\n");
                    }
                    if (i == (maxClients - 1)) {
                        if (!notification.equals("List User Online:\n")) {
                            this.os.writeUTF(notification);
                        } else {
                            this.os.writeUTF(notification + "No user online\n");
                        }
                    }
                }

                // notice that new users are online
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] != this) {
                        clients[i].os.writeUTF("--- A new user " + name + " entered the chat room ---\n");
                    }
                }
            }

            // Start the conversation
            String received = "";
            while(!(received = is.readUTF()).trim().isEmpty()) {
                try {
                    //received = is.readUTF();
                    if (received.startsWith("/quit")) break;
                    // if the message is private sent it to the given client.
                    if (received.startsWith("@")) {
                        String[] words = received.split("\\s", 2);
                        if (words.length > 1 && words[1] != null) {
                            words[1] = words[1].trim();
                            if (!words[1].isEmpty()) {
                                synchronized (this) {
                                    for (int i=0; i<maxClients; i++) {
                                        if (clients[i] != null && clients[i] != this
                                                && clients[i].clientName != null
                                                && clients[i].clientName.equals(words[0])) {
                                            clients[i].os.writeUTF("[" + name + " to you] " + words[1] + "\n");
                                            //this.os.writeUTF(">" + name + ">" + words[1]);
                                            break;
                                        }
                                        if (i == (maxClients - 1)) {
                                            this.os.writeUTF(words[0] + " not online || " + words[0] + " not exists\n");
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // the message is public, broadcast it to all clients
                        synchronized (this) {
                            for (int i = 0; i < maxClients; i++) {
                                if (clients[i] != null && clients[i].clientName != null && clients[i] != this) {
                                    clients[i].os.writeUTF("[" + name + "] " + received + "\n");
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            synchronized (this) {
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] != this && clients[i].clientName != null) {
                        clients[i].os.writeUTF("--- The user " + name + " is leaving the chat room! ---\n");
                    }
                }
            }
            os.writeUTF("Bye\n");

            // clean up
            synchronized (this) {
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] == this) {
                        clients[i] = null;
                    }
                }
            }

            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}