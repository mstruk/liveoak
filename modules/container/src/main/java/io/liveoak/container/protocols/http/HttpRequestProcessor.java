package io.liveoak.container.protocols.http;

import io.liveoak.common.Constants;
import io.liveoak.common.DefaultMediaTypeMatcher;
import io.liveoak.common.DefaultResourceParams;
import io.liveoak.common.DefaultResourceRequest;
import io.liveoak.common.DefaultReturnFields;
import io.liveoak.common.codec.DefaultLazyResourceState;
import io.liveoak.common.codec.ResourceCodecManager;
import io.liveoak.container.traversal.Pipeline;
import io.liveoak.spi.MediaType;
import io.liveoak.spi.MediaTypeMatcher;
import io.liveoak.spi.Pagination;
import io.liveoak.spi.RequestType;
import io.liveoak.spi.ResourceParams;
import io.liveoak.spi.ResourcePath;
import io.liveoak.spi.ResourceRequest;
import io.liveoak.spi.ReturnFields;
import io.liveoak.spi.Sorting;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class HttpRequestProcessor extends Pipeline.Processor<HttpServerExchange, ResourceRequest> {

    private final ResourceCodecManager codecManager;

    public HttpRequestProcessor(Pipeline pipeline, ResourceCodecManager codecManager) {
        super(pipeline);
        this.codecManager = codecManager;
    }

    @Override
    public void process(HttpServerExchange exchange) throws Exception {

        String path = exchange.getRequestURI();
        int lastDotLoc = path.lastIndexOf('.');

        String extension = null;

        if (lastDotLoc > 0) {
            extension = path.substring(lastDotLoc + 1);
        }

        String acceptHeader = exchange.getRequestHeaders().getFirst(Headers.ACCEPT);
        if (acceptHeader == null) {
            acceptHeader = "application/json";
        }
        MediaTypeMatcher mediaTypeMatcher = new DefaultMediaTypeMatcher(acceptHeader, extension);

        ResourceParams params = DefaultResourceParams.instance(exchange.getQueryParameters());

        ResourceRequest request = null;
        if (exchange.getRequestMethod().equals(Methods.POST)) {
            String contentTypeHeader = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
            MediaType contentType = new MediaType(contentTypeHeader);
            request = new DefaultResourceRequest.Builder(RequestType.CREATE, new ResourcePath(path))
                    .resourceParams(params)
                    .mediaTypeMatcher(mediaTypeMatcher)
                    .requestAttribute(Headers.AUTHORIZATION_STRING, exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION))
                    .requestAttribute(Headers.CONTENT_TYPE_STRING, contentType)
                    .requestAttribute(Constants.HTTP_SERVER_EXCHANGE, exchange)
                    .resourceState(new DefaultLazyResourceState(codecManager, contentType, exchange))
                    .build();
        } else if (exchange.getRequestMethod().equals(Methods.GET)) {
            request = new DefaultResourceRequest.Builder(RequestType.READ, new ResourcePath(path))
                    .resourceParams(params)
                    .mediaTypeMatcher(mediaTypeMatcher)
                    .requestAttribute(Headers.AUTHORIZATION_STRING, exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION))
                    .requestAttribute(Constants.HTTP_SERVER_EXCHANGE, exchange)
                    .pagination(decodePagination(params))
                    .returnFields(decodeReturnFields(params))
                    .sorting(decodeSorting(params))
                    .build();
        } else if (exchange.getRequestMethod().equals(Methods.PUT)) {
            String contentTypeHeader = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
            MediaType contentType = new MediaType(contentTypeHeader);
            request = new DefaultResourceRequest.Builder(RequestType.UPDATE, new ResourcePath(path))
                    .resourceParams(params)
                    .mediaTypeMatcher(mediaTypeMatcher)
                    .requestAttribute(Headers.AUTHORIZATION_STRING, exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION))
                    .requestAttribute(Headers.CONTENT_TYPE_STRING, contentType)
                    .requestAttribute(Constants.HTTP_SERVER_EXCHANGE, exchange)
                    .resourceState(new DefaultLazyResourceState(codecManager, contentType, exchange))
                    .build();
        } else if (exchange.getRequestMethod().equals(Methods.DELETE)) {
            request = new DefaultResourceRequest.Builder(RequestType.DELETE, new ResourcePath(path))
                    .resourceParams(params)
                    .mediaTypeMatcher(mediaTypeMatcher)
                    .requestAttribute(Headers.AUTHORIZATION_STRING, exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION))
                    .requestAttribute(Constants.HTTP_SERVER_EXCHANGE, exchange)
                    .build();
        }

        if (request == null) {
            throw new UnsupportedOperationException(exchange.getRequestMethod().toString());
        }

        pipeline().associate(request.requestContext());
        pipeline().proceed(request);
    }

    private ReturnFields decodeReturnFields(ResourceParams params) {
        String fieldsValue = params.value("fields");
        DefaultReturnFields returnFields = null;
        if (fieldsValue != null && !"".equals(fieldsValue)) {
            returnFields = new DefaultReturnFields(fieldsValue);
        } else {
            returnFields = new DefaultReturnFields("*");
        }

        String expandValue = params.value("expand");

        if (expandValue != null && !"".equals(expandValue)) {
            returnFields = returnFields.withExpand(expandValue);
        }

        return returnFields;
    }

    protected Pagination decodePagination(ResourceParams params) {

        int offset = limit(intValue(params.value("offset"), 0), 0, Integer.MAX_VALUE);
        int limit = limit(intValue(params.value("limit"), Pagination.DEFAULT_LIMIT), 0, Pagination.MAX_LIMIT);

        return new Pagination() {
            public int offset() {
                return offset;
            }

            public int limit() {
                return limit;
            }
        };
    }

    protected Sorting decodeSorting(ResourceParams params) {
        String spec = params.value("sort");
        if (spec != null) {
            return new Sorting(spec);
        }
        return new Sorting();
    }

    private static int limit(int value, int lower, int upper) {
        if (value < lower) {
            return lower;
        } else if (value > upper) {
            return upper;
        }
        return value;
    }

    private int intValue(String value, int def) {
        if (value == null) {
            return def;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
