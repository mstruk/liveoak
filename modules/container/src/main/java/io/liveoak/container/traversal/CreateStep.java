package io.liveoak.container.traversal;

import io.liveoak.spi.resource.async.Resource;

/**
* @author Bob McWhirter
*/
public class CreateStep implements TraversalPlan.Step {

    @Override
    public void execute(TraversalPlan.StepContext context, Resource resource) throws Exception {
        context.dispatcher().create(context.requestContext(), resource, context.responder());
    }
}
