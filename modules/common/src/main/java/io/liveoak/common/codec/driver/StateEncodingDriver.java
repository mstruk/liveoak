
package io.liveoak.common.codec.driver;

import com.fasterxml.jackson.databind.JsonNode;
import io.liveoak.common.codec.NonEncodableValueException;
import io.liveoak.common.codec.StateEncoder;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.state.ResourceState;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="mailto:mwringe@redhat.com">Matt Wringe</a>
 * @author Ken Finnigan
 */
public class StateEncodingDriver extends AbstractEncodingDriver {

    protected StateEncoder stateEncoder;

    public StateEncodingDriver(RequestContext ctx, StateEncoder stateEncoder, ResourceState resourceState) {
        super (resourceState, ctx.returnFields());
        this.stateEncoder = stateEncoder;
    }

    public ResourceState state() {
        return (ResourceState) object();
    }

    @Override
    public StateEncoder encoder() {
        return stateEncoder;
    }

    @Override
    public void encode() throws Exception {
        encodeState(state());
    }

    protected void encodeState(ResourceState resourceState) throws Exception {
        encoder().startResource(resourceState);
        encodeProperties(resourceState);
        encodeMembers(resourceState);
        encoder().endResource(resourceState);
    }

    protected void encodeMembers(ResourceState resourceState) throws Exception {
        if ( resourceState.members() != null && !resourceState.members().isEmpty() ) {
            encoder().startMembers();
            for (ResourceState memberState :resourceState.members()) {
                encodeValue( memberState );
            }
            encoder().endMembers();
        }
    }

    protected void encodeProperties( ResourceState resourceState ) throws Exception {
        Iterator<String> fields = resourceState.object().fieldNames();

        if (fields.hasNext()) {
            encoder().startProperties();

            do {
                String fieldName = fields.next();
                encodeProperty(fieldName, resourceState.object().get(fieldName));
            } while (fields.hasNext());

            encoder().endProperties();
        }
    }

    protected void encodeProperty(String propertyName, Object property) throws Exception{
        encoder().startProperty( propertyName ) ;
        encodeValue( property );
        encoder().endProperty( propertyName );
    }

    protected void encodeValue(Object value) throws Exception {
        if (value instanceof ResourceState) {
            encodeState( ( ResourceState ) value );
        }
        else if (value instanceof JsonNode) {
            JsonNode node = (JsonNode)value;
            if (node.isTextual()) {
                encoder().writeValue(node.asText());
            } else if (node.canConvertToInt()) {
                encoder().writeValue(node.intValue());
            } else if (node.isDouble()) {
                encoder().writeValue(node.doubleValue());
            } else if (node.canConvertToLong()) {
                encoder().writeValue(node.asLong());
            } else if (node.isBoolean()) {
                encoder().writeValue(node.asBoolean());
            } else {
                throw new NonEncodableValueException(value);
            }
        } else if (value instanceof Date ) {
            encoder().writeValue( ( Date ) value );
       //TODO: figure out when writing a link should be used....
//        } else if (value instanceof URI ) {
////        } else if (property instanceof ResourceState) {
//            encoder().writeLink((URI) property);
//        }
        } else if (value instanceof URI) {
            encoder().writeValue(((URI)value).getPath());
        } else if (value instanceof Map ) {
            encoder().writeValue( ( Map ) value );
        } else if (value instanceof Collection ) {
            encodeList((Collection)value);
        } else if (value == null) {
            encoder().writeNullValue();
        } else {
            throw new NonEncodableValueException(value);
        }
    }

    protected void encodeList(Collection list) throws Exception {
        encoder().startList();
        for (Object element : list) {
            if (element instanceof ResourceState) {

            }
            encodeValue( element );
        }
        encoder().endList();
    }

    @Override
    public void close() throws Exception {
        encoder().close();
    }
}
