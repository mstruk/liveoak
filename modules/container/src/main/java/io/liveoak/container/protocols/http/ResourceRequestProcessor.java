package io.liveoak.container.protocols.http;

import java.util.LinkedList;

import io.liveoak.container.tenancy.GlobalContext;
import io.liveoak.container.Dispatcher;
import io.liveoak.container.traversal.Pipeline;
import io.liveoak.container.traversal.TraversingResponder;
import io.liveoak.spi.ResourceRequest;
import io.liveoak.spi.ResourceResponse;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class ResourceRequestProcessor extends Pipeline.Processor<ResourceRequest, ResourceResponse> {

    private final GlobalContext globalContext;
    private final Dispatcher dispatcher;

    public ResourceRequestProcessor(Pipeline pipeline, GlobalContext globalContext, Dispatcher dispatcher) {
        super(pipeline);
        this.globalContext = globalContext;
        this.dispatcher = dispatcher;
    }

    @Override
    public void process(ResourceRequest request) throws Exception {
        if (request == null) {
            // the previous processor has proceeded to this one so we have to continue carry the flag
            pipeline().proceed();
            return;
        }

        LinkedList<Object> out = new LinkedList<>();
        new TraversingResponder(dispatcher, globalContext, request, out).resourceRead(globalContext);

        if (out.isEmpty()) {
            // what to do here?
            // we can't proceed without output
            // but we have to - the last processor should be error processor handling the situation
            // out should never be empty!
            // Looks like it should all be simplified - we don't care about null, about types, we just pass on
            // it's not us to take care of errors
            // unless it's up to us to construct an error object
            // in that case it would be simplest for all processors to simply pass on an exception object

            // WRONG: if out is empty it means we have dispatched processing, and pipeline will be continued in another invocation
            // We must not proceed.
            //pipeline().proceed();
            return;
        }

        Object result = out.getFirst();
        if (result instanceof ResourceResponse) {
            pipeline().proceed(result);
        } else {
            // TODO: what if it is an error?
            // We have to have an error handler at the end of the pipeline and
            // it has to take care of things
            pipeline().proceed(result);
        }
    }
}
