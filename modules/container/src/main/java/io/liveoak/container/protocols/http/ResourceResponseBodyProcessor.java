package io.liveoak.container.protocols.http;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

import io.liveoak.client.impl.ClientResourceResponseImpl;
import io.liveoak.common.DefaultResourceErrorResponse;
import io.liveoak.common.codec.driver.RootEncodingDriver;
import io.liveoak.common.codec.state.ResourceStateEncoder;
import io.liveoak.container.traversal.Pipeline;
import io.liveoak.spi.ResourceErrorResponse;
import io.liveoak.spi.ResourceResponse;
import io.liveoak.spi.client.ClientResourceResponse;
import io.liveoak.spi.resource.BlockingResource;
import io.liveoak.spi.state.ResourceState;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class ResourceResponseBodyProcessor extends Pipeline.Processor<ResourceResponse, ResourceResponse> {


    private final Executor workerPool;

    public ResourceResponseBodyProcessor(Pipeline pipeline, Executor workerPool) {
        super(pipeline);
        this.workerPool = workerPool;
    }

    @Override
    public void process(ResourceResponse response) throws Exception {

        if (response == null) {
            // the previous processor has proceeded to this one so we have to continue carry the flag
            pipeline().proceed();
            return;
        }

        if (response instanceof ResourceResponse && !(response instanceof ResourceErrorResponse)) {

            Runnable action = () -> {
                List<Object> out = new LinkedList<>();
                try {
                    encode(response, out);
                    pipeline().proceed(out.get(0));
                } catch (Exception e) {
                    // TODO: log properly
                    e.printStackTrace();
                    try {
                        pipeline().proceed(new DefaultResourceErrorResponse(response.inReplyTo(), ResourceErrorResponse.ErrorType.INTERNAL_ERROR, e.toString(), e));
                    } catch (Exception ex) {
                        // TODO: log properly
                        e.printStackTrace();
                    }
                }
            };

            if (response.resource() instanceof BlockingResource) {
                this.workerPool.execute(action);
            } else {
                action.run();
            }

        } else {
            pipeline().proceed(response);
        }
    }

    /**
     * Encode (for some cheap value of 'encode') a resulting resource into a ResourceState.
     *
     * @param response The response to encode.
     * @throws Exception
     */
    protected void encode(ResourceResponse response, List<Object> out) {
        final ClientResourceResponse.ResponseType responseType = ClientResourceResponse.ResponseType.OK;
        if (response.resource() == null) {
            out.add(new ClientResourceResponseImpl(response.inReplyTo(), responseType, response.inReplyTo().resourcePath().toString(), null));
            // TODO fire event
            //ctx.fireUserEventTriggered(new RequestCompleteEvent(response.requestId()));
            return;
        }

        final ResourceStateEncoder encoder = new ResourceStateEncoder();

        RootEncodingDriver driver = new RootEncodingDriver(response.inReplyTo().requestContext(), encoder, response.resource(), () -> {
            ResourceState state = encoder.root();
            response.setState(state);
            out.add(response);
        });

        try {
            driver.encode();
        } catch (Exception e) {
            out.add(new DefaultResourceErrorResponse( response.inReplyTo(), ResourceErrorResponse.ErrorType.NOT_ACCEPTABLE, e.getMessage(), e ) );
            // TODO fire event
            //ctx.fireUserEventTriggered(new RequestCompleteEvent(response.requestId()));
        }

    }
}
