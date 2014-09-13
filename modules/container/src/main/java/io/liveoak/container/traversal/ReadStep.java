package io.liveoak.container.traversal;

import io.liveoak.spi.resource.async.Resource;

/**
* @author Bob McWhirter
*/
public class ReadStep implements TraversalPlan.Step {
    public ReadStep(String name) {
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    private String name;

    @Override
    public void execute(TraversalPlan.StepContext context, Resource resource) throws Exception {
        resource.readMember(context.requestContext(), this.name, context.responder());
        // how does the pipeline work for BinaryResources that write big streams to out?
        // the same way there are partial uploads there are partial downloads as well
        // when resource produces a response it should have a way of communicating back to container offset and size it is returning
        // Range should be (offset,len), not (from,to)
    }

    public String toString() {
        return "[ReadStep: name=" + this.name + "]";
    }
}
