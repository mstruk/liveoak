package org.projectodd.restafari.container.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.List;

import org.projectodd.restafari.container.codec.ResourceCodec;
import org.projectodd.restafari.container.codec.ResourceCodecManager;
import org.projectodd.restafari.container.responses.ResourceResponse;

public class HttpResourceResponseEncoder extends MessageToMessageEncoder<ResourceResponse> {
    
    public HttpResourceResponseEncoder(ResourceCodecManager codecManager) {
        this.codecManager = codecManager;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ResourceResponse msg, List<Object> out) throws Exception {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK );
        ResourceCodec codec = this.codecManager.getResourceCodec( msg.getMimeType() );
        ByteBuf encoded = codec.encode(msg.getResource());
        
        response.headers().add( HttpHeaders.Names.CONTENT_LENGTH, encoded.readableBytes() );
        response.content().writeBytes( encoded );
        out.add( response );
    }
    
    private ResourceCodecManager codecManager;


}