package io.liveoak.spi.state;

import java.io.File;
import java.io.InputStream;

import io.liveoak.spi.MediaType;
import java.nio.ByteBuffer;
import io.netty.handler.codec.http.multipart.FileUpload;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public interface LazyResourceState extends ResourceState {

    long getContentLength();

    MediaType getContentType();

    boolean hasBigContent();

    File contentAsFile();

    InputStream contentAsStream();

    byte [] contentAsByteBuffer();

    //void fileUpload(FileUpload fileUpload);

    void content(byte [] content);
}
