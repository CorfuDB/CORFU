package org.corfudb.logreplication.send;

import java.util.UUID;

/**
 * Los Replication Event Metadata
 */
public class LogReplicationEventMetadata {

    private static final UUID NIL_UUID = new UUID(0,0);

    /*
     * Represents the request Id that preceded this event.
     *
     * This is used to correlate the new event with the state in which it was originated.
     * For example, a trimmed exception from state A vs. a trimmed exception from state B.
     */
    private UUID requestId;

    /* Represents the last synced timestamp.
     *
     * For snapshot sync, it represents the base snapshot.
     * For log entry sync, it represents the last log entry synced.
    */
    private long syncTimestamp;

    /**
     * Empty Metadata
     *
     * @return an empty instance of log replication event metadata
     */
    public static LogReplicationEventMetadata empty() {
        return new LogReplicationEventMetadata(NIL_UUID, 0L);
    }

    /**
     * Constructor
     *
     * @param requestId identifier of the request that preceded this event.
     */
    public LogReplicationEventMetadata(UUID requestId) {
        this.requestId = requestId;
    }

    /**
     * Constructor
     *
     * @param syncTimestamp last synced timestamp.
     */
    public LogReplicationEventMetadata(long syncTimestamp) {
        this.syncTimestamp = syncTimestamp;
    }

    /**
     * Constructor
     *
     * @param requestId identifier of the request that preceded this event.
     * @param syncTimestamp last synced timestamp.
     */
    public LogReplicationEventMetadata(UUID requestId, long syncTimestamp) {
        this.requestId = requestId;
        this.syncTimestamp = syncTimestamp;
    }

    public UUID getRequestId() {
        return this.requestId;
    }

    public long getSyncTimestamp() {
        return this.syncTimestamp;
    }

}

