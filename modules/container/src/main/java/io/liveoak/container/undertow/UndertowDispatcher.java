package io.liveoak.container.undertow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import io.liveoak.common.Constants;
import io.liveoak.container.Dispatcher;
import io.liveoak.container.traversal.BaseResponder;
import io.liveoak.container.traversal.Pipeline;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.resource.BlockingResource;
import io.liveoak.spi.resource.async.BinaryResource;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.spi.resource.async.Responder;
import io.liveoak.spi.state.LazyResourceState;
import io.undertow.UndertowMessages;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpServerConnection;
import io.undertow.util.SameThreadExecutor;
import org.xnio.ChannelListener;
import org.xnio.Pooled;
import org.xnio.channels.StreamSourceChannel;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class UndertowDispatcher implements Dispatcher {

    private final Executor executor;
    private HttpServerExchange exchange;

    public UndertowDispatcher(HttpServerExchange exchange, Executor workerPool) {
        this.exchange = exchange;
        this.executor = workerPool;
    }

    @Override
    public void dispatch(Runnable runnable) {
        exchange.dispatch(executor, runnable);
    }

    @Override
    public void update(RequestContext context, Resource resource, Responder responder) throws Exception {

        actionWithResourceBody(context, resource, responder, () -> {
            try {
                resource.updateProperties(context, context.requestState(), responder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void create(RequestContext context, Resource resource, Responder responder) throws Exception {

        actionWithResourceBody(context, resource, responder, () -> {
            try {
                resource.createMember(context, context.requestState(), responder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected void actionWithResourceBody(RequestContext context, Resource resource, Responder responder, Runnable runnable) throws Exception {

        boolean isBinary = resource instanceof BinaryResource;

        Pooled<ByteBuffer> extraBytes = ((HttpServerConnection) exchange.getConnection()).getExtraBytes();

        long bodyLen = exchange.getRequestContentLength();

        boolean bodyFullyRead = extraBytes != null && bodyLen != -1 ? bodyLen <= extraBytes.getResource().remaining() : false;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChannelListener<StreamSourceChannel> bodyReadHandler = new ChannelListener<StreamSourceChannel>() {

            byte [] buff = new byte[4096];
            AtomicLong total = new AtomicLong();

            @Override
            public void handleEvent(StreamSourceChannel channel) {
                try {
                    int c = 0;
                    final Pooled<ByteBuffer> pooled = exchange.getConnection().getBufferPool().allocate();
                    try {
                        final ByteBuffer buffer = pooled.getResource();
                        do {
                            buffer.clear();
                            c = channel.read(buffer);
                            if (c > 0) {
                                buffer.flip();
                                while ((c = buffer.remaining()) > 0) {
                                    c = c > buff.length ? buff.length : c;
                                    buffer.get(buff, 0, c);
                                    baos.write(buff, 0, c);
                                }
                            }
                        } while (c > 0 && baos.size() < Constants.MAX_REQUEST_SIZE);
                    } finally {
                        pooled.free();
                    }

                    if (baos.size() >= Constants.MAX_REQUEST_SIZE) {
                        if (isBinary) {
                            updateContent(channel);
                        } else {
                            responder.invalidRequest("Body exceeds size limit in bytes: " + Constants.MAX_REQUEST_SIZE);
                        }
                        return;
                    }

                    // after we have read the body, we have to continue an invocation where we left off
                    if (c == -1 || exchange.isRequestComplete()) {
                        if (isBinary) {
                            updateContent(channel);
                        } else {

                            ((LazyResourceState) context.requestState()).content(baos.toByteArray());

                            HttpHandler handler = new HttpHandler() {
                                @Override
                                public void handleRequest(HttpServerExchange exchange) throws Exception {
                                    runnable.run();
                                    Object result = null;
                                    if (responder instanceof BaseResponder) {
                                        List<Object> output = ((BaseResponder) responder).output();
                                        result = output.isEmpty() ? null : output.get(0);
                                    }
                                    Pipeline.instance(context).proceed(result);
                                }
                            };

                            if (resource instanceof BlockingResource) {
                                exchange.dispatch(handler);
                            } else {
                                exchange.dispatch(SameThreadExecutor.INSTANCE, handler);
                            }
                        }
                    }
                } catch (Exception e) {
                    responder.internalError(e);
                    //IoUtils.safeClose(channel);
                    //exchange.endExchange();
                }
            }

            protected void updateContent(final StreamSourceChannel channel) {
                HttpHandler handler = new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        long offset = total.get();
                        boolean complete = offset + baos.size() == bodyLen;
                        ((BinaryResource) resource).updateContent(context, responder,
                                baos.toByteArray(), offset, complete);

                        total.addAndGet(baos.size());
                        baos.reset();
                        channel.resumeReads();

                        if (exchange.isRequestComplete()) {
                            Object result = null;
                            if (responder instanceof BaseResponder) {
                                List<Object> output = ((BaseResponder) responder).output();
                                result = output.isEmpty() ? null : output.get(0);
                            }
                            Pipeline.instance(context).proceed(result);
                        }
                    }
                };

                if (resource instanceof BlockingResource) {
                    channel.suspendReads();
                    exchange.dispatch(handler);
                } else {
                    exchange.dispatch(SameThreadExecutor.INSTANCE, handler);
                }
            }
        };

        StreamSourceChannel channel = exchange.getRequestChannel();
        if (channel == null) {
            throw new IOException(UndertowMessages.MESSAGES.requestChannelAlreadyProvided());
        } else {
            // determine if we have the body content fully buffered already - if it was small enough
            if (resource instanceof BinaryResource) {
                if (((BinaryResource) resource).willProcessUpdate(context, responder)) {
                    channel.getReadSetter().set(bodyReadHandler);
                    channel.resumeReads();
                }
            } else {
                if (bodyFullyRead) {
                    bodyReadHandler.handleEvent(channel);
                } else {
                    channel.getReadSetter().set(bodyReadHandler);
                    channel.resumeReads();
                }
            }
        }
    }
}
