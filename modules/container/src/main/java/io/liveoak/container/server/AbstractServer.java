/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container.server;

import io.liveoak.spi.container.Server;
import io.liveoak.container.protocols.PipelineConfigurator;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Base networkServer capable of connecting a container to a network ports.
 *
 * @author Bob McWhirter
 */
public abstract class AbstractServer implements Server {

    public AbstractServer() {
    }

    private Undertow server;
    public abstract SocketAddress localAddress();

    public void pipelineConfigurator(PipelineConfigurator pipelineConfigurator) {
        this.pipelineConfigurator = pipelineConfigurator;
    }

    public PipelineConfigurator pipelineConfigurator() {
        return this.pipelineConfigurator;
    }

    /**
     * Synchronously start the network listener.
     *
     * @throws InterruptedException If interrupted before completely starting.
     */
    public void start() throws InterruptedException {

        SocketAddress address = localAddress();
        if (address instanceof InetSocketAddress == false) {
            throw new IllegalStateException("This server only supports InetSocketAddresses");
        }
        InetSocketAddress addr = (InetSocketAddress) address;

        server = Undertow.builder()
            .addHttpListener(addr.getPort(), addr.getHostString())
            .setHandler(createHandler())
                .build();

        server.start();
    }

    /**
     * Synchronously stop the network listener.
     *
     * @throws InterruptedException If interrupted before completely stopping.
     */
    public void stop() throws InterruptedException {
        server.stop();
    }

    protected PipelineConfigurator getPipelineConfigurator() {
        return this.pipelineConfigurator;
    }

    /**
     * Create a networkServer-specific port-handler.
     *
     * <p>This is implemented by concrete subclasses to provide
     * SSL or bare networking handling.</p>
     *
     * @return The channel-handler for the netowrk listener.
     */
    protected abstract HttpHandler createHandler();

    private PipelineConfigurator pipelineConfigurator;

}
