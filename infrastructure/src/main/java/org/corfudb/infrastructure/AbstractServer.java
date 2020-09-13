package org.corfudb.infrastructure;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.protocols.API;
import org.corfudb.runtime.protocol.proto.CorfuProtocol.Header;
import org.corfudb.runtime.protocol.proto.CorfuProtocol.Request;
import org.corfudb.runtime.protocol.proto.CorfuProtocol.Response;
import org.corfudb.protocols.wireprotocol.CorfuMsg;
import org.corfudb.protocols.wireprotocol.CorfuMsgType;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by mwei on 12/4/15.
 */
@Slf4j
public abstract class AbstractServer {

    /**
     * Current server state
     */
    private final AtomicReference<ServerState> state = new AtomicReference<>(ServerState.READY);

    /**
     * Get the message handler for this instance.
     *
     * @return A message handler.
     */
    public abstract HandlerMethods getHandler();

    /**
     * Get the request handlers for this instance.
     *
     * @return The request handler methods.
     */
    public RequestHandlerMethods getHandlerMethods() {
        //TODO: Make abstract once other servers are implemented.
        return null;
    }

    /**
     * Seal the server with the epoch.
     *
     * @param epoch Epoch to seal with
     */
    public void sealServerWithEpoch(long epoch) {
        // Overridden in log unit to flush operations stamped with an old epoch
    }

    public abstract boolean isServerReadyToHandleMsg(CorfuMsg msg);

    /**
     * Determine if the server is ready to handle a request.
     * @param requestHeader The incoming request message header.
     * @return True if the server is ready to handle this request, and false otherwise.
     */
    public boolean isServerReadyToHandleReq(Header requestHeader) {
        //TODO: Make abstract once other servers are implemented
        return false;
    }

    /**
     * A stub that handlers can override to manage their threading, otherwise
     * the requests will be executed on the IO threads
     * @param msg
     * @param ctx
     * @param r
     */
    protected void processRequest(CorfuMsg msg, ChannelHandlerContext ctx, IServerRouter r) {
        getHandler().handle(msg, ctx, r);
    }

    /**
     * A stub that handlers can override to manage their threading, otherwise
     * the requests will be executed on the IO threads
     * @param req An incoming request message.
     * @param ctx The channel handler context.
     * @param r The router that took in the request.
     */
    protected void processRequest(Request req, ChannelHandlerContext ctx, IRequestRouter r) {
        getHandlerMethods().handle(req, ctx, r);
    }


    /**
     * Handle a incoming Netty message.
     *
     * @param msg An incoming message.
     * @param ctx The channel handler context.
     * @param r   The router that took in the message.
     */
    public final void handleMessage(CorfuMsg msg, ChannelHandlerContext ctx, IServerRouter r) {
        if (getState() == ServerState.SHUTDOWN) {
            log.warn("Server received {} but is already shutdown.", msg.getMsgType().toString());
            return;
        }

        if (!isServerReadyToHandleMsg(msg)) {
            r.sendResponse(ctx, msg, CorfuMsgType.NOT_READY.msg());
            return;
        }

        processRequest(msg, ctx, r);
    }

    /**
     * Handle a incoming request message.
     *
     * @param req An incoming request message.
     * @param ctx The channel handler context.
     * @param r   The router that took in the request message.
     */
    public final void handleRequest(Request req, ChannelHandlerContext ctx, IRequestRouter r) {
        if (getState() == ServerState.SHUTDOWN) {
            log.warn("handleRequest[{}]: Server received {} but is already shutdown.",
                    req.getHeader().getRequestId(), req.getHeader().getType().toString());
            return;
        }

        if(!isServerReadyToHandleReq(req.getHeader())) {
            r.sendResponse(getNotReadyError(req.getHeader()), ctx);
            return;
        }

        processRequest(req, ctx, r);
    }

    private Response getNotReadyError(Header requestHeader) {
        return API.getErrorResponseNoPayload(API.generateResponseHeader(requestHeader, false, true),
                API.getNotReadyServerError());
    }

    /**
     * Handle an incoming message (not Netty specific).
     *
     * @param msg An incoming message.
     * @param r   The router that took in the message.
     */
    public final void handleMessage(CorfuMsg msg, IServerRouter r) {
        if (getState() == ServerState.SHUTDOWN) {
            log.warn("Server received {} but is already shutdown.", msg.getMsgType().toString());
            return;
        }

        if (!isServerReadyToHandleMsg(msg)) {
            r.sendResponse(msg, CorfuMsgType.NOT_READY.msg());
            return;
        }

        processRequest(msg, null, r);
    }

    protected void setState(ServerState newState) {
        state.updateAndGet(currState -> {
            if (currState == ServerState.SHUTDOWN && newState != ServerState.SHUTDOWN) {
                throw new IllegalStateException("Server is in SHUTDOWN state. Can't be changed");
            }

            return newState;
        });
    }

    public ServerState getState() {
        return state.get();
    }

    /**
     * Shutdown the server.
     */
    public void shutdown() {
        setState(ServerState.SHUTDOWN);
    }

    /**
     * The server state.
     * Represents server in a particular state: READY, NOT_READY, SHUTDOWN.
     */
    public enum ServerState {
        READY, SHUTDOWN
    }
}
