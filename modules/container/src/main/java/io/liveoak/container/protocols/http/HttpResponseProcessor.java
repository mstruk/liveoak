package io.liveoak.container.protocols.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.liveoak.common.DefaultResourceErrorResponse;
import io.liveoak.common.DefaultResourceRequest;
import io.liveoak.common.codec.EncodingResult;
import io.liveoak.common.codec.IncompatibleMediaTypeException;
import io.liveoak.common.codec.ResourceCodecManager;
import io.liveoak.container.tenancy.ApplicationContext;
import io.liveoak.container.tenancy.InternalApplication;
import io.liveoak.container.traversal.Pipeline;
import io.liveoak.spi.MediaType;
import io.liveoak.spi.MediaTypeMatcher;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.RequestType;
import io.liveoak.spi.ResourceErrorResponse;
import io.liveoak.spi.ResourcePath;
import io.liveoak.spi.ResourceRequest;
import io.liveoak.spi.ResourceResponse;
import io.liveoak.spi.resource.async.BinaryContentSink;
import io.liveoak.spi.resource.async.BinaryResource;
import io.liveoak.spi.resource.async.Resource;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class HttpResponseProcessor extends Pipeline.Processor<ResourceResponse, Object> {

    private static final Logger log = Logger.getLogger(HttpResponseProcessor.class);

    private final ResourceCodecManager codecManager;
    private final HttpServerExchange exchange;

    public HttpResponseProcessor(Pipeline pipeline, HttpServerExchange exchange, ResourceCodecManager codecManager) {
        super(pipeline);
        this.exchange = exchange;
        this.codecManager = codecManager;
    }

    @Override
    public void process(ResourceResponse response) throws Exception {

        if (response == null) {
            // the previous processor has proceeded to this one so we have to continue carry the flag
            pipeline().proceed();
            return;
        }

        int responseStatusCode = 0;
        String responseMessage = null;

        boolean shouldEncodeState = false;
        switch (response.responseType()) {
            case CREATED:
                responseStatusCode = StatusCodes.CREATED;
                shouldEncodeState = true;
                break;
            case READ:
                responseStatusCode = StatusCodes.OK;
                shouldEncodeState = true;
                break;
            case UPDATED:
                responseStatusCode = StatusCodes.OK;
                shouldEncodeState = true;
                break;
            case DELETED:
                responseStatusCode = StatusCodes.OK;
                shouldEncodeState = true;
                break;
            case ERROR:
                if (response instanceof ResourceErrorResponse) {
                    shouldEncodeState = true;
                    switch (((DefaultResourceErrorResponse) response).errorType()) {
                        case NOT_AUTHORIZED:
                            responseStatusCode = StatusCodes.UNAUTHORIZED;
                            break;
                        case FORBIDDEN:
                            responseStatusCode = StatusCodes.FORBIDDEN;
                            break;
                        case NOT_ACCEPTABLE:
                            responseStatusCode = StatusCodes.NOT_ACCEPTABLE;
                            break;
                        case NO_SUCH_RESOURCE:
                            responseStatusCode = StatusCodes.NOT_FOUND;
                            break;
                        case RESOURCE_ALREADY_EXISTS:
                            responseStatusCode = StatusCodes.NOT_ACCEPTABLE;
                            break;
                        case CREATE_NOT_SUPPORTED:
                            responseStatusCode = StatusCodes.METHOD_NOT_ALLOWED;
                            responseMessage = "Create not supported";
                            break;
                        case READ_NOT_SUPPORTED:
                            responseStatusCode = StatusCodes.METHOD_NOT_ALLOWED;
                            responseMessage = "Read not supported";
                            break;
                        case UPDATE_NOT_SUPPORTED:
                            responseStatusCode = StatusCodes.METHOD_NOT_ALLOWED;
                            responseMessage = "UpdateStep not supported";
                            break;
                        case DELETE_NOT_SUPPORTED:
                            responseStatusCode = StatusCodes.METHOD_NOT_ALLOWED;
                            responseMessage = "Delete not supported";
                            break;
                        case INTERNAL_ERROR:
                            responseStatusCode = StatusCodes.INTERNAL_SERVER_ERROR;
                            responseMessage = StatusCodes.INTERNAL_SERVER_ERROR_STRING;
                            break;
                    }

                    //TODO: add content values here to return proper error messages to the client
                    // eg unique error id, short error message, link to page with more information, etc...
                    //response.content().writeBytes(...)

                }
                break;
        }


        EncodingResult encodingResult = null;
        if (shouldEncodeState) {
            MediaTypeMatcher matcher = response.inReplyTo().mediaTypeMatcher();

            InternalApplication app = findApplication(response.resource());
            if (app != null) {
                ResourcePath htmlAppPath = app.htmlApplicationResourcePath();
                if ((!(response.resource() instanceof BinaryResource)) && (htmlAppPath != null)) {
                    MediaType bestMatch = matcher.findBestMatch(this.codecManager.mediaTypes());
                    if (bestMatch == MediaType.HTML) {
                        // HTML was requested and we have an HTML app
                        ResourceRequest htmlAppRequest = new DefaultResourceRequest.Builder(RequestType.READ, htmlAppPath).mediaTypeMatcher(response.inReplyTo().mediaTypeMatcher()).build();
                        //ctx.channel().pipeline().fireChannelRead(htmlAppRequest);
                        // TODO figure out a way to dispatch back to the start of our pipeline processing
                        return;
                    }
                }
            }

            try {
                encodingResult = encodeState(response.inReplyTo().requestContext(), matcher, response);
            } catch (IncompatibleMediaTypeException e) {
                log.error("Incompatible media type", e);
                exchange.setResponseCode(StatusCodes.NOT_ACCEPTABLE);
                exchange.getResponseSender().send(e.getMessage());
                exchange.endExchange();
                return;
            } catch (Throwable e) {
                log.error("Could not encode HTTP response", e);
                exchange.setResponseCode(StatusCodes.INTERNAL_SERVER_ERROR);
                exchange.endExchange();
                return;
            }
        }

        if (encodingResult != null) {

            if (response.resource() instanceof BinaryResource) {
                BinaryResource bin = (BinaryResource) response.resource();
                if (bin.contentLength() == 0) {
                    exchange.setResponseCode(responseStatusCode);
                    exchange.setResponseContentLength(0);
                } else {
                    exchange.setResponseCode(responseStatusCode);
                    exchange.setResponseContentLength(bin.contentLength());
                    exchange.getResponseHeaders().put(Headers.LOCATION, response.resource().uri().toString());
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, bin.mediaType().toString());

                    bin.readContent(response.inReplyTo().requestContext(), new EncodingBinaryContentSink(exchange));
                    return;
                }
            } else {
                byte [] content = encodingResult.encoded();

                exchange.setResponseCode(responseStatusCode);
                exchange.setResponseContentLength(content.length);

                if (response.resource() != null) {
                    exchange.getResponseHeaders().put(Headers.LOCATION, response.resource().uri().toString());
                } else {
                    exchange.getResponseHeaders().put(Headers.LOCATION, response.inReplyTo().resourcePath().toString());
                }
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, encodingResult.mediaType().toString());

                ByteBuffer buf = ByteBuffer.allocate(content.length);
                buf.put(content);
                buf.flip();
                exchange.getResponseSender().send(buf);
            }
        } else {
            exchange.setResponseCode(responseStatusCode);
        }

        exchange.endExchange();

        // TODO:
        //ctx.fireUserEventTriggered(new RequestCompleteEvent(msg.requestId()));

        pipeline().proceed();
    }

    protected EncodingResult encodeState(RequestContext ctx, MediaTypeMatcher mediaTypeMatcher, ResourceResponse response) throws Exception {
        return this.codecManager.encode(ctx, mediaTypeMatcher, response);
    }

    protected InternalApplication findApplication(Resource resource) {
        Resource current = resource;
        while (current != null) {
            if (current instanceof ApplicationContext) {
                return ((ApplicationContext) current).application();
            }
            current = current.parent();
        }
        return null;
    }


    static class EncodingBinaryContentSink implements BinaryContentSink {

        private HttpServerExchange exchange;
        private Sender sender;
        private volatile boolean flushed = true;
        private Object lock = new Object();


        /**
         * This class is supposed to be used by a single thread calling accept().
         *
         * Until accepted content is pushed to client socket in the background, another call to accept() will block and wait.
         * That means that pushing the whole body in one call to accept() will not block the calling thread.
         * Doing multiple calls to accept() may block the calling thread which means that a Resource performing such calling
         * has to perform that from a non-io thread, which may be achieved by implementing a BlovkingResource interface, or by
         * resource delegating execution to another executor / thread.
         *
         * @param exchange
         */
        public EncodingBinaryContentSink(HttpServerExchange exchange) {
            this.exchange = exchange;
            sender = exchange.getResponseSender();
        }

        @Override
        public void close() {
            sender.close();
            exchange.endExchange();
            // What does it mean to fire an event in our model?
            // We need a clean API for it
            // ctx.fireUserEventTriggered(new RequestCompleteEvent(msg.requestId()));
        }

        @Override
        public void accept(ByteBuffer buffer) {
            if (!flushed) {
                synchronized (lock) {
                    if (!flushed) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException("Interrupted", e);
                        }
                    }
                    flushed = false;
                }
            }
            sender.send(buffer, callback);
        }

        private IoCallback callback = new IoCallback() {
            @Override
            public void onComplete(HttpServerExchange exchange, Sender sender) {
                synchronized(lock) {
                    flushed = true;
                }
            }

            @Override
            public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
                synchronized(lock) {
                    flushed = true;
                }
            }
        };
    }
}
