package it.polimi.ingsw.server.controller_server;

import javax.naming.ldap.Control;

/**
 *  Una classe di default per implementare l'interfaccia IEvent.
 *  Quello che fa e' prendersi cura del listener e del metodo toString.
 */
public class Event implements IEvent {

    private String name;
    private ControllerServer listener;

    /**
     * Constructor to force naming of each event.
     * @param eventName The name used for logging
     */
    public Event(String eventName) {
        name = eventName;
    }

    /**
     * This causes the controller to transition to the next state if
     * possible.  If there is no transaction for the current state + this
     * event, it will throw an IllegalStateException.
     */
    public void fireStateEvent() {
        listener.fireStateEvent(this);
    }

    /**
     * The controller will set itself as listener for events from this
     * object.  I don't think it will ever need more than one listener,
     * hence the set instead of add; this should make it easier for
     * a developer to implement the interface (which will be useful if
     * you are also extending an event helper class)
     *
     * @param engine the controller that will get events
     */
    public void setStateEventListener(ControllerServer engine) {
        listener = engine;
    }

    /**
     * Used for logging purposes
     * @return the name set in the constructor.
     */
    public String toString() {
        return name;
    }
}