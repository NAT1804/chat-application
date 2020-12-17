import java.io.*;
import java.net.Socket;
import java.util.Calendar;

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
                //os.writeUTF("Enter your name: ");
                try {
                    name = is.readUTF().trim();
                    if (name.indexOf('@') == -1) {
                        break;
                    } else {
                        os.writeUTF("The name should not contain '@' character.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //Welcome
            os.writeUTF("Welcome " + name + " to our chat room");
            server.displayEvent("User " + name + " have connected to the Server");

            synchronized (this) {
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] == this) {
                        clientName = "@" + name;
                        break;
                    }
                }

                // announce the list of users who are online
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] != this) {
                        this.os.writeUTF(clients[i].clientName.substring(1) + " online");
                    }
                }

                // notice that new users are online
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] != this) {
                        clients[i].os.writeUTF("--- A new user " + name + " entered the chat room ---");
                        clients[i].os.writeUTF(name + " online");
                    }
                }

            }

            // Start the conversation
            String received = "";
            int isStartSend = 0;
            while(true) {
                try {
                    received = is.readUTF().trim();
                    if (received.startsWith("/quit")) break;

                    if (received.indexOf("SEND") >= 0) {
                        String words[];
                        if(received.startsWith("@")) {
                            words = received.split("\\s", 3);
                        } else {
                            words = received.split("\\s", 2);
                        }

                        if (words.length == 2)
                            downloadFile("./storage/server/" + words[1]);
                        else
                            downloadFile("./storage/server/" + words[2]);
                    }

                    // turn on mode "DOWNLOAD"
                    if (received.startsWith("DOWNLOAD")) {
                        String[] parts = received.split("\\s", 2);
                        isStartSend = 1;
//                        if (parts.length == 2) {
                            if (checkFileName(parts[1])) {
                                synchronized (this) {
                                    this.os.writeUTF("Download file successfully " + parts[1]);
                                }
                                displayMsg("[" + clientName.substring(1) + "] " + received);
                                sendFile("./storage/server/" + parts[1]);
                            } else {
                                received = (parts[1] + " not exist");
                                synchronized (this) {
                                    this.os.writeUTF(received);
                                }
                                displayMsg("[" + clientName.substring(1) + "] File download request doesn't exist" );
                            }
//                        }
                    }

                    // if the message is private sent it to the given client.
                    if (isStartSend == 0) { //avoid mode "DOWNLOAD"
                        if (received.startsWith("@")) {
                            String[] words = received.split("\\s", 2);
                            if (words.length > 1 && words[1] != null) {
                                words[1] = words[1].trim();
                                if (!words[1].isEmpty()) {
                                    synchronized (this) {
                                        for (int i = 0; i < maxClients; i++) {
                                            if (clients[i] != null && clients[i] != this
                                                    && clients[i].clientName != null
                                                    && clients[i].clientName.equals(words[0])) {
                                                clients[i].os.writeUTF("[" + name + " to you] " + words[1]);
                                                displayMsg("[" + name + " to " + clients[i].clientName.substring(1) + "] " + words[1]);
                                                this.os.writeUTF("[you to " + clients[i].clientName.substring(1) + "] " + words[1]);
                                                break;
                                            }
                                            if (i == (maxClients - 1)) {
                                                this.os.writeUTF(words[0].substring(1) + " not connected");
                                                displayMsg("[" + this.clientName.substring(1) + " to " + words[0].substring(1) + "] " + words[0].substring(1) + " not online");
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // the message is public, broadcast it to all clients
                            synchronized (this) {
                                for (int i = 0; i < maxClients; i++) {
                                    if (clients[i] != null && clients[i].clientName != null) {
                                        clients[i].os.writeUTF("[" + name + "] " + received);
                                    }
                                }
                                displayMsg("[" + name + " to everybody] " + received);
                            }
                        }
                    }
                    // exit mode "DOWNLOAD"
                    isStartSend = 0;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            synchronized (this) {
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] != this && clients[i].clientName != null) {
                        clients[i].os.writeUTF("--- The user " + name + " is leaving the chat room! ---");
                        clients[i].os.writeUTF(name + " exit");
                    }
                }

            }
            os.writeUTF("Bye User");
            server.displayEvent("User " + this.clientName.substring(1) + " loses connection with Server");

            // clean up
            synchronized (this) {
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] == this) {
                        clients[i] = null;
                    }
                }
            }

            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayMsg(String msg) {
        String mess = "[" + server.getSdf().format(Calendar.getInstance().getTime()) + "] " + msg;
        if (server.getServerGUI() != null) {
            server.getServerGUI().appendRoom(mess + "\n");
            try {
                server.getFile().write(mess + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean checkFileName(String filename) {
        File infile = new File("./storage/server/" + filename);
        if (!infile.exists()) return false;
        else return true;
    }

    public void sendFile(String filename) throws IOException {
        File infile = new File(filename);
        FileInputStream fis = new FileInputStream(infile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        byte[] contents;
        long fileLength = infile.length();
        long current = 0;
        os.writeLong(fileLength);
        while(current != fileLength)
        {
            int size = 1024;
            if (fileLength - current >= size)
                current += size;
            else {
                size = (int)(fileLength - current);
                current = fileLength;
            }
            contents = new byte[size];
            bis.read(contents, 0, size);

            os.write(contents);

        }
        os.flush();
    }

    public void downloadFile(String filename) throws IOException {
        FileOutputStream fos    = new FileOutputStream(filename);
        BufferedOutputStream bos= new BufferedOutputStream(fos);
        byte[] contents         = new byte[1024];
        long fileLength         = is.readLong();
        int byteRead            = 0;
        int size                = 0;
        while(size < fileLength) {
            byteRead = is.read(contents);
            bos.write(contents, 0, byteRead);
            size += byteRead;
        }
        bos.flush();
    }

    public void close(){
        try {
            clientSocket.close();
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}