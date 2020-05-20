package org.corfudb.protocols.wireprotocol.logreplication;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import org.corfudb.protocols.wireprotocol.ICorfuPayload;

import java.util.UUID;

@Data
public class LogReplicationEntry implements ICorfuPayload<LogReplicationEntry> {

    private LogReplicationEntryMetadata metadata;

    private byte[] payload;

    public LogReplicationEntry(LogReplicationEntryMetadata metadata, byte[] payload) {
        this.payload = payload;
        this.metadata = metadata;
    }

    public LogReplicationEntry(MessageType type, long epoch, UUID syncRequestId, long entryTS, long preTS, long snapshot, long sequence,
                               byte[] payload) {
        this.metadata = new LogReplicationEntryMetadata(type, epoch, syncRequestId, entryTS, preTS, snapshot, sequence);
        this.payload = payload;
    }

    public LogReplicationEntry(ByteBuf buf) {
        metadata = ICorfuPayload.fromBuffer(buf, LogReplicationEntryMetadata.class);
        payload = ICorfuPayload.fromBuffer(buf, byte[].class);
    }

    public static LogReplicationEntry generateAck(LogReplicationEntryMetadata metadata) {
        return new LogReplicationEntry(metadata, new byte[0]);
    }

    @Override
    public void doSerialize(ByteBuf buf) {
        ICorfuPayload.serialize(buf, metadata);
        ICorfuPayload.serialize(buf, payload);
    }
}
