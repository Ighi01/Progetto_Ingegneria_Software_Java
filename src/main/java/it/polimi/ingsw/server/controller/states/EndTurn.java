package it.polimi.ingsw.server.controller.states;

import it.polimi.ingsw.server.controller.ServerController;
import it.polimi.ingsw.server.model.Model;
import it.polimi.ingsw.utils.stateMachine.Controller;
import it.polimi.ingsw.utils.stateMachine.Event;
import it.polimi.ingsw.utils.stateMachine.IEvent;
import it.polimi.ingsw.utils.stateMachine.State;

public class EndTurn extends State {
    private final Event goToAssistentCardPhase;

    private final ServerController serverController;


    public Event goToAssistentCardPhase() {
        return goToAssistentCardPhase;
    }

    public EndTurn(ServerController serverController) {
        super("[End Turn]");
        this.serverController = serverController;
        Controller controller = ServerController.getFsm();
        goToAssistentCardPhase= new Event("end turn");
        goToAssistentCardPhase.setStateEventListener(controller);
    }

    @Override
    public IEvent entryAction(IEvent cause) throws Exception {
        Model model = serverController.getModel();
        for(int i = 0; i< model.getTable().getClouds().size(); i++){
            if(model.getTable().getClouds().get(i).getStudentsAccumulator().size()==0) {
                boolean check = model.getTable().getClouds().get(i).charge(model.getTable().getBag());
                if (!check) {
                    model.setlastturn();
                    break;
                }
            }
        }
        model.nextPlayer();
        goToAssistentCardPhase().fireStateEvent();
        return super.entryAction(cause);
    }
}