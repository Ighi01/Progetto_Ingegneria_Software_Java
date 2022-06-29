package it.polimi.ingsw.utils.network.events;

import it.polimi.ingsw.utils.network.events.ParametersFromNetwork;

/**
 * This class make the users able to store the message and the result of its waiting (boolean value that is true when no one message is arrived) into a single object
 */
public class ResultOfWaiting {
    private final ParametersFromNetwork fromNetwork;
    final boolean response;

    public ResultOfWaiting(ParametersFromNetwork param, boolean resp) {
        this.fromNetwork = param;
        this.response = resp;
    }

    public ParametersFromNetwork getMessage() {
        return fromNetwork;
    }

    public boolean isNotArrived() {
        return response;
    }
}