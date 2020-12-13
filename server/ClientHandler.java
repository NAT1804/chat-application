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
                        os.writeUTF("The name should not contain '@' character.\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //Welcome
            os.writeUTF("Welcome " + name + " to our chat room. To leave enter /quit in a new line.\n");
            server.displayEvent("User " + name + " connect to Server");

            synchronized (this) {
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] == this) {
                        clientName = "@" + name;
                        break;
                    }
                }

                // announce the list of users who are online
                String notification = "";
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] != this) {
                        notification += (clients[i].clientName.substring(1) + " online\n");
                    }
                    if (i == (maxClients - 1)) {
                        if (!notification.equals("")) {
                            this.os.writeUTF(notification);
                        } else {
                            this.os.writeUTF(notification + "No user online");
                        }
                    }
                }

                // notice that new users are online
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] != this) {
                        clients[i].os.writeUTF("--- A new user " + name + " entered the chat room ---");
                    }
                }
            }

            // Start the conversation
            String received = "";
            while(true) {
                try {
                    received = is.readUTF().trim();

                    if (received.startsWith("/quit")) break;

                    if (received.startsWith("SEND")) {
                        downloadFile("./storage/server/" + received.substring(5));
                    }

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
                                            clients[i].os.writeUTF("[" + name + " to you] " + words[1]);
                                            //this.os.writeUTF(">" + name + ">" + words[1]);
                                            break;
                                        }
                                        if (i == (maxClients - 1)) {
                                            this.os.writeUTF(words[0] + " not online || " + words[0] + " not exists");
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
                                    clients[i].os.writeUTF("[" + name + "] " + received);
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
                        clients[i].os.writeUTF("--- The user " + name + " is leaving the chat room! ---");
                    }
                }
            }
            os.writeUTF("Bye");

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

    private void displayMsg(String msg) {
        String mess = "[" + server.getSdf().format(Calendar.getInstance().getTime()) + "] " + msg;
        if (server.getServerGUI() != null)
            server.getServerGUI().appendRoom(mess+"\n");
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
            //server.displayEvent("Sending file ... " + (current*100)/fileLength + "% complete!");

        }
        //server.displayEvent("Length of file " + filename + " : " + fileLength + "B");
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
            //server.displayEvent("Downloading ..." + (size*100)/fileLength + "% complete!");
        }

        //server.displayEvent("Length of file " + filename + " : " + fileLength + "B");

        bos.flush();
        //fos.close();
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