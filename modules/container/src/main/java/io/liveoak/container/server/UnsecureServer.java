/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container.server;

import io.liveoak.container.protocols.http.HttpResourceRequestHandler;

import io.undertow.server.HttpHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

public class UnsecureServer extends AbstractNetworkServer {

    public UnsecureServer() {
        super();
    }

    @Override
    protected HttpHandler createHandler() {
        return new WebSocketProtocolHandshakeHandler(new WebSocketConnectionCallback() {
            @Override
            public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                // todo
            }
        }, new HttpResourceRequestHandler(pipelineConfigurator().globalContext(), pipelineConfigurator().codecManager(), pipelineConfigurator().workerPool()));
    }
}
