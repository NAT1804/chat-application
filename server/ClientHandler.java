import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class ClientHandler extends Thread {
    private String clientName = null;
    private Socket clientSocket = null;
    private DataInputStream is = null;
    private DataOutputStream os = null;
    private int maxClients;
    private final ClientHandler []clients;
    private Server server = null;
    private List<String> groupName;

    public ClientHandler(Socket clientSocket, ClientHandler[] clients, Server server)
    {
        this.clientSocket = clientSocket;
        this.clients = clients;
        maxClients = clients.length;
        this.server = server;
        groupName = new ArrayList<>();
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
                        //os.writeUTF("The name should not contain '@' character.");
                        server.displayEvent("A client has entered the wrong username");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //Welcome
            os.writeUTF("Welcome to chat room, " + name + "! [sys_msg]");
            server.displayEvent(name + " have connected to the Server");

            synchronized (this) {
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] == this) {
                        clientName = "@" + name;
                        break;
                    }
                }

                // Announce the list of users who are online
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] != this) {
                        this.os.writeUTF(clients[i].clientName.substring(1) + " online");
                    }
                }

                // Notice that new users are online
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] != this) {
                        clients[i].os.writeUTF(name + " entered the chat room. [sys_msg]");
                        clients[i].os.writeUTF(name + " online");
                    }
                }

            }

            // Start the conversation
            String received = "";
            boolean isPersonalAct = false;
            boolean isStartSend = false;
            while(true) {
                try {
                    received = is.readUTF().trim();
                    if (received.startsWith("/quit")) break;

                    // mode "CREATE GROUP"
                    if(received.startsWith("CREATEGROUP")) {
                        isPersonalAct = true;
                        groupName.add(received.substring(12));
                        synchronized (this) {
                            this.os.writeUTF("Create group " + received.substring(12) + " successfully! [sys_msg]");
                            this.os.writeUTF("Gr:" + this.groupName.get(groupName.size()-1) + " online");
                            displayMsg(this.clientName.substring(1) + " create group " + this.groupName.get(groupName.size()-1));
                        }
                    }

                    // mode "JOIN GROUP"
                    if(received.startsWith("JOINGROUP")) {
                        isPersonalAct = true;
                        String nameOfGroup = received.substring(10);
                        synchronized (this) {
                            if (checkGroupExist(nameOfGroup)) {
                                groupName.add(nameOfGroup);
                                this.os.writeUTF("Join group " + nameOfGroup + " successfully! [sys_msg]");
                                this.os.writeUTF("Gr:" + nameOfGroup + " online");
                                displayMsg(this.clientName.substring(1) + " join group " + nameOfGroup);
                            } else {
                                this.os.writeUTF("Group " + received.substring(10) + " not exists! [sys_msg]");
                            }
                        }
                    }

                    // mode "SEND"
                    if (received.indexOf("SEND") >= 0) {
                        isStartSend = true;
                        String words[];
                        if(received.startsWith("@")) {
                            words = received.split("\\s", 3); // send private
                        } else {
                            words = received.split("\\s", 2); // send to everyone
                        }

                        if (words.length == 2) {
                            downloadFile("./storage/server/" + words[1]); // send private
                        } else {
                            downloadFile("./storage/server/" + words[2]); // send to everyone
                        }
                    }

                    // turn on mode "DOWNLOAD"
                    if (received.startsWith("DOWNLOAD")) {
                        String[] parts = received.split("\\s", 2);
                        isPersonalAct = true;
//                        if (parts.length == 2) {
                            if (checkFileName(parts[1])) {
                                synchronized (this) {
                                    this.os.writeUTF("[sys_msg] Download file successfully " + parts[1]);
                                }
                                displayMsg("[" + clientName.substring(1) + "] Download file " + parts[1]);
                                sendFile("./storage/server/" + parts[1]);
                            } else {
                                received = (parts[1] + " not exist");
                                synchronized (this) {
                                    this.os.writeUTF(received);
                                }
                                displayMsg("[sys] [" + clientName.substring(1) + "] File download request doesn't " +
                                        "exist" );
                            }
//                        }
                    }

                    // if the message is private sent it to the given client.
                    if (!isPersonalAct) { //avoid mode "DOWNLOAD"
                        if (received.startsWith("@")) {
                            String[] words = received.split("\\s", 2);
                            if (words.length > 1 && words[1] != null) {
                                words[1] = words[1].trim();
                                if (!words[1].isEmpty()) {
                                    if(words[0].indexOf(":") >= 0) {
                                        String nameOfGroup = words[0].substring(4);
                                        synchronized (this) {
                                            for (int i = 0; i < maxClients; ++i) {
                                                if (clients[i] != null && clients[i] != this && clients[i].groupName.contains(nameOfGroup)) {
                                                    if(isStartSend) {
                                                        clients[i].os.writeUTF("[" + this.clientName.substring(1) +
                                                                " to " + words[0].substring(1) + "] Send file " + words[1].substring(5) + " [sys_msg]");
                                                        displayMsg("[" + this.clientName.substring(1) + " to " + words[0].substring(1) + "] " + words[1].substring(5));
                                                        this.os.writeUTF("[you to " + words[0].substring(1) + "] Send" +
                                                                " file " + words[1].substring(5) + " successfully [sys_msg]");
                                                    } else {
                                                        clients[i].os.writeUTF("[" + this.clientName.substring(1) + " to " + words[0].substring(1) + "] " + words[1]);
                                                        displayMsg("[" + this.clientName.substring(1) + " to " + words[0].substring(1) + "] " + words[1]);
                                                        this.os.writeUTF("[you to " + words[0].substring(1) + "] " + words[1]);
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        synchronized (this) {
                                            for (int i = 0; i < maxClients; i++) {
                                                if (clients[i] != null && clients[i] != this
                                                        && clients[i].clientName != null
                                                        && clients[i].clientName.equals(words[0])) {
                                                    if (isStartSend) {
                                                        clients[i].os.writeUTF("[" + name + " to you] Send file " + words[1].substring(5) + " [sys_msg]");
                                                        displayMsg("[" + name + " to " + clients[i].clientName.substring(1) + "] Send file " + words[1].substring(5));
                                                        this.os.writeUTF("[you to " + clients[i].clientName.substring(1) + "] Send file " + words[1].substring(5) + " successfully [sys_msg]");
                                                    } else {
                                                        clients[i].os.writeUTF("[" + name + " to you] " + words[1]);
                                                        displayMsg("[" + name + " to " + clients[i].clientName.substring(1) + "] " + words[1]);
                                                        this.os.writeUTF("[you to " + clients[i].clientName.substring(1) + "] " + words[1]);
                                                    }
                                                    break;
                                                }
                                                if (i == (maxClients - 1)) {
                                                    this.os.writeUTF(words[0].substring(1) + " not connected " +
                                                            "[sys_msg]");
                                                    displayMsg("[" + this.clientName.substring(1) + " to " + words[0].substring(1) + "] " + words[0].substring(1) + " not connected");
                                                }
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
                                        if(isStartSend) {
                                            clients[i].os.writeUTF("[" + name + "] " + "Send file " + received.substring(5) + " [sys_msg]");
                                        } else {
                                            clients[i].os.writeUTF("[" + name + "] " + received);
                                        }
                                    }
                                }
                                displayMsg("[" + name + " to everybody] " + received);
                            }
                        }
                    }

                    // exit mode "DOWNLOAD"
                    isPersonalAct = false;
                    // exit mode "SEND"
                    isStartSend = false;

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            synchronized (this) {
                for (int i=0; i<maxClients; i++) {
                    if (clients[i] != null && clients[i] != this && clients[i].clientName != null) {
                        clients[i].os.writeUTF("[sys] " + name + " has left the chat room.");
                        clients[i].os.writeUTF(name + " exit");
                    }
                }

            }
            os.writeUTF("Bye User");
            server.displayEvent(this.clientName.substring(1) + " loses connection with Server");

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

    private String formatMsg(String msg) {
        return "[" + server.getSdf().format(Calendar.getInstance().getTime()) + "] " + msg;
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

    public boolean checkGroupExist(String groupname) {
        for(int i=0; i<maxClients; ++i) {
            if(clients[i] != null && clients[i].groupName.contains(groupname))
                return true;
        }

        return false;
    }

    // gửi file tới Client yêu cầu
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

    // download file do Clients gửi lên
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