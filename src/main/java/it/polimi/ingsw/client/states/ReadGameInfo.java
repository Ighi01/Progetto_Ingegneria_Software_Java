package it.polimi.ingsw.client.states;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.view.View;
import it.polimi.ingsw.utils.stateMachine.Controller;
import it.polimi.ingsw.utils.stateMachine.IEvent;

import java.io.IOException;

public class ReadGameInfo extends ReadFromTerminal {
    public ReadGameInfo(View view, ClientModel clientModel, Controller controller) throws IOException {
        super(view, clientModel, controller, 2, "GAMEINFO");
    }

    @Override
    public void exitAction(IEvent cause) throws IOException {
            clientModel.setNumofplayer(Integer.parseInt(clientModel.getFromTerminal().get(0)));
            clientModel.setGamemode(clientModel.getFromTerminal().get(1));
    }
}
