/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.common.codec;

import java.io.ByteArrayOutputStream;

import io.liveoak.common.codec.driver.StateEncodingDriver;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.state.ResourceState;

/**
 * @author Bob McWhirter
 * @author <a href="http://community.jboss.org/people/kenfinni">Ken Finnigan</a>
 */
public class ResourceCodec {

    public ResourceCodec(Class<? extends StateEncoder> encoderClass, ResourceDecoder decoder) {
        this.encoderClass = encoderClass;
        this.decoder = decoder;
    }

    public boolean hasEncoder() {
        return this.encoderClass != null;
    }

    public boolean hasDecoder() {
        return this.decoder != null;
    }

    public byte [] encode(RequestContext ctx, ResourceState resourceState) throws Exception {
        StateEncoder encoder = this.encoderClass.newInstance();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.initialize(baos);
        StateEncodingDriver driver = new StateEncodingDriver(ctx, encoder, resourceState);
        driver.encode();
        driver.close();
        return baos.toByteArray();
    }

    public ResourceState decode(byte [] resource) throws Exception {
        return this.decoder.decode(resource);
    }

    private final Class<? extends StateEncoder> encoderClass;
    private final ResourceDecoder decoder;

}
