package org.corfudb.infrastructure.logreplication;

import org.corfudb.protocols.wireprotocol.logreplication.LogReplicationEntry;

import java.util.List;

/**
 * This Interface comprises Data Path send operations for both Source and Sink.
 *
 * Application is expected to transmit messages from source to sink, and ACKs
 * from sink to source.
 *
 *
 */
public interface DataSender {

    /**
     * Application callback on next available message for transmission to remote site.
     *
     * @param message LogReplicationEntry representing the data to send across sites.
     * @return
     */
    boolean send(LogReplicationEntry message);

    /**
     * Application callback on next available messages for transmission to remote site.
     *
     * @param messages list of LogReplicationEntry representing the data to send across sites.
     * @return
     */
    boolean send(List<LogReplicationEntry> messages);

    /**
     * Application callback on error.
     *
     * @param error log replication error
     */
    void onError(LogReplicationError error);
}
