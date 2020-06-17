package org.corfudb.transport.server;

import lombok.Getter;
import org.corfudb.infrastructure.ServerContext;
import org.corfudb.runtime.Messages.CorfuMessage;
import org.corfudb.transport.logreplication.LogReplicationServerRouter;

import java.util.concurrent.CompletableFuture;

/**
 * Server Transport Adapter.
 *
 * If Log Replication relies on a custom transport protocol for communication across servers,
 * this interface must be extended by the server-side adapter to implement a custom channel.
 *
 * @author annym 05/15/2020
 */
public abstract class IServerChannelAdapter {

    @Getter
    private final LogReplicationServerRouter router;

    @Getter
    private final ServerContext serverContext;

    @Getter
    private final int port;

    public IServerChannelAdapter(ServerContext serverContext, LogReplicationServerRouter adapter) {
        this.serverContext = serverContext;
        this.router = adapter;
        this.port = Integer.parseInt((String) serverContext.getServerConfig().get("<port>"));
    }

    /**
     * Send message across channel.
     *
     * @param msg corfu message (protoBuf definition)
     */
    public abstract void send(CorfuMessage msg);

    /**
     * Receive a message from Client.
     *
     * @param msg received corfu message
     */
    public void receive(CorfuMessage msg) {
        getRouter().receive(msg);
    }

    /**
     * Initialize adapter.
     *
     * @return Completable Future on connection start
     */
    public abstract CompletableFuture<Boolean> start();

    /**
     * Close connections or gracefully shutdown the channel.
     */
    public void stop() {}
}
