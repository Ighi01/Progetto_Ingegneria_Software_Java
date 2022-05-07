package it.polimi.ingsw.utils.observerPattern;


import java.io.IOException;

public interface Observer {
    /**
     * se vieni notificato cosa vuoi fare?
     */
    public  void update(Object message) throws Exception;

    /**
     * iscriviti
     */
    public void subscribe();

    /**
     * disiscriviti
     */
    public void unSubscribe();

}
