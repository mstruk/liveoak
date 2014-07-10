/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.common.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.liveoak.spi.state.ResourceState;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Bob McWhirter
 * @author Ken Finnigan
 */
public class DefaultResourceState implements ResourceState {

    public DefaultResourceState() {
        this.objectNode = JsonNodeFactory.instance.objectNode();
    }

    public DefaultResourceState(String id) {
        this();
        this.id(id);
    }

    public DefaultResourceState(ObjectNode objectNode) {
        this.objectNode = objectNode;
    }

    @Override
    public String id() {
        return this.objectNode.get("id").asText();
    }

    @Override
    public void id(String id) {
        this.objectNode.put("id", id);
    }

    @Override
    public URI uri() {
        try {
            return new URI(this.objectNode.get("self").get("href").asText());
        } catch (NullPointerException npe) {
            // NPE covers "self" or "href" being null JsonNodes
            return null;
        } catch (URISyntaxException e) {
            //TODO Proper logging
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void uri(URI uri) {
        JsonNode selfNode = this.objectNode.get("self");
        if (selfNode == null) {
            selfNode = JsonNodeFactory.instance.objectNode();
        }

        if (selfNode.isObject()) {
            ((ObjectNode)selfNode).put("href", uri.toString());
        } else {
            //TODO Log warning about non ObjectNode
        }
    }

    @Override
    public JsonNode getProperty(String name) {
        return this.objectNode.get(name);
    }

    @Override
    public JsonNode removeProperty(String name) {
        return this.objectNode.remove(name);
    }

    @Override
    public void putProperty(String name, Object value) {
        if (value instanceof ResourceState) {
            this.objectNode.put(name, ((ResourceState)value).object());
        } else if (value instanceof BigDecimal) {
            this.objectNode.put(name, (BigDecimal)value);
        } else if (value instanceof Long) {
            this.objectNode.put(name, (Long)value);
        } else if (value instanceof Float) {
            this.objectNode.put(name, (Float)value);
        } else if (value instanceof Double) {
            this.objectNode.put(name, (Double)value);
        } else if (value instanceof Short) {
            this.objectNode.put(name, (Short)value);
        } else if (value instanceof Integer) {
            this.objectNode.put(name, (Integer)value);
        } else if (value instanceof String) {
            this.objectNode.put(name, (String)value);
        } else if (value instanceof Boolean) {
            this.objectNode.put(name, (Boolean)value);
        }
    }

    @Override
    public ObjectNode object() {
        return this.objectNode;
    }

    @Override
    public void object(ObjectNode objectNode) {
        this.objectNode = objectNode;
    }

    @Override
    public void addMember(ResourceState member) {
        JsonNode membersNode = this.objectNode.get("_members");
        if (membersNode == null) {
            membersNode = JsonNodeFactory.instance.arrayNode();
            this.objectNode.put("_members", membersNode);
        }

        ((ArrayNode)membersNode).add(member.object());
    }

    @Override
    public List<ResourceState> members() {
        JsonNode membersNode = this.objectNode.get("_members");
        if (membersNode != null && membersNode.isArray()) {
            List<ResourceState> members = new ArrayList<>();
            membersNode.elements().forEachRemaining(e -> members.add(new DefaultResourceState((ObjectNode)e)));
            return members;
        }
        return Collections.EMPTY_LIST;
    }

    public String toString() {
        return "[DefaultResourceState: id=" + this.id() + "; uri=" + this.uri() + "; properties=" + this.objectNode.fieldNames() + "; members=" + this.objectNode.get("_members") + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultResourceState that = (DefaultResourceState) o;

        if (objectNode != null ? !objectNode.equals(that.objectNode) : that.objectNode != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return objectNode != null ? objectNode.hashCode() : 0;
    }

    private ObjectNode objectNode;
}
