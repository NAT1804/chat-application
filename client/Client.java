/*
Ho va ten: Nguyen Anh Tuan
MSSV: 18021376
Mo ta: file Client
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

import java.net.Socket;

public class Client{

    private Socket socket = null;
    private Scanner scn = null;
    private DataInputStream dis = null;
    private DataOutputStream dos = null;
    private ClientGUI clientGUI = null;
    private String host;
    private int port;
    private SimpleDateFormat sdf;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public Client(String host, int port, ClientGUI clientGUI) {
        this.host = host;
        this.port = port;
        this.clientGUI = clientGUI;
        this.sdf = new SimpleDateFormat("HH:mm:ss dd/MM/YYYY");
    }

    public boolean init() {
        try {
            socket = new Socket(host, port);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            //send(socket.getInetAddress() + ":" + socket.getPort() + " connected");

            Thread receiveThread = new Thread(){
                @Override
                public void run() {
                    try {
                        String received;
                        while(true) {
                            received = dis.readUTF().trim();

                            if (received.indexOf("online") >= 0) {
                                displayUser(received);
                            }
                            else if (received.indexOf("exit") >= 0) {
                                removeUser(received);
                            }
                            else {
                                String words[] = received.split("\\s", 5);
                                if (received.startsWith("Download")) {
                                    downloadFile("./storage/client/" + words[4]);
                                }
                                display(received);
                                if (received.equals("Bye User")) {
                                    close();
                                    break;
                                }
                            }
                        }
                    } catch (IOException ioe) {
                        display("Server has close the connect " + ioe);
                    }
                }
            };
            receiveThread.start();
        } catch (UnknownHostException uhe) {
            display("Fault: " + uhe);
            return false;
        } catch (IOException ioe) {
            display("Fault: " + ioe);
            return false;
        }
        return true;
    }

    // hàm hiển thị tin nhắn
    public void display(String msg) {
        String mess = "[" + sdf.format(Calendar.getInstance().getTime()) + "] " + msg;
        if (clientGUI != null)
            clientGUI.appendRoom(mess + "\n");
    }

    // Hàm hiển thị user online
    public void displayUser(String msg) {
        if (clientGUI != null)
            clientGUI.appendUser(msg + "\n");
    }

    // Hàm xóa user online khi ngắt kết nối
    public void removeUser(String msg) {
        if (clientGUI != null)
            clientGUI.delUser(msg);
    }

    public void send(String tosend) {
        try {
            dos.writeUTF(tosend);
        } catch (IOException ioe) {
            display("Fault: " + ioe);
        }
    }

    public void close(){
        try {
            dis.close();
            dos.close();
            socket.close();
            if (clientGUI != null)
                clientGUI.connectionFailed();
        }
        catch (IOException ioe)
        {
            System.out.println(ioe);
        }
    }

    // gửi file cho Server
    public void sendFile(String filename) throws IOException {
        File infile = new File(filename);
        FileInputStream fis = new FileInputStream(infile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        byte[] contents;
        long fileLength = infile.length();
        long current = 0;
        dos.writeLong(fileLength);
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

            dos.write(contents);

        }
        dos.flush();
    }

    // download file từ Server
    public void downloadFile(String filename) throws IOException {
        FileOutputStream fos    = new FileOutputStream(filename);
        BufferedOutputStream bos= new BufferedOutputStream(fos);
        byte[] contents         = new byte[1024];
        long fileLength         = dis.readLong();
        int byteRead            = 0;
        int size                = 0;
        while(size < fileLength) {
            byteRead = dis.read(contents);
            bos.write(contents, 0, byteRead);
            size += byteRead;
        }

        bos.flush();
        //fos.close();
    }

    // --- main ---
    public static void main(String[] args) {
        final int DEFAULT_PORT = 8080;
        String host = "localhost";
        Client client = new Client(host, DEFAULT_PORT);

        client.init();
    }
}
