package org.corfudb.infrastructure.logreplication.replication.send;

import lombok.extern.slf4j.Slf4j;
import org.corfudb.infrastructure.logreplication.DataSender;
import org.corfudb.infrastructure.logreplication.proto.LogReplicationMetadata.ReplicationStatusVal;
import org.corfudb.infrastructure.logreplication.replication.LogReplicationAckReader;
import org.corfudb.protocols.wireprotocol.logreplication.LogReplicationEntry;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Manage the SnapshotSender's message buffer.
 */
@Slf4j
public class SnapshotSenderBufferManager extends SenderBufferManager {
    private LogReplicationAckReader ackReader;

    public SnapshotSenderBufferManager(DataSender dataSender, LogReplicationAckReader ackReader) {
        super(dataSender);
        this.ackReader = ackReader;
    }

    /**
     * While receiving an ACK, will update the maxAck and also remove messages and CompletableFutures whose
     * corresponding snapshotSeqNum is not larger than the ACK.
     * @param newAck
     */
    @Override
    public void updateAck(Long newAck) {
        if (maxAckTimestamp < newAck) {
            log.debug("Ack Received for Snapshot Sync {}", newAck);
            maxAckTimestamp = newAck;
            pendingMessages.evictAccordingToSeqNum(maxAckTimestamp);
            pendingCompletableFutureForAcks = pendingCompletableFutureForAcks.entrySet().stream()
                    .filter(entry -> entry.getKey() > maxAckTimestamp)
                    .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
            ackReader.setAckedTsAndSyncType(newAck, ReplicationStatusVal.SyncType.SNAPSHOT);
        }
    }

    /**
     * Update the maxAck with snapShotSyncSeqNum.
     * @param entry
     */
    @Override
    public void updateAck(LogReplicationEntry entry) {
        updateAck(entry.getMetadata().getSnapshotSyncSeqNum());
    }

    /**
     * Use the message's snapshotSeqNum as the key to add its messages's corresponding ACK's
     * CompletableFuture to the hash table.
     * @param message
     * @param cf
     */
    @Override
    public void addCFToAcked(LogReplicationEntry message, CompletableFuture<LogReplicationEntry> cf) {
        pendingCompletableFutureForAcks.put(message.getMetadata().getSnapshotSyncSeqNum(), cf);
    }
}
