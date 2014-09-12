/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.common.codec;

import io.liveoak.spi.MediaType;

/**
 * @author Bob McWhirter
 */
public class EncodingResult {

    public EncodingResult(MediaType mediaType, byte [] encoded) {
        this.mediaType = mediaType;
        this.encoded = encoded;
    }

    public MediaType mediaType() {
        return this.mediaType;
    }

    public byte [] encoded() {
        return this.encoded;
    }

    private MediaType mediaType;
    private byte [] encoded;
}
