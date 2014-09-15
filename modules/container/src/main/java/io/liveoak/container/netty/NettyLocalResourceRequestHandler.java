package io.liveoak.container.netty;

import io.liveoak.container.Dispatcher;
import io.liveoak.container.protocols.http.ResourceRequestProcessor;
import io.liveoak.container.protocols.http.ResourceResponseBodyProcessor;
import io.liveoak.container.tenancy.GlobalContext;
import io.liveoak.container.traversal.Pipeline;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class NettyLocalResourceRequestHandler extends ChannelDuplexHandler {

    private final GlobalContext globalContext;

    public NettyLocalResourceRequestHandler(GlobalContext globalContext) {
        this.globalContext = globalContext;
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        super.read(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Pipeline pipeline = new Pipeline();

        Dispatcher dispatcher = new NettyDispatcher(ctx);

        Pipeline.Processor head = new ResourceRequestProcessor(pipeline, globalContext, dispatcher);
        Pipeline.Processor tail = head;

        Pipeline.Processor next = new ResourceResponseBodyProcessor(pipeline, dispatcher);
        tail = tail.next(next);

        next = new Pipeline.Processor(pipeline) {

            @Override
            public void process(Object msg) throws Exception {
                flushToChannel(ctx, msg);
            }
        };
        tail.next(next);

        pipeline.head(head);
        pipeline.proceed(msg);
    }

    private void flushToChannel(ChannelHandlerContext ctx, Object msg) {
        if (msg != null) {
            ctx.writeAndFlush(msg);
        }
        ChannelPipeline pipeline = ctx.pipeline();
        if (pipeline == null) {
            return;
        }
        ChannelHandlerContext context = pipeline.firstContext();
        if (context == null) {
            return;
        }
        context.read();
    }
}
