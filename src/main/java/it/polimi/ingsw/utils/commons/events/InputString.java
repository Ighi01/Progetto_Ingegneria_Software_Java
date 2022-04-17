package it.polimi.ingsw.utils.commons.events;

import it.polimi.ingsw.utils.commandLine.CommandPrompt;
import it.polimi.ingsw.utils.observerPattern.Observer;
import it.polimi.ingsw.utils.stateMachine.Event;

import java.io.IOException;

/**
 * Evento di inserimento di una parola da parte dell'utente,
 * l'utente scrive una parola di suo gradimento e preme invio.
 *
 * * @author Fernando
 */

public class InputString extends Event implements Observer {
    private CommandPrompt commandPrompt;

    public InputString() throws IOException {
        super("\"L'utente ha scitto una parola a piacere\"" );
        this.commandPrompt = CommandPrompt.getInstance();
        this.subscribe();
    }

    @Override
    public void update(Object message) throws IOException, InterruptedException {
        fireStateEvent();
    }

    @Override
    public void subscribe() {
        this.commandPrompt.subscribeObserver(this);
    }

    @Override
    public void unSubscribe() {
        this.commandPrompt.unsubscribeObserver(this);
    }
}