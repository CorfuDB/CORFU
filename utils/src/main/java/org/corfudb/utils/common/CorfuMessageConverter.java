package org.corfudb.utils.common;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.corfudb.protocols.wireprotocol.CorfuMsg;
import org.corfudb.protocols.wireprotocol.CorfuMsgType;
import org.corfudb.protocols.wireprotocol.CorfuPayloadMsg;
import org.corfudb.protocols.wireprotocol.PriorityLevel;
import org.corfudb.protocols.wireprotocol.logreplication.LogReplicationEntry;
import org.corfudb.protocols.wireprotocol.logreplication.LogReplicationNegotiationResponse;
import org.corfudb.protocols.wireprotocol.logreplication.LogReplicationQueryLeaderShipResponse;
import org.corfudb.runtime.Messages;
import org.corfudb.runtime.Messages.CorfuMessage;
import org.corfudb.runtime.Messages.LogReplicationEntryMetadata;
import org.corfudb.runtime.Messages.CorfuMessageType;

import java.util.UUID;


public class CorfuMessageConverter {

    /**
     * Convert between legacy Java CorfuMsg to protoBuf definition.
     *
     * @param msg legacy corfu message
     * @return protoBuf message definition or null if message is not supported.
     */
    public static CorfuMessage toProtoBuf(CorfuMsg msg) {

        CorfuMessage.Builder protoCorfuMsg = CorfuMessage.newBuilder()
                .setClientID(Messages.Uuid.newBuilder().setLsb(msg.getClientID().getLeastSignificantBits())
                        .setMsb(msg.getClientID().getMostSignificantBits()).build())
                .setEpoch(msg.getEpoch())
                .setPriority(Messages.CorfuPriorityLevel.valueOf(msg.getPriorityLevel().name()))
                .setRequestID(msg.getRequestID());

        switch (msg.getMsgType()) {
            case LOG_REPLICATION_ENTRY:
                CorfuPayloadMsg<LogReplicationEntry> entry = (CorfuPayloadMsg<LogReplicationEntry>) msg;
                LogReplicationEntry logReplicationEntry = entry.getPayload();
                return protoCorfuMsg
                        .setType(CorfuMessageType.LOG_REPLICATION_ENTRY)
                        // Set Log Replication Entry as payload
                        .setPayload(Any.pack(Messages.LogReplicationEntry.newBuilder()
                                .setMetadata(LogReplicationEntryMetadata.newBuilder()
                                        .setType(Messages.LogReplicationEntryType.valueOf(logReplicationEntry.getMetadata().getMessageMetadataType().name()))
                                        .setPreviousTimestamp(logReplicationEntry.getMetadata().getPreviousTimestamp())
                                        .setSnapshotSyncSeqNum(logReplicationEntry.getMetadata().getSnapshotSyncSeqNum())
                                        .setSnapshotTimestamp(logReplicationEntry.getMetadata().getSnapshotTimestamp())
                                        .setSyncRequestId(Messages.Uuid.newBuilder().setMsb(logReplicationEntry.getMetadata().getSyncRequestId().getMostSignificantBits())
                                                .setLsb(logReplicationEntry.getMetadata().getSyncRequestId().getLeastSignificantBits()).build())
                                        .setTimestamp(logReplicationEntry.getMetadata().getTimestamp()))
                                .setData(ByteString.copyFrom(logReplicationEntry.getPayload()))
                                .build()))
                        .build();
            case LOG_REPLICATION_NEGOTIATION_RESPONSE:
                CorfuPayloadMsg<LogReplicationNegotiationResponse> corfuMsg = (CorfuPayloadMsg<LogReplicationNegotiationResponse>) msg;
                LogReplicationNegotiationResponse negotiationResponse = corfuMsg.getPayload();
                return protoCorfuMsg
                        .setType(Messages.CorfuMessageType.LOG_REPLICATION_NEGOTIATION_RESPONSE)
                        .setPayload(Any.pack(Messages.LogReplicationNegotiationResponse.newBuilder()
                                .setBaseSnapshotTimestamp(negotiationResponse.getBaseSnapshotTimestamp())
                                .setLogEntryTimestamp(negotiationResponse.getLogEntryTimestamp())
                                .build()))
                        .build();
            case LOG_REPLICATION_QUERY_LEADERSHIP_RESPONSE:
                CorfuPayloadMsg<LogReplicationQueryLeaderShipResponse> corfuPayloadMsg = (CorfuPayloadMsg<LogReplicationQueryLeaderShipResponse>) msg;
                LogReplicationQueryLeaderShipResponse leaderShipResponse = corfuPayloadMsg.getPayload();
                return protoCorfuMsg
                        .setType(Messages.CorfuMessageType.LOG_REPLICATION_QUERY_LEADERSHIP_RESPONSE)
                        .setPayload(Any.pack(Messages.LogReplicationQueryLeadershipResponse.newBuilder()
                                .setEpoch(leaderShipResponse.getEpoch())
                                .setIsLeader(leaderShipResponse.isLeader())
                                .build()))
                        .build();
            case LOG_REPLICATION_NEGOTIATION_REQUEST:
                return protoCorfuMsg
                        .setType(Messages.CorfuMessageType.LOG_REPLICATION_NEGOTIATION_REQUEST)
                        .build();
            case LOG_REPLICATION_QUERY_LEADERSHIP:
                return protoCorfuMsg
                        .setType(Messages.CorfuMessageType.LOG_REPLICATION_QUERY_LEADERSHIP)
                        .build();
            default:
                throw new IllegalArgumentException(String.format("{} type is not supported", msg.getMsgType()));
        }
    }

    /**
     * Convert from protoBuf definition to legacy Java Corfu Message
     *
     * @param protoMessage protoBuf message
     * @return legacy java corfu message
     */
    public static CorfuMsg fromProtoBuf(CorfuMessage protoMessage) throws CorfuMessageProtoBufException {

        // Base Corfu Message
        UUID clientId = new UUID(protoMessage.getClientID().getMsb(), protoMessage.getClientID().getLsb());
        long requestId = protoMessage.getRequestID();
        PriorityLevel priorityLevel = PriorityLevel.fromProtoType(protoMessage.getPriority());
        ByteBuf buf = Unpooled.wrappedBuffer(protoMessage.getPayload().toByteArray());
        long epoch = protoMessage.getEpoch();

        try {
            switch (protoMessage.getType()) {
                case LOG_REPLICATION_ENTRY:
                    LogReplicationEntry logReplicationEntry = LogReplicationEntry
                            .fromProto(protoMessage.getPayload().unpack(Messages.LogReplicationEntry.class));

                    return new CorfuPayloadMsg<>(CorfuMsgType.LOG_REPLICATION_ENTRY, logReplicationEntry)
                            .setClientID(clientId)
                            .setRequestID(requestId)
                            .setPriorityLevel(priorityLevel)
                            .setBuf(buf)
                            .setEpoch(epoch);
                case LOG_REPLICATION_NEGOTIATION_REQUEST:
                    return new CorfuMsg(clientId, null, requestId, epoch, null,
                            CorfuMsgType.LOG_REPLICATION_NEGOTIATION_REQUEST, priorityLevel);
                case LOG_REPLICATION_QUERY_LEADERSHIP:
                    return new CorfuMsg(clientId, null, requestId, epoch, null,
                            CorfuMsgType.LOG_REPLICATION_QUERY_LEADERSHIP, priorityLevel);
                case LOG_REPLICATION_NEGOTIATION_RESPONSE:
                        LogReplicationNegotiationResponse negotiationResponse = LogReplicationNegotiationResponse
                                .fromProto(protoMessage.getPayload().unpack(Messages.LogReplicationNegotiationResponse.class));

                        return new CorfuPayloadMsg<>(CorfuMsgType.LOG_REPLICATION_NEGOTIATION_RESPONSE, negotiationResponse)
                                .setClientID(clientId)
                                .setRequestID(requestId)
                                .setPriorityLevel(priorityLevel)
                                .setBuf(buf)
                                .setEpoch(epoch);
                case LOG_REPLICATION_QUERY_LEADERSHIP_RESPONSE:
                        LogReplicationQueryLeaderShipResponse leadershipResponse = LogReplicationQueryLeaderShipResponse
                                .fromProto(protoMessage.getPayload().unpack(Messages.LogReplicationQueryLeadershipResponse.class));

                        return new CorfuPayloadMsg<>(CorfuMsgType.LOG_REPLICATION_QUERY_LEADERSHIP_RESPONSE, leadershipResponse)
                                .setClientID(clientId)
                                .setRequestID(requestId)
                                .setPriorityLevel(priorityLevel)
                                .setBuf(buf)
                                .setEpoch(epoch);
                default:
                    throw new IllegalArgumentException(String.format("{} type is not supported", protoMessage.getType().name()));
            }
        } catch (Exception e) {
            throw new CorfuMessageProtoBufException(e);
        }
    }

}
