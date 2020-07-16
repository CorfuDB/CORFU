package org.corfudb.common.protocol;

import org.corfudb.common.protocol.proto.CorfuProtocol;
import org.corfudb.common.protocol.proto.CorfuProtocol.Header;
import org.corfudb.common.protocol.proto.CorfuProtocol.MessageType;
import org.corfudb.common.protocol.proto.CorfuProtocol.Request;
import org.corfudb.common.protocol.proto.CorfuProtocol.Response;
import org.corfudb.common.protocol.proto.CorfuProtocol.PingRequest;
import org.corfudb.common.protocol.proto.CorfuProtocol.AuthenticateRequest;
import org.corfudb.common.protocol.proto.CorfuProtocol.AuthenticateResponse;
import org.corfudb.common.protocol.proto.CorfuProtocol.Priority;
import org.corfudb.common.protocol.proto.CorfuProtocol.ProtocolVersion;
import org.corfudb.common.protocol.proto.CorfuProtocol.StreamAddressRange;
import org.corfudb.common.protocol.proto.CorfuProtocol.QueryStreamRequest;

import java.util.List;
import java.util.UUID;

/**
 * Created by Maithem on 7/1/20.
 */

public class API {

    public static final ProtocolVersion CURRENT_VERSION = ProtocolVersion.v0;
    public static final UUID DEFAULT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static CorfuProtocol.UUID getUUID(UUID uuid) {
        return CorfuProtocol.UUID.newBuilder()
                .setLsb(uuid.getLeastSignificantBits())
                .setMsb(uuid.getMostSignificantBits())
                .build();
    }

    public static Header newHeader(long requestId, Priority priority, MessageType type,
                                   long epoch, UUID clusterId, UUID clientId,
                                   boolean ignoreClusterId, boolean ignoreEpoch) {
        return Header.newBuilder()
                .setVersion(CURRENT_VERSION)
                .setRequestId(requestId)
                .setPriority(priority)
                .setType(type)
                .setEpoch(epoch)
                .setClusterId(getUUID(clusterId))
                .setClientId(getUUID(clientId))
                .setIgnoreClusterId(ignoreClusterId)
                .setIgnoreEpoch(ignoreEpoch)
                .build();
    }

    public static Request newPingRequest(Header header) {
        PingRequest pingRequest = PingRequest.getDefaultInstance();
        return Request.newBuilder()
                .setHeader(header)
                .setPingRequest(pingRequest)
                .build();
    }

    public static Request newAuthenticateRequest(Header header, UUID clientId, UUID serverId) {
        AuthenticateRequest authRequest = AuthenticateRequest.newBuilder()
                                            .setClientId(getUUID(clientId))
                                            .setServerId(getUUID(serverId))
                                            .build();
        return Request.newBuilder()
                .setHeader(header)
                .setAuthenticateRequest(authRequest)
                .build();
    }

    public static Response newAuthenticateResponse(Header header, UUID serverId, String version) {
        AuthenticateResponse authResponse = AuthenticateResponse.newBuilder()
                                                .setServerId(getUUID(serverId))
                                                .setCorfuVersion(version)
                                                .build();
        return Response.newBuilder()
                .setHeader(header)
                .setAuthenticateResponse(authResponse)
                .build();
    }

    public static Request newQueryStreamRequest(Header header, QueryStreamRequest.ReqType type,
                                                List<StreamAddressRange> ranges) {
        QueryStreamRequest qsRequest = QueryStreamRequest.newBuilder()
                                                .setType(type)
                                                .addAllStreamRanges(ranges)
                                                .build();
        return Request.newBuilder()
                .setHeader(header)
                .setQueryStreamRequest(qsRequest)
                .build();
    }

    public static Request newQueryStreamRequest(Header header, List<StreamAddressRange> ranges) {
        return newQueryStreamRequest(header, QueryStreamRequest.ReqType.STREAMS, ranges);
    }
}
