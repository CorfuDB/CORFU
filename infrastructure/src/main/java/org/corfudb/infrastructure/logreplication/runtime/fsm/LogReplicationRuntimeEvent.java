package org.corfudb.infrastructure.logreplication.runtime.fsm;

import lombok.Data;
import org.corfudb.infrastructure.logreplication.replication.fsm.LogReplicationEvent;

import java.util.UUID;

/**
 * This class represents a Log Replication Runtime Event, i.e., an action which
 * triggers a valid transition in the Log Replication Runtime FSM.
 */
@Data
public class LogReplicationRuntimeEvent {
    /**
     * Enum listing the various type of LogReplicationRuntimeEvent.
     */
    public enum LogReplicationRuntimeEventType {
        ON_CONNECTION_UP,
        ON_CONNECTION_DOWN,
        REMOTE_LEADER_NOT_FOUND,
        REMOTE_LEADER_FOUND,
        REMOTE_LEADER_LOSS,
        LOCAL_LEADER_LOSS,
        ERROR,
        NEGOTIATION_COMPLETE,
        NEGOTIATION_FAILED
    }

    /*
     * Communication Event Type
     */
    private LogReplicationRuntimeEventType type;

    /*
     * Endpoint associated to this communication event.
     */
    private String endpoint;

    /*
     *  Negotiation result, input to Log Replication State Machine
     */
    private LogReplicationEvent negotiationResult;

    /*
     * Exception for ON_ERROR event
     */
    private Throwable t;

    /**
     * Constructor
     *
     * @param type runtime event type
     */
    public LogReplicationRuntimeEvent(LogReplicationRuntimeEventType type) {
        this.type = type;
    }

    /**
     * Constructor
     *
     * @param type runtime event type
     */
    public LogReplicationRuntimeEvent(LogReplicationRuntimeEventType type, String endpoint) {
        this.type = type;
        this.endpoint = endpoint;
    }

    /**
     * Constructor
     *
     * @param type runtime event type
     * @param negotiationResult negotiation result
     */
    public LogReplicationRuntimeEvent(LogReplicationRuntimeEventType type, LogReplicationEvent negotiationResult) {
        this.type = type;
        this.negotiationResult = negotiationResult;
    }

    /**
     * Constructor
     *
     * @param type runtime event type
     * @param t throwable for this error
     */
    public LogReplicationRuntimeEvent(LogReplicationRuntimeEventType type, Throwable t) {
        this.type = type;
        this.t = t;
    }

}
