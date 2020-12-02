import java.io.*;
import java.net.UnknownHostException;
import java.util.Scanner;

import java.net.Socket;

public class Client {
    private static final int DEFAULT_PORT = 8080;
    private Socket socket = null;
    private Scanner scn = null;
    private DataInputStream dis = null;
    private DataOutputStream dos = null;

    public Client(String address, int port) {
        try {
            socket = new Socket(address, port);
            scn = new Scanner(System.in);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            Thread receiveThread = new Thread(){
                @Override
                public void run() {
                    try {
                        String received;
                        while(!(received = dis.readUTF()).trim().isEmpty()) {
                            System.out.print(received);
                            if (received.equals("Bye")) {
                                socket.close();
                                break;
                            }
                        }
                    } catch (IOException ioe) {
                        System.out.println(ioe);
                    }
                }
            };
            receiveThread.start();

            String tosend;
            while(!(tosend = scn.nextLine()).trim().isEmpty()) {
                dos.writeUTF(tosend);
                if(tosend.equals("/quit"))
                {
                    //System.out.println("Closing this connection : " + socket);
                    System.out.println("Connection closed");
                    break;
                }
            }

        } catch (UnknownHostException uhe) {
            System.out.println(uhe);
        } catch (IOException ioe) {
            System.out.println(ioe);
        }

        try {
            scn.close();
            dis.close();
            dos.close();
            //socket.close();
        }
        catch (IOException ioe)
        {
            System.out.println(ioe);
        }
    }

    public static void main(String[] args) {
        System.out.print("Input address to connecting with Server: ");
        String addr = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            addr = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Client client = new Client(addr, DEFAULT_PORT);
    }
}
