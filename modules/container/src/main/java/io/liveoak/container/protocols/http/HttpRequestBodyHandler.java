package io.liveoak.container.protocols.http;

import java.nio.charset.Charset;
import java.util.List;
import io.liveoak.common.DefaultResourceRequest;
import io.liveoak.common.codec.ResourceCodecManager;
import io.liveoak.spi.RequestType;
import io.liveoak.spi.ResourcePath;
import io.liveoak.spi.state.LazyResourceState;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.util.ReferenceCountUtil;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class HttpRequestBodyHandler extends MessageToMessageDecoder<Object> {

    private ResourceCodecManager mgr;
    private DefaultResourceRequest request;
    private FileUpload fileUpload;

    private static final HttpDataFactory factory =
            new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); //Use disk if size exceed


    public HttpRequestBodyHandler(ResourceCodecManager codecManager) {
        this.mgr = codecManager;
    }


    /*
     * This object performs a role similar to HttpObjectAggregator.
     * If DefaultResourceRequest is received, it means the request head has been parsed and converted, but not yet
     * the request body necessarily.
     *
     * We try to handle the body intelligently. Up to some limit we only employ memory buffer.
     * If size is greater than that, we cache to disk.
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {

        if (msg instanceof LastHttpContent) {
            // Use the witheld DefaultResourceRequest - cashed to a field
            // getState returns ResourceState with ContentType, and ContentLength,
            // and possibly some other header there.
            // Call state.updateComplete(msg.content())  - that passes ByteBuf.

            // This can write the last chunk into memory buffer if uplimit not yet reached,
            // otherwise appends to already existing tmpFile it created.
            // It marks itself as complete(), and we now pass the witheld request on to outgoing queue
            // via add()
            //if (request.state() != null) {
            //    ((LazyResourceState) request.state()).updateComplete(((HttpContent) msg).content());
            //}
            if (fileUpload != null) {
                fileUpload.addContent(((HttpContent) msg).content().retain(), true);
                if (request.state() != null) {
                    ((LazyResourceState) request.state()).fileUpload(fileUpload);
                } else {
                    fileUpload.delete();
                }
            }
            out.add(request);
            request = null;
        } else if (msg instanceof HttpContent) {
            // Call state.update(msg.content()) on ResourceState of the witheld
            // DefaultResourceRequest
            //((LazyResourceState) request.state()).update(((HttpContent) msg).content());
            if (fileUpload != null) {
                fileUpload.addContent(((HttpContent) msg).content().retain(), false);
            }
            // otherwise just dump the sent content
        } else if (msg instanceof DefaultResourceRequest) {
            // Check ResourceState returned by getState to see if there is a body expected
            // Presence of content type, or content length both signifies we expect a body
            // In that case we withold the object.
            // We automatically ignore any body if requestType is not CREATE or UPDATE
            // In that case we pass the object on as it is complete.
            if (this.request != null) {
                throw new IllegalStateException("Assertion failed: request not null on first invocation!");
            }
            this.request = (DefaultResourceRequest) msg;

            if (request.requestType() != RequestType.CREATE && request.requestType() != RequestType.UPDATE) {
                return;
            }

            HttpRequest original = (HttpRequest) request.requestContext().requestAttributes().getAttribute("HTTP_REQUEST");
            List<ResourcePath.Segment> segments = request.resourcePath().segments();
            String filename = segments.size() < 1 ? "unknown" : segments.get(segments.size()-1).name();
            factory.createAttribute(original, "filename", filename);

            String contentLength = original.headers().get("Content-Length");
            long clen = 0;
            if (contentLength != null) {
                factory.createAttribute(original, "Content-Length", contentLength);
                try {
                    clen = Long.parseLong(contentLength);
                } catch (Exception ignored) {
                    // TODO log this?
                }
            }
            String contentType = original.headers().get("Content-Type");
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            factory.createAttribute(original, "Content-Type", contentType);

            fileUpload = factory.createFileUpload(original,
                    request.resourcePath().toString(), filename,
                    contentType, "binary", Charset.forName("utf-8"),
                    clen);
        } else {
            // in any other case simply pass the message on
            out.add(ReferenceCountUtil.retain(msg));
        }
    }
}
