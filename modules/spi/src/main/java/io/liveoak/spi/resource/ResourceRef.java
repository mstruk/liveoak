package io.liveoak.spi.resource;

import java.net.URI;

import io.liveoak.spi.ResourcePath;
import io.liveoak.spi.resource.async.Resource;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class ResourceRef implements Resource {

    private ResourcePath resourcePath;
    private Resource parent;
    private String id;

    public ResourceRef(Resource parent, String id) {
        this.parent = parent;
        this.id = id;
    }

    public ResourceRef(ResourcePath resourcePath) {
        this.resourcePath = resourcePath;
        this.parent = new ResourceRef(resourcePath.parent());
        this.id = resourcePath.tail().name();
    }

    public ResourceRef(URI uri) {
        resourcePath = new ResourcePath(uri.getPath().toString());
        this.parent = new ResourceRef(resourcePath.parent());
        this.id = resourcePath.tail().name();
    }

    @Override
    public Resource parent() {
        return parent;
    }

    @Override
    public String id() {
        return id;
    }
}
