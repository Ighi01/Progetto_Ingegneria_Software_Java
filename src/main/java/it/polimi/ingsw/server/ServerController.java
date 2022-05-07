package it.polimi.ingsw.server;

import it.polimi.ingsw.server.model.Model;
import it.polimi.ingsw.server.states.*;
import it.polimi.ingsw.utils.cli.CommandPrompt;
import it.polimi.ingsw.utils.stateMachine.Controller;
import it.polimi.ingsw.utils.stateMachine.Event;

import java.io.IOException;

public class ServerController{
    Model model;
    ConnectionModel connectionModel = new ConnectionModel();
    Idle idle = new Idle();
    Event start = new Event("[Controller Started]");
    Controller fsm = new Controller("Controllore del Server", idle);



    public ServerController() throws Exception {
        fsm.showDebugMessages();

        //Costruzione degli stati necessari
        SpecifyPortScreen specifyPortScreen = new SpecifyPortScreen();
        WaitFirstPlayer waitFirstPlayer = new WaitFirstPlayer(this);
        WaitFirstPlayerGameInfo waitFirstPlayerGameInfo = new WaitFirstPlayerGameInfo(this);
        WaitOtherClients waitOtherClients = new WaitOtherClients(this);
        CreateGame createGame = new CreateGame(this);
        ChooseAssistantCard chooseAssistantCard = new ChooseAssistantCard(this);

        // Dichiarazione delle transizioni tra gli stati
        fsm.addTransition(idle, start, specifyPortScreen);
        fsm.addTransition(specifyPortScreen, specifyPortScreen.portSpecified(), waitFirstPlayer);
        fsm.addTransition(waitFirstPlayer,waitFirstPlayer.gotFirstMessage(),waitFirstPlayerGameInfo);
        fsm.addTransition(waitFirstPlayerGameInfo,waitFirstPlayerGameInfo.gotNumOfPlayersAndGamemode(),waitOtherClients);
        fsm.addTransition(waitOtherClients, waitOtherClients.allClientsConnected(), createGame);
        fsm.addTransition(createGame, createGame.gameCreated(), chooseAssistantCard);
        // L'evento di start è l'unico che deve essere fatto partire manualmente
        start.fireStateEvent();
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Controller getFsm() {
        return fsm;
    }

    public ConnectionModel getConnectionModel() {
        return connectionModel;
    }

    public static void main(String[] args) {
        try {
            new ServerController();
        } catch (Exception e){e.printStackTrace();}
    }
}
