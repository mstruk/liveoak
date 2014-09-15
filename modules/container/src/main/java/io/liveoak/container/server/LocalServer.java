package io.liveoak.container.server;

import java.net.SocketAddress;

import io.liveoak.container.protocols.PipelineConfigurator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;

/**
 * @author Bob McWhirter
 */
public class LocalServer {

    public LocalServer() {
        this.group = new NioEventLoopGroup();
    }

    public void pipelineConfigurator(PipelineConfigurator pipelineConfigurator) {
        this.pipelineConfigurator = pipelineConfigurator;
    }

    protected EventLoopGroup eventLoopGroup() {
        return this.group;
    }

    protected Class<? extends ServerChannel> channelClass() {
        return LocalServerChannel.class;
    }

    public SocketAddress localAddress() {
        return new LocalAddress("liveoak");
    }

    protected ChannelHandler createChildHandler() {
        return new ChannelInitializer<LocalChannel>() {
            protected void initChannel(LocalChannel ch) throws Exception {
                pipelineConfigurator.setupLocal(ch.pipeline());
            }
        };
    }

    /**
     * Synchronously start the network listener.
     *
     * @throws InterruptedException If interrupted before completely starting.
     */
    public void start() throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .channel(channelClass())
                .group(eventLoopGroup())
                .localAddress(localAddress())
                        //.handler( new DebugHandler( "server-handler" ) )
                .childHandler(createChildHandler());
        ChannelFuture future = serverBootstrap.bind();
        future.sync();
    }

    /**
     * Synchronously stop the network listener.
     *
     * @throws InterruptedException If interrupted before completely stopping.
     */
    public void stop() throws InterruptedException {
        Future<?> future = eventLoopGroup().shutdownGracefully();
        future.sync();
    }

    private EventLoopGroup group;
    private PipelineConfigurator pipelineConfigurator;
}
