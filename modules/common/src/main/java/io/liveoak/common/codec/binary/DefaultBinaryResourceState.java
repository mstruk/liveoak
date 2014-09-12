/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.common.codec.binary;

import io.liveoak.common.codec.DefaultResourceState;
import io.liveoak.spi.MediaType;
import io.liveoak.spi.state.BinaryResourceState;

/**
 * @author <a href="http://community.jboss.org/people/kenfinni">Ken Finnigan</a>
 */
public class DefaultBinaryResourceState extends DefaultResourceState implements BinaryResourceState {

    byte [] buffer;

    public DefaultBinaryResourceState() {
    }

    public DefaultBinaryResourceState(byte [] buffer) {
        this.buffer = buffer;
    }

    @Override
    public String getMimeType() {
        return MediaType.OCTET_STREAM.toString();
    }

    @Override
    public byte [] getBuffer() {
        return buffer;
    }
}
