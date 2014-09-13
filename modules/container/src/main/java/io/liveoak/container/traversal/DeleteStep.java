package io.liveoak.container.traversal;

import java.util.List;

import io.liveoak.spi.resource.async.Resource;

/**
* @author Bob McWhirter
*/
public class DeleteStep implements TraversalPlan.Step {

    @Override
    public void execute(TraversalPlan.StepContext context, Resource resource) throws Exception {
        resource.delete(context.requestContext(), context.responder());
        List<Object> output = context.output();
        Pipeline.instance(context.requestContext()).proceed(output.isEmpty() ? null : output.get(0));
    }
}
