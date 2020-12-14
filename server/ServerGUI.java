import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ServerGUI extends JFrame implements WindowListener, ActionListener {
    private JButton stopStart;
    private JTextArea chat, event;
    private JTextField tPortNumber;
    private Server server;

    public ServerGUI(int port) {
        super("Chat Server");
        server = null;
        buildGUI(port);
    }

    private void buildGUI(int port) {
        JPanel north = new JPanel();
        north.add(new JLabel("Port number: "));
        tPortNumber = new JTextField(" " + port);
        north.add(tPortNumber);
        stopStart = new JButton("Start");
        stopStart.addActionListener(this);
        north.add(stopStart);
        add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(2,1));
        chat = new JTextArea("Chat room.\n",80, 1);
        chat.setEditable(false);
        //appendRoom("Chat room.\n");
        center.add(new JScrollPane(chat));
        event = new JTextArea("Events log.\n",80,1);
        event.setEditable(false);
        //appendEvent("Events log.\n");
        center.add(new JScrollPane(event));
        add(center);

        addWindowListener(this);
        setSize(400, 600);
        setResizable(false);
        setVisible(true);
    }

    public void appendEvent(String s) {
        event.append(s);
        event.setCaretPosition(event.getText().length() - 1);
    }

    public void appendRoom(String s) {
        chat.append(s);
        chat.setCaretPosition(chat.getText().length() - 1);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (server != null) {
            server.stop();
            server = null;
            tPortNumber.setEditable(true);
            stopStart.setText("Start");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(tPortNumber.getText().trim());
        }
        catch(Exception er) {
            appendEvent("Invalid port number");
            return;
        }
        // create a new Server
        server = new Server(port, this);
        // and start it as a thread
        new ServerRunning().start();
        stopStart.setText("Stop");
        tPortNumber.setEditable(false);

    }

    public static void main(String[] args) {
        new ServerGUI(8080);
    }

    @Override
    public void windowClosing(WindowEvent e) {
        if(server != null) {
            try {
                server.stop();          // ask the server to close the conection
            } catch(Exception eClose) {
                eClose.printStackTrace();
            }
            server = null;
        }
        // dispose the frame
        dispose();
        System.exit(0);
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    class ServerRunning extends Thread {
        public void run() {
            server.init();         // should execute until if fails
            // the server failed
            stopStart.setText("Start");
            tPortNumber.setEditable(true);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String time = "[" + sdf.format(Calendar.getInstance().getTime()) + "] ";
            appendEvent(time + "Server crashed\n");
            server = null;
        }
    }
}
