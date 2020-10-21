package org.corfudb.infrastructure;

import com.codahale.metrics.Timer;
import io.netty.channel.ChannelHandlerContext;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.runtime.proto.service.CorfuMessage.RequestMsg;
import org.corfudb.runtime.proto.service.CorfuMessage.RequestPayloadMsg;
import org.corfudb.runtime.exceptions.unrecoverable.UnrecoverableCorfuError;
import org.corfudb.util.CorfuComponent;
import org.corfudb.util.MetricsUtils;


@Slf4j
public class RequestHandlerMethods {

    private final Map<RequestPayloadMsg.PayloadCase, String> timerNameCache = new HashMap<>();

    /** The handler map. */
    private final Map<RequestPayloadMsg.PayloadCase, HandlerMethod> handlerMap;

    /**
     * A functional interface for server request handlers. Server request handlers should
     * be fast and not block. If a handler blocks for an extended period of time, it will
     * exhaust the server's thread pool. I/O and other long operations should be handled
     * on another thread.
     */
    @FunctionalInterface
    public interface HandlerMethod {
        void handle(@Nonnull RequestMsg req,
                    @Nonnull ChannelHandlerContext ctx,
                    @Nonnull IServerRouter r);
    }

    /** Get the types of requests this handler will handle.
     *
     * @return  A set containing the types of requests this handler will handle.
     */
    public Set<RequestPayloadMsg.PayloadCase> getHandledTypes() {
        return handlerMap.keySet();
    }

    /** Construct a new instance of RequestHandlerMethods. */
    public RequestHandlerMethods() {
        handlerMap = new EnumMap<>(RequestPayloadMsg.PayloadCase.class);
    }

    /** Handle an incoming Corfu request message.
     *
     * @param req       The request message to handle.
     * @param ctx       The channel handler context.
     * @param r         The server router.
     */
    @SuppressWarnings("unchecked")
    public void handle(RequestMsg req, ChannelHandlerContext ctx, IServerRouter r) {
        final HandlerMethod handler = handlerMap.get(req.getPayload().getPayloadCase());
        try {
            handler.handle(req, ctx, r);
        } catch(Exception e) {
            log.error("handle[{}]: Unhandled exception processing {} request",
                    req.getHeader().getRequestId(), req.getPayload().getPayloadCase(), e);
            //TODO(Zach): Send exception/error response
        }
    }

    /** Generate handlers for a particular server.
     *
     * @param caller    The context that is being used. Call MethodHandles.lookup() to obtain.
     * @param server    The object that implements the server.
     * @return          New request handlers for caller class.
     */
    public static RequestHandlerMethods generateHandler(@Nonnull final MethodHandles.Lookup caller,
                                                        @NonNull final AbstractServer server) {
        RequestHandlerMethods handler = new RequestHandlerMethods();
        Arrays.stream(server.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(RequestHandler.class))
                .forEach(method -> handler.registerMethod(caller, server, method));
        return handler;
    }

    private void registerMethod(@Nonnull final MethodHandles.Lookup caller,
                                @Nonnull final AbstractServer server,
                                @Nonnull final Method method) {
        final RequestHandler annotation = method.getAnnotation(RequestHandler.class);

        if(handlerMap.containsKey(annotation.type())) {
            throw new UnrecoverableCorfuError("HandlerMethod for " + annotation.type() + " already registered!");
        }

        try {
            HandlerMethod h;
            if (Modifier.isStatic(method.getModifiers())) {
                MethodHandle mh = caller.unreflect(method);
                h = (HandlerMethod) LambdaMetafactory.metafactory(caller,
                        "handle", MethodType.methodType(HandlerMethod.class),
                        mh.type(), mh, mh.type()).getTarget().invokeExact();
            } else {
                // Instance method, so we need to capture the type.
                MethodType mt = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
                MethodHandle mh = caller.findVirtual(server.getClass(), method.getName(), mt);
                MethodType mtt = mh.type().dropParameterTypes(0, 1);
                h = (HandlerMethod) LambdaMetafactory.metafactory(caller, "handle",
                        MethodType.methodType(HandlerMethod.class, server.getClass()),
                        mtt, mh, mtt).getTarget().bindTo(server).invoke();
            }

            // Install pre-conditions on handler and place the handler in the map
            final HandlerMethod handler = generateConditionalHandler(annotation.type(), h);
            handlerMap.put(annotation.type(), handler);
        } catch(Throwable e) {
            log.error("registerMethod: Exception during request handler registration", e);
            throw new UnrecoverableCorfuError(e);
        }
    }

    private HandlerMethod generateConditionalHandler(@NonNull final RequestPayloadMsg.PayloadCase type,
                                                     @NonNull final HandlerMethod handler) {
        // Generate a timer based on the Corfu request type
        final Timer timer = getTimer(type);

        // Register the handler. Depending on metrics collection configuration by MetricsUtil,
        // handler will be instrumented by the metrics context.
        return (req, ctx, r) -> {
            try (Timer.Context context = MetricsUtils.getConditionalContext(timer)) {
                handler.handle(req, ctx, r);
            }
        };
    }

    private Timer getTimer(@Nonnull RequestPayloadMsg.PayloadCase type) {
        timerNameCache.computeIfAbsent(type,
                aType -> (CorfuComponent.INFRA_MSG_HANDLER + aType.name().toLowerCase()));
        return ServerContext.getMetrics().timer(timerNameCache.get(type));
    }
}