package io.liveoak.container.undertow;

import java.util.concurrent.Executor;

import io.liveoak.container.Dispatcher;
import io.liveoak.container.DispatcherFactory;
import io.undertow.server.HttpServerExchange;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class UndertowDispatcherFactory implements DispatcherFactory<HttpServerExchange> {

    private final Executor workerPool;

    public UndertowDispatcherFactory(Executor workerPool) {
        this.workerPool = workerPool;
    }

    public Dispatcher createDispatcher(HttpServerExchange exchange) {
        return new UndertowDispatcher(exchange, workerPool);
    }
}
