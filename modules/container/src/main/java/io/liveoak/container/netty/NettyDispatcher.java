package io.liveoak.container.netty;

import io.liveoak.container.Dispatcher;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.spi.resource.async.Responder;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class NettyDispatcher implements Dispatcher {

    private final ChannelHandlerContext ctx;

    public NettyDispatcher(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void dispatch(Runnable runnable) {
        this.ctx.pipeline().channel().eventLoop().execute(runnable);
    }

    @Override
    public void update(RequestContext context, Resource resource, Responder responder) throws Exception {
        resource.updateProperties(context, context.requestState(), responder);
    }

    @Override
    public void create(RequestContext context, Resource resource, Responder responder) throws Exception {
        resource.createMember(context, context.requestState(), responder);
    }
}
