package org.corfudb.transport.logreplication;
;

/**
 * This class indicates an invalid transition has been detected,
 * i.e., unexpected event has been processed when in a given state.
 *
 * @author annym
 */
public class IllegalTransitionException extends Exception {

    /**
     * Constructor
     *
     * @param event     incoming lock event
     * @param stateType current state type
     */
    public IllegalTransitionException(CommunicationEvent.CommunicationEventType event, CommunicationStateType stateType) {
        super(String.format("Illegal transition for event %s while in state %s", event, stateType));
    }

}