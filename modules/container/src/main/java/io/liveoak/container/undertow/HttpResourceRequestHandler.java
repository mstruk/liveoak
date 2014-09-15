package io.liveoak.container.undertow;

import java.util.concurrent.Executor;

import io.liveoak.common.codec.ResourceCodecManager;
import io.liveoak.container.protocols.http.HttpRequestProcessor;
import io.liveoak.container.protocols.http.HttpResponseProcessor;
import io.liveoak.container.protocols.http.ResourceRequestProcessor;
import io.liveoak.container.protocols.http.ResourceResponseBodyProcessor;
import io.liveoak.container.tenancy.GlobalContext;
import io.liveoak.container.Dispatcher;
import io.liveoak.container.traversal.Pipeline;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class HttpResourceRequestHandler implements HttpHandler {

    private final GlobalContext globalContext;
    private final ResourceCodecManager codecManager;
    private final Executor workerPool;

    public HttpResourceRequestHandler(GlobalContext globalContext, ResourceCodecManager codecManager, Executor workerPool) {
        this.globalContext = globalContext;
        this.codecManager = codecManager;
        this.workerPool = workerPool;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        Pipeline pipeline = new Pipeline();

        Dispatcher dispatcher = new UndertowDispatcher(exchange, workerPool);

        Pipeline.Processor head = new HttpRequestProcessor(pipeline, codecManager);
        Pipeline.Processor tail = head;

        Pipeline.Processor next = new ResourceRequestProcessor(pipeline, globalContext, dispatcher);
        tail = tail.next(next);

        next = new ResourceResponseBodyProcessor(pipeline, dispatcher);
        tail = tail.next(next);

        next = new HttpResponseProcessor(pipeline, exchange, codecManager);
        tail.next(next);

        pipeline.head(head);
        pipeline.proceed(exchange);
    }
}
