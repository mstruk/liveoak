/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.spi.state;

import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Opaque state of a resource.
 *
 * <p>State objects are used to instill new state into a server-side resource.</p>
 *
 * @author Bob McWhirter
 * @author Ken Finnigan
 */
public interface ResourceState {

    /**
     * Retrieve the ID of the resource.
     *
     * @return The ID of the resource.
     */
    String id();

    /**
     * Set the ID of the resource.
     *
     * @param id The ID of the resource.
     */
    void id(String id);

    URI uri();

    void uri(URI uri);

    /**
     * Retrieve the property value.
     *
     * @param name The property name.
     * @return The {@link com.fasterxml.jackson.databind.JsonNode} representing the property.
     */
    JsonNode getProperty(String name);

    JsonNode removeProperty(String name);

    void putProperty(String name, Object value);

    ObjectNode object();

    void object(ObjectNode jsonNode);

    List<ResourceState> members();

    void addMember(ResourceState member);
}
