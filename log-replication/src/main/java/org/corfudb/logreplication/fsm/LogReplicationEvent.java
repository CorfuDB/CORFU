package org.corfudb.logreplication.fsm;

import lombok.Data;
import org.corfudb.logreplication.send.LogReplicationEventMetadata;

import java.util.UUID;

/**
 * This class represents a Log Replication Event.
 *
 * An event triggers the transition from one state to another.
 */
@Data
public class LogReplicationEvent {

    /**
     * Enum listing the various type of LogReplicationEvent.
     */
    public enum LogReplicationEventType {
        REPLICATION_START,          // External event which signals start of log replication process
        REPLICATION_STOP,           // External event which signals stop of log replication process
        SNAPSHOT_SYNC_REQUEST,      // External event which signals start of a snapshot sync (full-sync)
        SNAPSHOT_SYNC_CONTINUE,     // Internal event to continue snapshot sync (broken to accommodate multi-site
                                    // replication for a shared thread pool)
        SNAPSHOT_SYNC_COMPLETE,     // External/Internal event which signals snapshot sync has been completed
        LOG_ENTRY_SYNC_CONTINUE,    // Internal event to continue log entry sync (broken to accommodate multi-site
                                    // replication for a shared thread pool)
        SYNC_CANCEL,                // External/Internal event requesting to cancel sync (snapshot or log entry)
        LOG_ENTRY_SYNC_REPLICATED,  // External event which signals a log entry sync has been successfully replicated
                                    // on the standby site (ack)
        REPLICATION_SHUTDOWN        // External/Internal event which signals log replication to be terminated (only a
                                    // JVM restart can enable log replication after this)
    }

    /*
     *  Log Replication Event Identifier
     */
    private UUID eventID;

    /*
     *  Log Replication Event Type
     */
    private LogReplicationEventType type;

    /*
     *  Event Metadata
     */
    private LogReplicationEventMetadata metadata;

    /**
     * Constructor
     *
     * @param type log replication event type
     */
    public LogReplicationEvent(LogReplicationEventType type) {
        this(type, LogReplicationEventMetadata.empty());
    }

    /**
     * Constructor
     *
     * @param type log replication event type
     * @param metadata log replication event metadata
     *
\     */
    public LogReplicationEvent(LogReplicationEventType type, LogReplicationEventMetadata metadata) {
        this.type = type;
        this.eventID = UUID.randomUUID();
        this.metadata = metadata;
    }
}