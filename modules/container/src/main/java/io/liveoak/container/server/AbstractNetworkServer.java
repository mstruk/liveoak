/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import io.liveoak.spi.container.NetworkServer;

/**
 * Base networkServer capable of connecting a container to a network ports.
 *
 * @author Bob McWhirter
 */
public abstract class AbstractNetworkServer extends AbstractServer implements NetworkServer {

    public AbstractNetworkServer() {
    }

    @Override
    public SocketAddress localAddress() {
        return new InetSocketAddress(this.host, this.port);
    }

    @Override
    public void host(InetAddress host) {
        this.host = host;
    }

    @Override
    public InetAddress host() {
        return this.host;
    }

    @Override
    public void port(int port) {
        this.port = port;
    }

    @Override
    public int port() {
        return this.port;
    }

    private int port;
    private InetAddress host;
}
