package io.liveoak.spi.resource;

import java.util.Collection;

import io.liveoak.spi.RequestContext;
import io.liveoak.spi.resource.async.PropertySink;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.spi.resource.async.ResourceSink;
import io.liveoak.spi.resource.async.Responder;
import io.liveoak.spi.state.ResourceState;

/**
 * @author Bob McWhirter
 */
public interface SynchronousResource extends Resource {

    default Collection<? extends Resource> members() {
        return null;
    }

    default Resource member(String id) {
        return null;
    }

    default ResourceState properties() throws Exception {
        return null;
    }

    default void properties(ResourceState props) throws Exception {
        // nothing
    }

    @Override
    default void readMembers(RequestContext ctx, ResourceSink sink) throws Exception {
        Collection<? extends Resource> members = members();

        if (members != null) {
            for (Resource each : members) {
                sink.accept(each);
            }
        }

        sink.close();
    }

    @Override
    default void readMember(RequestContext ctx, String id, Responder responder) throws Exception {
        Resource member = member(id);
        if (member == null) {
            responder.noSuchResource(id);
            return;
        }

        responder.resourceRead(member);
    }

    @Override
    default void readProperties(RequestContext ctx, PropertySink sink) throws Exception {
        ResourceState props = properties();

        if (props != null) {
            props.object().fieldNames().forEachRemaining(key -> sink.accept(key, props.getProperty(key)));
        }

        sink.close();
    }

    @Override
    default void updateProperties(RequestContext ctx, ResourceState state, Responder responder) throws Exception {
        properties( state );
        responder.resourceUpdated( this );
    }
}
