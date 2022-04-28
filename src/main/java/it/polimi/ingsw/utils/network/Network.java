package it.polimi.ingsw.utils.network;

import java.awt.event.*;
import javax.swing.*;


public class Network implements ActionListener{
    private static NetworkHandler connection;
    private static boolean gotConnect;
    private static Network instance = null;

    // elementi grafici "virtuali" che astraggono i dettagli implementativi
    private static JButton serverButton,clientButton, sendButton;
    private static JTextField IPAddress,port,textToSend;
    private static JTextArea textReceived;
    private static JButton disconnectButton;



    public void actionPerformed(ActionEvent event){
        if(event.getSource() == clientButton){
            connection = new NetworkHandler(IPAddress.getText(), Integer.parseInt(port.getText()), this);
            gotConnect = connection.connect();
            if(gotConnect){
                System.out.println("[Connected to server]");
            }

        }else if(event.getSource() == serverButton){
            connection = new NetworkHandler(Integer.parseInt(port.getText()), this);
            gotConnect = connection.connect();
            if(gotConnect){
                System.out.println("[Listening for Clients]");
            }

        }else if(event.getSource() == sendButton){
            if(connection != null){
                connection.sendText(textToSend.getText());
            }
        }else if(event.getSource() == connection){
            textReceived.setText(connection.readText());
            textReceived.setCaretPosition(textReceived.getDocument().getLength());

        }else if(event.getSource() == disconnectButton){
            connection.disconnect();
        }

    }

    private Network(){
        serverButton = new JButton();
        serverButton.addActionListener(this);
        clientButton = new JButton();
        clientButton.addActionListener(this);
        sendButton = new JButton();
        sendButton.addActionListener(this);
        IPAddress = new JTextField();
        port = new JTextField();
        textToSend = new JTextField();
        textToSend.addActionListener(this);
        //textReceived = new JTextArea();
        disconnectButton = new JButton();
        disconnectButton.addActionListener(this);
    }

    public static JTextArea checkNewMessages() {
        if (instance.textReceived == null){
            instance.textReceived = new JTextArea();
        }
        return instance.textReceived;
    }

    public static void setupServer(String port){
        if (instance == null){
            instance = new Network();
        }
        instance.port.setText(port);
        instance.serverButton.doClick();
    };

    public static void setupClient(String ip,String port){
        if (instance == null){
            instance = new Network();
        }
        instance.IPAddress.setText(ip);
        instance.port.setText(port);
        instance.clientButton.doClick();
    };

    public static void disconnect(){
        instance.disconnectButton.doClick();
    }

    public static boolean isConnected() {
        return instance.gotConnect;
    }

    public static void send(String message){
        instance.textToSend.setText(message);
        instance.sendButton.doClick();
    }

    public static String getMyIp(){
        return instance.connection.getMyAddress();
    }
}