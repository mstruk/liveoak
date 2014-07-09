/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.common.codec.json;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.liveoak.common.codec.DefaultResourceState;
import io.liveoak.common.codec.ResourceDecoder;
import io.liveoak.spi.state.ResourceState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

/**
 * @author Bob McWhirter
 * @author Ken Finnigan
 */
public class JSONDecoder implements ResourceDecoder {

    public JSONDecoder() {
    }

    public JSONDecoder(boolean replaceProperties) {
        this.replaceProperties = replaceProperties;
    }

    @Override
    public ResourceState decode(ByteBuf resource) throws IOException {
        return decode(() -> factory().createParser(new ByteBufInputStream(resource)));
    }

    public ResourceState decode(File resource) throws IOException {
        return decode(() -> factory().configure(JsonParser.Feature.ALLOW_COMMENTS, true).createParser(resource));
    }

    private JsonFactory factory() {
        JsonFactory factory = new JsonFactory();
        factory.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        factory.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        return factory;
    }

    private ResourceState decode(Callable<JsonParser> parserCallable) throws IOException {
        JsonParser parser;
        try {
            parser = parserCallable.call();
        } catch (Exception e) {
            throw new IOException(e);
        }

        TreeNode treeNode = parser.readValueAsTree();
        ResourceState result = null;

        if (treeNode.isObject()) {
            result = new DefaultResourceState((ObjectNode) treeNode);
        }

        if (result == null) {
            result = new DefaultResourceState();
        }

        return result;
    }

    private boolean replaceProperties = false;
}
