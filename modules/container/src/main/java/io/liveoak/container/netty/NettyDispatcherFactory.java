package io.liveoak.container.netty;

import io.liveoak.container.Dispatcher;
import io.liveoak.container.DispatcherFactory;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class NettyDispatcherFactory implements DispatcherFactory<ChannelHandlerContext> {

    @Override
    public Dispatcher createDispatcher(ChannelHandlerContext delegate) {
        return new NettyDispatcher(delegate);
    }
}
