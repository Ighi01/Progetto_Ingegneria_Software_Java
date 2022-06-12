package it.polimi.ingsw.client.view.gui;

import com.google.gson.Gson;
import it.polimi.ingsw.client.GUI;
import it.polimi.ingsw.client.model.ClientModel;
import it.polimi.ingsw.utils.network.Network;
import it.polimi.ingsw.utils.network.events.ParametersFromNetwork;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public class SetupGame implements Initializable {
    private final GUI gui = new GUI();
    private final Gson gson = new Gson();
    private int connectedPlayers;
    private ClientModel receivedClientModel;
    private boolean isToReset, waitForFirst = true;
    private int myID;

    ParametersFromNetwork message;

    private boolean notread = false;

    @FXML
    private Label connectedOnIp = new Label();
    @FXML
    private Label connectedOnPort = new Label();


    @FXML
    private Label otherPlayersLabel = new Label();
    @FXML
    public Label numberOfPlayersLabel;
    @FXML
    public Label gameModeLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.connectedOnIp.setText("Connected on IP: " + this.gui.getClientModel().getIp());
        this.connectedOnPort.setText("Connected on Port: " + this.gui.getClientModel().getPort());
        this.connectedPlayers = 0;
        this.gui.currNode = otherPlayersLabel;
        isToReset = false;
    }

    public void set2Players(MouseEvent mouseEvent) {
        this.gui.getClientModel().setNumofplayer(2);
        numberOfPlayersLabel.setText("Number of players: " + this.gui.getClientModel().getNumofplayer());
    }

    public void set3Players(MouseEvent mouseEvent) {
        this.gui.getClientModel().setNumofplayer(3);
        numberOfPlayersLabel.setText("Number of players: " + this.gui.getClientModel().getNumofplayer());
    }

    public void set4Players(MouseEvent mouseEvent) {
        this.gui.getClientModel().setNumofplayer(4);
        numberOfPlayersLabel.setText("Number of players: " + this.gui.getClientModel().getNumofplayer());
    }

    public void setPrincipiant(MouseEvent mouseEvent) {
        this.gui.getClientModel().setGameMode("PRINCIPIANT");
        this.gameModeLabel.setText("Game mode: principiant");
    }

    public void setExpert(MouseEvent mouseEvent) {
        this.gui.getClientModel().setGameMode("EXPERT");
        this.gameModeLabel.setText("Game mode: expert");
    }

    public void start(MouseEvent mouseEvent) throws InterruptedException, IOException { //todo : questo è lo start da primo client, c'è da fare anche quello da non primo client
        if (this.gui.getClientModel().getNumofplayer() != 2 && this.gui.getClientModel().getNumofplayer() != 3 && this.gui.getClientModel().getNumofplayer() != 4) {
            this.otherPlayersLabel.setText("ERROR: Please select a number of players!");
        } else if (this.gui.getClientModel().getGameMode() == null) {
            this.otherPlayersLabel.setText("ERROR: Please select a game mode!");
        } else {
            this.gui.currNode = otherPlayersLabel;
            boolean responseReceived = false;
            Network.send(gson.toJson(this.gui.getClientModel()));

            while (!responseReceived) {
                System.out.println("invio al server in attesa di ack...");
                ParametersFromNetwork ack = new ParametersFromNetwork(1);
                ack.enable();
                long start = System.currentTimeMillis();
                long end = start + 15 * 1000L;
                boolean check = ack.waitParametersReceived(5);
                if (check || System.currentTimeMillis() >= end) {
                    System.out.println("\n\nServer non ha dato risposta");
                    Network.disconnect();
                    gui.currNode = otherPlayersLabel;
                    this.otherPlayersLabel.setText("...Server non ha dato alcuna risposta, mi disconnetto...");
                    TimeUnit.SECONDS.sleep(5);
                    System.exit(0);
                }

                if (gson.fromJson(ack.getParameter(0), ClientModel.class).getClientIdentity() == this.gui.getClientModel().getClientIdentity()) {
                    responseReceived = true;
                }
            }
            System.out.println("[Conferma ricevuta]");
            System.out.println("In attesa che gli altri giocatori si colleghino...");
            this.gui.currNode = otherPlayersLabel;
            this.otherPlayersLabel.setText("...Waiting for other players to join the game...");

            myID = gui.getClientModel().getClientIdentity();
            waitings();
        }
    }

    public void waitings() throws InterruptedException {
        while(!isToReset){

        if (!notread) {
            message = new ParametersFromNetwork(1);
            message.enable();
            long start = System.currentTimeMillis();
            long end = start + 15 * 1000L;
            if (waitForFirst) {
                message.waitParametersReceived();
            } else {
                boolean check = message.waitParametersReceivedMax(end);
                if (check) {
                    System.out.println("\n\nServer non ha dato risposta");
                    Network.disconnect();
                    this.gui.currNode = otherPlayersLabel;
                    this.otherPlayersLabel.setText("...Server si è disconnesso, mi disconnetto...");
                    TimeUnit.SECONDS.sleep(5);
                    System.exit(0);
                }
            }
        }
        notread = false;
        //System.out.println(message.getParameter(0));
        ClientModel tryreceivedClientModel = gson.fromJson(message.getParameter(0), ClientModel.class);
        if (!Objects.equals(tryreceivedClientModel.getTypeOfRequest(), "CONNECTTOEXISTINGGAME")) {
            receivedClientModel = tryreceivedClientModel;
            if (Network.disconnectedClient()) {
                Network.disconnect();
                System.out.println("Il gioco è terminato a causa della disconnessione di un client");
                isToReset = true;
            }

            if (receivedClientModel.isGameStarted() && receivedClientModel.NotisKicked()) {
                waitForFirst = false;

                // Il messaggio è o una richiesta o una risposta

                // se il messaggio non è una risposta di un client al server vuol dire che
                if (!receivedClientModel.isResponse() && receivedClientModel.getTypeOfRequest() != null) {
                    // il messaggio è una richiesta del server alla view di un client

                    // se il messaggio è rivolto a me devo essere io a compiere l'azione
                    if (receivedClientModel.getClientIdentity() == myID) {
                        // il messaggio è rivolto a me
                        try {
                            System.out.println("request to me");
                            gui.setClientModel(receivedClientModel);
                            gui.requestToMe();

                            Thread t = new Thread(() -> {
                                try {
                                    wait_pings();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            t.start();
                            return;
                        } catch (InterruptedException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    if (receivedClientModel.getClientIdentity() != myID && receivedClientModel.getTypeOfRequest() != null &&
                            !receivedClientModel.isPingMessage() &&
                            !receivedClientModel.getTypeOfRequest().equals("TRYTORECONNECT") &&
                            !receivedClientModel.getTypeOfRequest().equals("DISCONNECTION")) {
                        try {
                            System.out.println("request to other");
                            gui.setClientModel(receivedClientModel);
                            gui.requestToOthers();

                            Thread t = new Thread(() -> {
                                message = new ParametersFromNetwork(1);
                                message.enable();
                                long start = System.currentTimeMillis();
                                long end = start + 15 * 1000L;
                                boolean check = false;
                                try {
                                    check = message.waitParametersReceivedMax(end);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                if (check) {
                                    System.out.println("\n\nServer non ha dato risposta");
                                    Network.disconnect();
                                    this.gui.currNode = otherPlayersLabel;
                                    this.otherPlayersLabel.setText("...Server si è disconnesso, mi disconnetto...");
                                    try {
                                        TimeUnit.SECONDS.sleep(5);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    System.exit(0);
                                }
                                notread = true;
                                Platform.runLater(() -> {
                                    try {
                                        waitings();
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                            });
                            t.start();
                            return;

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

        }
    }

    }

    public synchronized void wait_pings() throws InterruptedException {
        ClientModel tryreceivedClientModel;
        do {
            message = new ParametersFromNetwork(1);
            message.enable();
            long start = System.currentTimeMillis();
            long end = start + 15 * 1000L;
            boolean check = message.waitParametersReceivedMax(end);
            if (check) {
                System.out.println("\n\nServer non ha dato risposta");
                Network.disconnect();
                this.gui.currNode = otherPlayersLabel;
                this.otherPlayersLabel.setText("...Server si è disconnesso, mi disconnetto...");
                TimeUnit.SECONDS.sleep(5);
                System.exit(0);
            }

            tryreceivedClientModel = gson.fromJson(message.getParameter(0), ClientModel.class);

            if (!Objects.equals(tryreceivedClientModel.getTypeOfRequest(), "CONNECTTOEXISTINGGAME")) { //todo received.gettype.equals("connect")

                receivedClientModel = tryreceivedClientModel;

                if (receivedClientModel.isGameStarted() && receivedClientModel.NotisKicked()) {
                    // Il messaggio è o una richiesta o una risposta

                    // se il messaggio non è una risposta di un client al server vuol dire che
                    if (receivedClientModel.isResponse().equals(false) && receivedClientModel.getTypeOfRequest() != null) {
                        // il messaggio è una richiesta del server alla view di un client

                        // se il messaggio è rivolto a me devo essere io a compiere l'azione
                        if (receivedClientModel.getClientIdentity() == myID) {
                            // il messaggio è rivolto a me
                            if (receivedClientModel.isPingMessage()) {
                                gui.requestPing();
                            }
                        }
                    }
                }
            }
        } while (!Objects.equals(tryreceivedClientModel.getTypeOfRequest(), "CONNECTTOEXISTINGGAME") && receivedClientModel.isGameStarted() && receivedClientModel.NotisKicked() && receivedClientModel.isResponse().equals(false) && receivedClientModel.getTypeOfRequest() != null && receivedClientModel.getClientIdentity() == myID && receivedClientModel.isPingMessage());
        notread = true;
        Platform.runLater(() -> {
            try {
                waitings();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
