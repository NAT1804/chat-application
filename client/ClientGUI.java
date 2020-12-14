import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

public class ClientGUI extends JFrame implements WindowListener, ActionListener {
    private JLabel label;
    private JTextField tf, tfUsername, tfServer, tfPort, tfFile;
    private JButton connect, exit, upload, send, download;
    private JTextArea ta, list;
    private JComboBox comboBox;
    private boolean connected;
    private Client client;
    private int port;
    private String host;
    //private int firstInput;

    public ClientGUI(String host, int port) {
        super("Chat Client");
        this.host = host;
        this.port = port;
        //this.firstInput = 0;
        buildGUI(host ,port);
    }

    private void buildGUI(String host, int port) {

        /* --- The NorthPanel ---*/
        JPanel northPanel = new JPanel(new GridLayout(1,1));
        JPanel header = new JPanel(new GridLayout(1,6));
        tfServer = new JTextField(host);
        tfPort = new JTextField("" + port);
        header.add(new JLabel("Server Address: ", SwingConstants.RIGHT));
        header.add(tfServer);
        header.add(new JLabel("Port Number: ", SwingConstants.RIGHT));
        header.add(tfPort);
        northPanel.add(header);
        header.add(new JLabel("User Name: ", SwingConstants.RIGHT));
        tfUsername = new JTextField("");
        header.add(tfUsername);
        tfUsername.requestFocus();
        add(northPanel, BorderLayout.NORTH); // add north panel to the frame
        /* --- end ---*/

        /* --- The WestPanel ---*/
        JPanel westPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcWest = new GridBagConstraints();
        gbcWest.fill = GridBagConstraints.HORIZONTAL;
//        gbcWest.weighty = 1;
//        gbcWest.weightx = 1;
        gbcWest.gridx = 0;
        gbcWest.gridy = 0;
        gbcWest.gridwidth = 1;
        gbcWest.anchor = GridBagConstraints.PAGE_START;
        connect = new JButton("Connect");
        connect.addActionListener(this);
        westPanel.add(connect, gbcWest);
        gbcWest.gridx = 0;
        gbcWest.gridy = 1;
        gbcWest.anchor = GridBagConstraints.PAGE_START;
        exit = new JButton("Exit");
        exit.addActionListener(this);
        exit.setEnabled(false);
        westPanel.add(exit, gbcWest);
//        gbcWest.gridx = 0;
//        gbcWest.gridy = 2;
//        gbcWest.anchor = GridBagConstraints.PAGE_END;
//        create = new JButton("Create Group");
//        create.addActionListener(this);
//        create.setEnabled(false);
//        westPanel.add(create, gbcWest);
        gbcWest.gridx = 0;
        gbcWest.gridy = 2;
        download = new JButton("Download");
        download.addActionListener(this);
        download.setEnabled(false);
        westPanel.add(download, gbcWest);
        add(westPanel, BorderLayout.LINE_START);
        /* --- end ---*/

        /* --- The CenterPanel ---*/
        JPanel centerPanel = new JPanel(new GridLayout(2,1));
        centerPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbcCentral = new GridBagConstraints();
        gbcCentral.fill = GridBagConstraints.BOTH;
        gbcCentral.gridx = 0;
        gbcCentral.gridy = 0;
        centerPanel.add(new JLabel("Chat room\n"), gbcCentral);
        ta = new JTextArea("Welcome to the Chat room!\n", 80, 1);
        ta.setLineWrap(true);
        ta.setFont(new Font("Serif", Font.PLAIN, 15));
        gbcCentral.ipady = 435;
        gbcCentral.ipadx = 310;
        gbcCentral.gridx = 0;
        gbcCentral.gridy = 1;
        centerPanel.add(new JScrollPane(ta), gbcCentral);
        ta.setEditable(false);
        add(centerPanel, BorderLayout.CENTER);
        centerPanel.setPreferredSize(new Dimension(200, 400));
        /* --- end ---*/

        /* --- The EastPanel ---*/
        JPanel eastPanel = new JPanel();
        eastPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbcEast = new GridBagConstraints();
        gbcEast.fill = GridBagConstraints.VERTICAL;
        gbcEast.gridx = 0;
        gbcEast.gridy = 0;
        gbcEast.gridwidth = GridBagConstraints.REMAINDER;
        JLabel labelListUser = new JLabel(" List user online ");
        labelListUser.setSize(new Dimension(150, 20));
        eastPanel.add(labelListUser, gbcEast);
        list = new JTextArea(80, 1);
        list.setFont(new Font("Serif", Font.PLAIN, 16));
        gbcEast.fill = GridBagConstraints.HORIZONTAL;
        gbcEast.ipady = 435;
        gbcEast.ipadx = 140;
        gbcEast.gridx = 0;
        gbcEast.gridy = 1;
        gbcEast.insets = new Insets(0,0,0,5);
        gbcEast.gridwidth = GridBagConstraints.REMAINDER;
        eastPanel.add(new JScrollPane(list), gbcEast);
        list.setEditable(false);
        add(eastPanel, BorderLayout.LINE_END);
        /* --- end ---*/

        /* --- The SouthPanel ---*/
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbcSouth = new GridBagConstraints();
        gbcSouth.fill = GridBagConstraints.HORIZONTAL;
        gbcSouth.gridx = 0;
        gbcSouth.gridy = 0;
        gbcSouth.gridwidth = 1;
        upload = new JButton("Upload");
        upload.addActionListener(this);
        upload.setEnabled(false);
        southPanel.add(upload, gbcSouth);
        gbcSouth.gridy = 0;
        gbcSouth.gridx = 1;
        gbcSouth.gridwidth = 4;
        gbcSouth.ipadx = 425;
        gbcSouth.fill = GridBagConstraints.HORIZONTAL;
        tfFile = new JTextField("No file selected");
        tfFile.setEditable(false);
        southPanel.add(tfFile, gbcSouth);
        gbcSouth.gridx = 5;
        gbcSouth.ipadx = 0;
        gbcSouth.gridwidth = 1;
        send = new JButton("Send");
        send.addActionListener(this);
        send.setEnabled(false);
        southPanel.add(send, gbcSouth);
        gbcSouth.gridx = 0;
        gbcSouth.gridy = 1;
        gbcSouth.gridwidth = 2;
        label = new JLabel("Enter your message below", SwingConstants.LEFT);
        southPanel.add(label, gbcSouth);
        gbcSouth.gridy = 2;
        gbcSouth.gridwidth = 1;
        gbcSouth.fill = GridBagConstraints.HORIZONTAL;
        gbcSouth.insets = new Insets(0,0,10,0);
        String option[] = {"Everybody"};
        comboBox = new JComboBox(option);
        comboBox.setEnabled(false);
        southPanel.add(comboBox, gbcSouth);
        gbcSouth.gridx = 1;
        gbcSouth.gridy = 2;
        gbcSouth.gridwidth = 5;
        gbcSouth.gridheight = 3;
        gbcSouth.ipadx = 425;
        gbcSouth.fill = GridBagConstraints.BOTH;
        tf = new JTextField("");
        tf.setBackground(Color.WHITE);
        tf.setEditable(false);
        southPanel.add(tf, gbcSouth);
        add(southPanel, BorderLayout.SOUTH);
        /* --- end ---*/

        /*--- frame ---*/
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
        setResizable(false);
        setLocationRelativeTo(null);
        /* --- end ---*/
    }

    public void appendRoom(String str) {
        ta.append(str);
        ta.setCaretPosition(ta.getText().length() - 1);
    }

    public void appendUser(String str) {
        String [] words = str.split("\\s");
        comboBox.addItem(words[0]);
        list.append(str);
        list.setCaretPosition(list.getText().length() - 1);
    }

    public void delUser(String str) {
        String [] words = str.split("\\s");
        comboBox.removeItem(words[0]);
    }

    public void connectionFailed() {
        connect.setEnabled(true);
        exit.setEnabled(false);
        //create.setEnabled(false);
        upload.setEnabled(false);
        send.setEnabled(false);
        comboBox.setEnabled(false);
        download.setEnabled(false);
        //label.setText("Enter your message below");
        tf.setEditable(false);
        tf.setText("");
        // let the user change them
        tfServer.setEditable(true);
        tfPort.setEditable(true);
        tfUsername.setEditable(true);
        // don't react to a <CR> after the username
        tf.removeActionListener(this);
        connected = false;
        //firstInput = 0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();

        // Button to
        if (o == exit) {
            client.send("/quit");
            return;
        }

        // Button to selecting file
        if (o == upload) {
            // create an object of JFileChooser class
            JFileChooser j = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
            // invoke the showsSaveDialog function to show the save dialog
            int r = j.showSaveDialog(null);
            // if the user selects a file
            if (r == JFileChooser.APPROVE_OPTION)
            {
                // set the label to the path of the selected file
                tfFile.setText(j.getSelectedFile().getAbsolutePath());
                return;
            }
            // if the user cancelled the operation
            else {
                tfFile.setText("No file selected");
                return;
            }
        }

        // Button to sending file
        if (o == send) {
            if (tfFile.getText().equals("No file selected")) {
                JOptionPane.showMessageDialog(this, "You need to select the file to send!");
            } else {
                try {
                    String dir = tfFile.getText();
                    int lastIndex = dir.lastIndexOf('\\');
                    String filename = dir.substring(lastIndex+1);
                    String target = (String) comboBox.getSelectedItem();
                    if (target.equals("Everybody")) {
                        client.send("SEND " + filename);
                    } else {
                        client.send("@" + target + " SEND " + filename);
                    }

                    client.sendFile(dir);
                    tfFile.setText("No file selected");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

        // Button to downloading
        if (o == download) {
            if (tf.getText().equals("")) {
                JOptionPane.showMessageDialog(this, "You need to input file name to download!");
                tf.requestFocus();
            } else {
                client.send("DOWNLOAD " + tf.getText());
                tf.setText("");
            }
        }

        // Enter message in Textfield tf
        if (connected) {
            String target = (String) comboBox.getSelectedItem();
            switch (target) {
                case "Everybody":
                    if (!tf.getText().equals("")) {
                        client.send(tf.getText());
                        tf.setText("");
                    }
                    break;
                default:
                    if (!tf.getText().equals("")) {
                        client.send("@" + target + " " + tf.getText());
                        tf.setText("");
                    }
                    break;
            }
        }

        // Button to connecting with Server
        if (o == connect) {
            String hostAddr = tfServer.getText().trim();
            if (hostAddr.length() == 0) {
                JOptionPane.showMessageDialog(this, "Host address can't be empty!");
                tfServer.requestFocus();
                return;
            }
            String portNumber = tfPort.getText().trim();
            if (portNumber.length() == 0) {
                JOptionPane.showMessageDialog(this, "Port can't be empty!");
                tfPort.requestFocus();
                return;
            }
            int portSelected = 0;
            try {
                portSelected = Integer.parseInt(portNumber);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            String username = tfUsername.getText().trim();
            if (username.length() == 0) {
                JOptionPane.showMessageDialog(this, "User name can't be empty!");
                tfUsername.requestFocus();
                return;
            }
            client = new Client(hostAddr, portSelected, this);
            if (!client.init()) return;

            // Send username
            client.send(username);

            tf.setEditable(true);
            tf.setText("");
            label.setText("Enter your message below");
            connected = true;

            // disable login button and enable other buttons
            connect.setEnabled(false);
            exit.setEnabled(true);
            //create.setEnabled(true);
            upload.setEnabled(true);
            send.setEnabled(true);
            comboBox.setEnabled(true);
            download.setEnabled(true);
            // disable the Server and Port JTextField
            tfServer.setEditable(false);
            tfPort.setEditable(false);
            tfUsername.setEditable(false);
            // Action listener for when the user enter a message
            tf.addActionListener(this);
            tf.requestFocus();
        }
    }

    public static void main(String[] args) {
        new ClientGUI("localhost", 8080);

    }

    @Override
    public void windowClosing(WindowEvent e) {
        if(client != null) {
            try {
                client.send("/quit"); // ask the server to close the conection
            } catch(Exception eClose) {
                eClose.printStackTrace();
            }
            System.out.println("Exit");
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
}
