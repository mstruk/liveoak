/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container.traversal;

import java.util.List;

import io.liveoak.common.DefaultResourceErrorResponse;
import io.liveoak.common.DefaultResourceResponse;
import io.liveoak.container.Dispatcher;
import io.liveoak.spi.ResourceErrorResponse;
import io.liveoak.spi.ResourceRequest;
import io.liveoak.spi.ResourceResponse;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.spi.resource.async.Responder;
import org.jboss.logging.Logger;

/**
 * @author Bob McWhirter
 */
public class BaseResponder implements Responder {

    private static Logger log = Logger.getLogger(BaseResponder.class);

    public BaseResponder(Dispatcher dispatcher, ResourceRequest inReplyTo, List<Object> out) {
        this.dispatcher = dispatcher;
        this.inReplyTo = inReplyTo;
        this.out = out;
    }

    BaseResponder createBaseResponder() {
        return new BaseResponder(this.dispatcher, this.inReplyTo, this.out);
    }

    ResourceRequest inReplyTo() {
        return this.inReplyTo;
    }

    @Override
    public void resourceRead(Resource resource) {
        out.add(new DefaultResourceResponse(this.inReplyTo, ResourceResponse.ResponseType.READ, resource));
        resumeRead();
    }

    @Override
    public void resourceCreated(Resource resource) {
        out.add(new DefaultResourceResponse(this.inReplyTo, ResourceResponse.ResponseType.CREATED, resource));
        resumeRead();
    }

    @Override
    public void resourceDeleted(Resource resource) {
        out.add(new DefaultResourceResponse(this.inReplyTo, ResourceResponse.ResponseType.DELETED, resource));
        resumeRead();
    }

    @Override
    public void resourceUpdated(Resource resource) {
        out.add(new DefaultResourceResponse(this.inReplyTo, ResourceResponse.ResponseType.UPDATED, resource));
        resumeRead();
    }

    @Override
    public void createNotSupported(Resource resource) {
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, ResourceErrorResponse.ErrorType.CREATE_NOT_SUPPORTED));
        resumeRead();
    }

    @Override
    public void readNotSupported(Resource resource) {
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, ResourceErrorResponse.ErrorType.READ_NOT_SUPPORTED));
        resumeRead();
    }

    @Override
    public void updateNotSupported(Resource resource) {
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, ResourceErrorResponse.ErrorType.UPDATE_NOT_SUPPORTED));
        resumeRead();
    }

    @Override
    public void deleteNotSupported(Resource resource) {
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, ResourceErrorResponse.ErrorType.DELETE_NOT_SUPPORTED));
        resumeRead();
    }

    @Override
    public void noSuchResource(String id) {
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, ResourceErrorResponse.ErrorType.NO_SUCH_RESOURCE));
        resumeRead();
    }

    @Override
    public void resourceAlreadyExists(String id) {
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, ResourceErrorResponse.ErrorType.RESOURCE_ALREADY_EXISTS));
        resumeRead();
    }

    @Override
    public void internalError(String message) {
        log.error(message, new RuntimeException("Stack trace: "));
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, ResourceErrorResponse.ErrorType.INTERNAL_ERROR, message));
        resumeRead();
    }

    @Override
    public void internalError(Throwable cause) {
        log.error("Internal error: ", cause);
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, ResourceErrorResponse.ErrorType.INTERNAL_ERROR, cause));
        resumeRead();
    }

    @Override
    public void invalidRequest(String message) {
        log.debug(message, new RuntimeException("Stack trace: "));
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, ResourceErrorResponse.ErrorType.NOT_ACCEPTABLE, message));
        resumeRead();
    }

    @Override
    public void invalidRequest(Throwable cause) {
        log.debug("Invalid request: ", cause);
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, ResourceErrorResponse.ErrorType.NOT_ACCEPTABLE, cause));
        resumeRead();
    }

    @Override
    public void invalidRequest(String message, Throwable cause) {
        log.debug(message, cause);
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, ResourceErrorResponse.ErrorType.NOT_ACCEPTABLE, cause));
        resumeRead();
    }

    @Override
    public void error(ResourceErrorResponse.ErrorType errorType) {
        log.debug("error(): " + errorType);
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, errorType));
        resumeRead();
    }

    @Override
    public void error(ResourceErrorResponse.ErrorType errorType, String message) {
        log.debug("error(): " + errorType + ", message: " + message);
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, errorType, message));
        resumeRead();
    }

    @Override
    public void error(ResourceErrorResponse.ErrorType errorType, String message, Throwable cause) {
        log.debug("[IGNORED] error(): " + errorType + ", message: " + message + ", cause: ", cause);
        out.add(new DefaultResourceErrorResponse(this.inReplyTo, errorType, message, cause));
        resumeRead();
    }

    protected void resumeRead() {
        // noop
    }

    public List<Object> output() {
        return out;
    }

    private final Dispatcher dispatcher;
    private final ResourceRequest inReplyTo;
    private final List<Object> out;
}
