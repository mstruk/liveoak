package io.liveoak.container;

import io.liveoak.spi.RequestContext;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.spi.resource.async.Responder;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public interface Dispatcher {

    void dispatch(Runnable runnable);

    void update(RequestContext context, Resource resource, Responder responder) throws Exception;

    void create(RequestContext context, Resource resource, Responder responder) throws Exception;
}
