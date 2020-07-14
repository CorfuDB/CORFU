package org.corfudb.infrastructure;

import com.google.common.collect.ImmutableList;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.protocols.wireprotocol.CorfuMsg;
import org.corfudb.protocols.wireprotocol.CorfuMsgType;
import org.corfudb.runtime.view.Layout;

/**
 * The netty server router routes incoming messages to registered roles using the Created by mwei on
 * 12/1/15.
 */
@Slf4j
@ChannelHandler.Sharable
public class NettyServerRouter extends ChannelInboundHandlerAdapter implements IServerRouter {

  /** This map stores the mapping from message type to netty server handler. */
  private final Map<CorfuMsgType, AbstractServer> handlerMap;

  /** This node's server context. */
  private final ServerContext serverContext;
  /** The epoch of this router. This is managed by the base server implementation. */
  @Getter @Setter volatile long serverEpoch;

  /** The {@link AbstractServer}s this {@link NettyServerRouter} routes messages for. */
  private final ImmutableList<AbstractServer> servers;

  /**
   * Construct a new {@link NettyServerRouter}.
   *
   * @param servers A list of {@link AbstractServer}s this router will route messages for.
   */
  public NettyServerRouter(ImmutableList<AbstractServer> servers, ServerContext serverContext) {
    this.serverContext = serverContext;
    this.serverEpoch = serverContext.getServerEpoch();
    this.servers = servers;
    handlerMap = new EnumMap<>(CorfuMsgType.class);

    servers.forEach(
        server -> {
          Set<CorfuMsgType> handledTypes = server.getHandler().getHandledTypes();
          handledTypes.forEach(handledType -> handlerMap.put(handledType, server));
        });
  }

  /**
   * {@inheritDoc}
   *
   * @deprecated This operation is no longer supported. The router will only route messages for
   *     servers provided at construction time.
   */
  @Override
  @Deprecated
  public void addServer(AbstractServer server) {
    throw new UnsupportedOperationException("No longer supported");
  }

  /** {@inheritDoc} */
  @Override
  public List<AbstractServer> getServers() {
    return servers;
  }

  @Override
  public void setServerContext(ServerContext serverContext) {
    throw new UnsupportedOperationException("The operation is not supported.");
  }

  /**
   * Send a netty message through this router, setting the fields in the outgoing message.
   *
   * @param ctx Channel handler context to use.
   * @param inMsg Incoming message to respond to.
   * @param outMsg Outgoing message.
   */
  public void sendResponse(ChannelHandlerContext ctx, CorfuMsg inMsg, CorfuMsg outMsg) {
    outMsg.copyBaseFields(inMsg);
    ctx.writeAndFlush(outMsg, ctx.voidPromise());
    log.trace("Sent response: {}", outMsg);
  }

  @Override
  public Optional<Layout> getCurrentLayout() {
    return Optional.ofNullable(serverContext.getCurrentLayout());
  }

  /**
   * Handle an incoming message read on the channel.
   *
   * @param ctx Channel handler context
   * @param msg The incoming message on that channel.
   */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    try {
      // The incoming message should have been transformed to a CorfuMsg earlier in the
      // pipeline.
      CorfuMsg m = ((CorfuMsg) msg);
      // We get the handler for this message from the map
      AbstractServer handler = handlerMap.get(m.getMsgType());
      if (handler == null) {
        // The message was unregistered, we are dropping it.
        log.warn("Received unregistered message {}, dropping", m);
      } else {
        if (messageIsValid(m, ctx)) {
          // Route the message to the handler.
          if (log.isTraceEnabled()) {
            log.trace("Message routed to {}: {}", handler.getClass().getSimpleName(), msg);
          }

          try {
            handler.handleMessage(m, ctx, this);
          } catch (Throwable t) {
            log.error(
                "channelRead: Handling {} failed due to {}:{}",
                m != null ? m.getMsgType() : "UNKNOWN",
                t.getClass().getSimpleName(),
                t.getMessage(),
                t);
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception during read!", e);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error("Error in handling inbound message, {}", cause);
    ctx.close();
  }
}
