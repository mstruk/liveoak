package io.liveoak.scripts.resourcetriggered.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.liveoak.spi.InvalidPropertyTypeException;
import io.liveoak.spi.PropertyException;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.RequiredPropertyException;
import io.liveoak.spi.resource.async.PropertySink;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.spi.resource.async.ResourceSink;
import io.liveoak.spi.resource.async.Responder;
import io.liveoak.spi.state.LazyResourceState;
import io.liveoak.spi.state.ResourceState;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="mailto:mwringe@redhat.com">Matt Wringe</a>
 */
public class ResourceScript implements Resource {

    // Property names specified in the resource state
    protected static final String NAME = "name";
    protected static final String DESCRIPTION = "description";
    protected static final String ENABLED = "enabled";
    protected static final String LIBRARIES = "libraries";
    protected static final String TARGET_PATH = "target-path";
    protected static final String PRIORITY = "priority";
    protected static final String PROVIDES = "provides";

    private ResourceScripts parent;

    // The script which this resource represents
    private Script script;

    public ResourceScript(ResourceScripts parent, ResourceState state) throws Exception {
        this.parent = parent;
        this.script = createScript(state);
    }

    public ResourceScript(ResourceScripts parent, Script script) throws Exception {
        this.parent = parent;
        this.script = script;
    }

    protected Script createScript(ResourceState state) throws PropertyException {

        String id = state.id();
        if (id == null) {
            if (script != null) {
                id = script.getId();
            } else {
                id = UUID.randomUUID().toString();
            }
        }

        String target = (String)getProperty(TARGET_PATH, state, true, String.class);

        Script.Builder builder = new Script.Builder(id, target);

        String name = (String) getProperty(NAME, state, false, String.class);
        if (name != null) {
            builder.setName(name);
        }

        String description = (String)getProperty(DESCRIPTION, state, false, String.class);
        if (description != null) {
            builder.setDescription(description);
        }

        Boolean enabled = (Boolean) getProperty(ENABLED, state, false, Boolean.class);
        if (enabled != null) {
            builder.setEnabled(enabled);
        }

        Integer priority = (Integer) getProperty(PRIORITY, state, false, Integer.class);
        if (priority != null) {
            builder.setPriority(priority);
        }

        List librariesProperty = (List) getProperty(LIBRARIES, state, false, ArrayList.class);
        if (librariesProperty != null) {
            List<String> libraries = new ArrayList<String>(librariesProperty.size());
            for (Object libName: librariesProperty) {
                if (libName instanceof String) {
                    libraries.add((String)libName);
                } else {
                    throw new InvalidPropertyTypeException(LIBRARIES, String.class, true);
                }
            }
            builder.setLibraries(libraries);
        }

        return builder.build();
    }

    //TODO: move this to a utility class or to the ResourceState class directly?
    protected Object getProperty(String name, ResourceState state, boolean required, Class<?> requestedType) throws PropertyException {
        Object propertyObject = state.getProperty(name);
        if (required && propertyObject == null) {
            throw new RequiredPropertyException(name, requestedType);
        } else if (propertyObject == null) {
            return null;
        } else if (propertyObject.getClass() == requestedType) {
            return requestedType.cast(propertyObject);
        } else {
            throw new InvalidPropertyTypeException(name, requestedType);
        }
    }

    public Script getScript() {
        return script;
    }

    @Override
    public Resource parent() {
        return this.parent;
    }

    @Override
    public String id() {
        return script.getId();
    }

    @Override
    public void readProperties(RequestContext ctx, PropertySink sink) throws Exception {
        sink.accept(NAME, script.getName());
        sink.accept(DESCRIPTION, script.getDescription());
        sink.accept(ENABLED, script.isEnabled());
        sink.accept(TARGET_PATH, script.getTarget());
        sink.accept(PRIORITY, script.getPriority());
        sink.accept(LIBRARIES, script.getLibraries());
        sink.accept(PROVIDES, script.getProvides());
        sink.close();
    }

    @Override
    public void updateProperties(RequestContext ctx, ResourceState state, Responder responder) throws Exception {
        try {
            if (script != null && state.id() != null && !state.id().equals(script.getId())) {
                responder.invalidRequest("The resource ID cannot be changed during an update.");
            } else {
                // since we don't do partial updates, we need to overwrite everything here with the new state
                this.script = createScript(state);
                parent.updateChild(this);
                responder.resourceUpdated(this);
            }
        } catch (PropertyException pe) {
            responder.invalidRequest(pe.getMessage());
        } catch (Exception e) {
            responder.internalError(e.getMessage());
        }
    }

    @Override
    public void createMember(RequestContext ctx, ResourceState state, Responder responder) throws Exception {
        if (state instanceof LazyResourceState) {
            LazyResourceState lazyResourceState = (LazyResourceState) state;
            byte [] content = lazyResourceState.contentAsByteBuffer();

            script.setScriptBuffer(content);
            parent.writeSourceFile(this.id(), script.getScriptBuffer());

            responder.resourceCreated(new ScriptFileResource(this));
            return;
        }
        responder.invalidRequest("The uploaded script must be a binary javascript file.");
    }

    @Override
    public void readMembers(RequestContext ctx, ResourceSink sink) throws Exception {
        if (script.getScriptBuffer() != null) {
            sink.accept(new ScriptFileResource(this));
        }
        sink.close();
    }

    @Override
    public void readMember(RequestContext ctx, String id, Responder responder) throws Exception {
        if (id.equals(ScriptFileResource.ID) && script.getScriptBuffer() != null) {
            responder.resourceRead(new ScriptFileResource(this));
        } else {
            responder.noSuchResource(id);
        }
    }

    @Override
    public void delete(RequestContext ctx, Responder responder) throws Exception {
        parent.deleteMember(ctx, script.getId(), responder);
        this.script = null;
    }

    //TODO: add DeleteMember to the Resource object?
    public void deleteMember(RequestContext ctx, String id, Responder responder) throws Exception {
        if (id == ScriptFileResource.ID && script.getScriptBuffer() != null) {
            Resource resource = new ScriptFileResource(this);
            this.script.setScriptBuffer(null);
            responder.resourceDeleted(resource);
        } else {
            responder.noSuchResource(id);
        }
    }
}
