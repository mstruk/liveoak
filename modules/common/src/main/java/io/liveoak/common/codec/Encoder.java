/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.common.codec;

import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

/**
 * @author Bob McWhirter
 */
public interface Encoder<Obj> extends AutoCloseable {

    void initialize(OutputStream out) throws Exception;

    void close() throws Exception;

    void startResource(Obj resource) throws Exception;

    void endResource(Obj resource) throws Exception;

    void writeLink(Obj link) throws Exception;

    void startProperties() throws Exception;

    void endProperties() throws Exception;

    void startProperty(String propertyName) throws Exception;

    void endProperty(String propertyName) throws Exception;

    void startMembers() throws Exception;

    void endMembers() throws Exception;

    void startList() throws Exception;

    void endList() throws Exception;

    void writeValue(String value) throws Exception;

    void writeValue(Integer value) throws Exception;

    void writeValue(Double value) throws Exception;

    void writeValue(Long value) throws Exception;

    void writeValue(Boolean value) throws Exception;

    void writeValue(Date value) throws Exception;

    void writeValue(Map value) throws Exception;

    void writeNullValue() throws Exception;

}
