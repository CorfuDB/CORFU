package org.corfudb.infrastructure;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.protocols.wireprotocol.CorfuMsg;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Created by mwei on 12/4/15.
 */
@Slf4j
public abstract class AbstractServer {
    public static final long NON_LOG_ADDR_MAGIC = -1L;
    public static final Duration SMALL_INTERVAL = Duration.ofMillis(60_000);


    @Getter
    @Setter
    boolean shutdown;

    public AbstractServer() {
        shutdown = false;
    }

    /** Get the message handler for this instance.
     * @return  A message handler.
     */
    public abstract CorfuMsgHandler getHandler();

    /**
     * Handle a incoming Netty message.
     *
     * @param msg An incoming message.
     * @param ctx The channel handler context.
     * @param r   The router that took in the message.
     */
    public void handleMessage(CorfuMsg msg, ChannelHandlerContext ctx, IServerRouter r) {
        if (isShutdown()) return;
        if (!getHandler().handle(msg, ctx, r)) {
            log.warn("Received unhandled message type {}" , msg.getMsgType());
        }
    }

    /**
     * Shutdown the server.
     */
    public void shutdown() {
        setShutdown(true);
    }

}
