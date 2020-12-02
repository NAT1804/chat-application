import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;
    private final int maxClients = 10;
    private ClientHandler clients[] = new ClientHandler[maxClients];

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server start");
            //System.out.println("Waiting a connection ...");
            while(true) {
                clientSocket = serverSocket.accept();
                int i;
                for (i = 0; i<maxClients; i++) {
                    if (clients[i] == null) {
                        clients[i] = new ClientHandler(clientSocket, clients);
                        clients[i].start();
                        break;
                    }
                }
                if (i == maxClients) {
                    DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());
                    os.writeUTF("Server too busy. Try later.");
                    os.close();
                    clientSocket.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int portNumber = 8080;
        if (args.length < 1) {
            System.out.println("Usage: java Server <portNumber>\n"
                    + "Using default port number = " + portNumber);
        } else {
            portNumber = Integer.valueOf(args[0]).intValue();
        }

        Server server = new Server(portNumber);
    }
}

class ClientHandler extends Thread {
    private String clientName = null;
    private Socket clientSocket = null;
    private DataInputStream is = null;
    private DataOutputStream os = null;
    private int maxClients;
    private final ClientHandler []clients;

    public ClientHandler(Socket clientSocket, ClientHandler[] clients)
    {
        this.clientSocket = clientSocket;
        this.clients = clients;
        maxClients = clients.length;
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
