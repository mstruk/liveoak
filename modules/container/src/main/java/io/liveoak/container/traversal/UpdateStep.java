package io.liveoak.container.traversal;

import io.liveoak.spi.resource.async.DelegatingResponder;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.spi.resource.async.Responder;

/**
 * @author Bob McWhirter
 */
public class UpdateStep implements TraversalPlan.Step {

    @Override
    public void execute(TraversalPlan.StepContext context, Resource resource) throws Exception {
        context.dispatcher().update(context.requestContext(), resource, context.responder());
    }

    @Override
    public Responder createResponder(TraversingResponder responder) {
        return new DelegatingResponder(responder) {
            @Override
            public void noSuchResource(String id) {
                responder.inReplyTo().state().id(id);
                responder.replaceStep(UpdateStep.this, new CreateStep());
                responder.doNextStep(responder.currentResource());
            }
        };
    }
}
