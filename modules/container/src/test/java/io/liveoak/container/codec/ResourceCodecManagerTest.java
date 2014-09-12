/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container.codec;

import io.liveoak.common.DefaultMediaTypeMatcher;
import io.liveoak.common.DefaultResourceResponse;
import io.liveoak.common.codec.DefaultResourceState;
import io.liveoak.common.codec.ResourceCodec;
import io.liveoak.common.codec.ResourceCodecManager;
import io.liveoak.common.codec.UnsupportedMediaTypeException;
import io.liveoak.container.InMemoryObjectResource;
import io.liveoak.common.codec.html.HTMLEncoder;
import io.liveoak.common.codec.json.JSONDecoder;
import io.liveoak.spi.MediaType;
import io.liveoak.spi.ResourceResponse;
import org.junit.Test;

/**
 * @author <a href="http://community.jboss.org/people/kenfinni">Ken Finnigan</a>
 */
public class ResourceCodecManagerTest {

    @Test(expected = UnsupportedMediaTypeException.class)
    public void nullDecoderTest() throws Exception {
        ResourceCodecManager manager = new ResourceCodecManager();
        manager.registerResourceCodec(MediaType.HTML, new ResourceCodec(HTMLEncoder.class, null));

        MediaType contentType = new MediaType("text/html");
        manager.decode(contentType, "<html></html>".getBytes());
    }

    @Test(expected = UnsupportedMediaTypeException.class)
    public void nullEncoderTest() throws Exception {
        ResourceCodecManager manager = new ResourceCodecManager();
        manager.registerResourceCodec(MediaType.JSON, new ResourceCodec(null, new JSONDecoder()));

        DefaultMediaTypeMatcher mediaTypeMatcher = new DefaultMediaTypeMatcher("application/json", "json");
        DefaultResourceState state = new DefaultResourceState();
        InMemoryObjectResource resource = new InMemoryObjectResource(null, "gary", state);

        DefaultResourceResponse resourceResponse = new DefaultResourceResponse(null, ResourceResponse.ResponseType.READ, resource);
        resourceResponse.setState( state );
        manager.encode(null, mediaTypeMatcher, resourceResponse);
    }
}
