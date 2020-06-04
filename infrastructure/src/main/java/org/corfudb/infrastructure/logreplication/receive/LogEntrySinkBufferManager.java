package org.corfudb.infrastructure.logreplication.receive;

import lombok.extern.slf4j.Slf4j;
import org.corfudb.protocols.wireprotocol.logreplication.LogReplicationEntry;
import org.corfudb.protocols.wireprotocol.logreplication.LogReplicationEntryMetadata;
import org.corfudb.protocols.wireprotocol.logreplication.MessageType;

import static org.corfudb.protocols.wireprotocol.logreplication.MessageType.LOG_ENTRY_MESSAGE;
import static org.corfudb.protocols.wireprotocol.logreplication.MessageType.LOG_ENTRY_REPLICATED;

/**
 * It manages the log entry sink buffer.
 */
@Slf4j
public class LogEntrySinkBufferManager extends SinkBufferManager {

    /**
     *
     * @param ackCycleTime
     * @param ackCycleCnt
     * @param size
     * @param lastProcessedSeq last processed log entry's timestamp
     * @param sinkManager
     */
    public LogEntrySinkBufferManager(int ackCycleTime, int ackCycleCnt, int size, long lastProcessedSeq, LogReplicationSinkManager sinkManager) {
        super(LOG_ENTRY_MESSAGE, ackCycleTime, ackCycleCnt, size, lastProcessedSeq, sinkManager);
    }

    /**
     *
     * @param entry
     * @return log entry message's previousTimestamp.
     */
    @Override
    long getPreSeq(LogReplicationEntry entry) {
        return entry.getMetadata().getPreviousTimestamp();
    }

    /**
     *
     * @param entry
     * @return log entry message's timestamp.
     */
    @Override
    long getCurrentSeq(LogReplicationEntry entry) {
        return entry.getMetadata().getTimestamp();
    }

    /**
     *
     * @param entry
     * @return ackMessage's metadata.
     */
    @Override
    public LogReplicationEntryMetadata makeAckMessage(LogReplicationEntry entry) {
        LogReplicationEntryMetadata metadata = new LogReplicationEntryMetadata(entry.getMetadata());
        metadata.setMessageMetadataType(LOG_ENTRY_REPLICATED);
        metadata.setTimestamp(lastProcessedSeq);
        return metadata;
    }

    /**
     * Verify if it is the correct message type for log entry replication
     * @param entry
     * @return
     */
    @Override
    public boolean verifyMessageType(LogReplicationEntry entry) {
        if (entry.getMetadata().getMessageMetadataType() != type) {
            log.warn("Got msg type {} but expecting type {}",
                    entry.getMetadata().getMessageMetadataType(), type);
            return false;
        }

        return true;
    }
}
