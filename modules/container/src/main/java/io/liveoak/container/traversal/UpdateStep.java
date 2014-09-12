package io.liveoak.container.traversal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import io.liveoak.common.Constants;
import io.liveoak.spi.resource.BlockingResource;
import io.liveoak.spi.resource.async.BinaryResource;
import io.liveoak.spi.resource.async.DelegatingResponder;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.spi.resource.async.Responder;
import io.liveoak.spi.state.LazyResourceState;
import io.undertow.UndertowMessages;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpServerConnection;
import io.undertow.util.SameThreadExecutor;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.Pooled;
import org.xnio.channels.StreamSourceChannel;

/**
 * @author Bob McWhirter
 */
public class UpdateStep implements TraversalPlan.Step {

    //private boolean complete;

    @Override
    public void execute(TraversalPlan.StepContext context, Resource resource) throws Exception {
        HttpServerExchange exchange = (HttpServerExchange) context.requestContext()
                .requestAttributes().getAttribute(Constants.HTTP_SERVER_EXCHANGE);

        if (exchange == null) {
            throw new RuntimeException("exchange == null");
        }

        Pooled<ByteBuffer> extraBytes = ((HttpServerConnection) exchange.getConnection()).getExtraBytes();

        long bodyLen = exchange.getRequestContentLength();

        boolean bodyFullyRead = extraBytes != null ? bodyLen <= extraBytes.getResource().remaining() : false;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChannelListener<StreamSourceChannel> bodyReadHandler = new ChannelListener<StreamSourceChannel>() {

            byte [] buff = new byte[4096];

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
                        } while (c > 0);
                    } finally {
                        pooled.free();
                    }

                    // after we have read the body, we have to continue an invocation where we left off
                    if (c == -1 || exchange.isRequestComplete()) {
                        ((LazyResourceState) context.state()).content(baos.toByteArray());

                        // TODO this doesn't look right as it only completes TraversingResponder task
                        // without also going through HttpResourceStateEncoder and HttpResourceResponseEncoder
                        HttpHandler handler = new HttpHandler() {
                            @Override
                            public void handleRequest(HttpServerExchange exchange) throws Exception {
                                resource.updateProperties(context.requestContext(), context.state(), context.responder());
                                List<Object> output = context.output();
                                Pipeline.instance(context.requestContext()).proceed(output.isEmpty() ? null : output.get(0));
                            }
                        };

                        if (resource instanceof BlockingResource) {
                            exchange.dispatch(handler);
                        } else {
                            exchange.dispatch(SameThreadExecutor.INSTANCE, handler);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Failed to handle channel event: ");
                    e.printStackTrace();
                    IoUtils.safeClose(channel);
                    exchange.endExchange();
                }
            }
        };

        StreamSourceChannel channel = exchange.getRequestChannel();
        if (channel == null) {
            throw new IOException(UndertowMessages.MESSAGES.requestChannelAlreadyProvided());
        } else {
            // determine if we have the body content fully buffered already - if it was small enough
            if (bodyFullyRead) {
                bodyReadHandler.handleEvent(channel);

            } else {
                channel.getReadSetter().set(bodyReadHandler);
                channel.resumeReads();
            }
        }

    }

    @Override
    public Responder createResponder(TraversingResponder responder) {
        return new DelegatingResponder(responder) {
            @Override
            public void noSuchResource(String id) {
                responder.inReplyTo().state().id(id);
                responder.replaceStep(UpdateStep.this, new CreateStep());
                responder.doNextStep(responder.currentResource());
            }
        };
    }
}
