import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Server {
    private int port;
    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;
    private final int maxClients = 10;
    private ClientHandler clients[] = new ClientHandler[maxClients];
    private boolean servercontinue = false;
    private ServerGUI serverGUI = null;
    private SimpleDateFormat sdf = null;

    public Server(int port) {
        this.port = port;
    }

    public Server(int port, ServerGUI serverGUI) {
        this.port = port;
        this.serverGUI = serverGUI;
        this.sdf = new SimpleDateFormat("HH:mm:ss");
    }

    public ServerGUI getServerGUI() {
        return serverGUI;
    }

    public SimpleDateFormat getSdf() {
        return sdf;
    }

    public void init() {
        servercontinue = true;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server start");

            while(servercontinue) {
                displayEvent("Waiting connections on port " + this.port + "...");
                clientSocket = serverSocket.accept();

                // stop waiting clients
                if (!servercontinue) break;

                int i;
                for (i = 0; i<maxClients; i++) {
                    if (clients[i] == null) {
                        clients[i] = new ClientHandler(clientSocket, clients, this);
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

    protected void stop() {
        servercontinue = false;
        try {
            new Socket("localhost", port);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void displayEvent(String msg) {
        String mess = "[" + sdf.format(Calendar.getInstance().getTime()) + "] " + msg;
        if (serverGUI != null)
            serverGUI.appendEvent(mess + "\n");
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
        server.init();
    }
}

