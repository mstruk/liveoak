package io.liveoak.common;

import java.nio.ByteBuffer;
import java.util.List;

import io.undertow.util.AttachmentKey;
import org.xnio.BufferAllocator;
import org.xnio.ByteBufferSlicePool;
import org.xnio.Pool;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class Constants {

    public static final int MAX_RESPONSE_SIZE = 100 * 1024;

    public static final int MAX_REQUEST_SIZE = 100 * 1024;

    public static final int BUFFERS_PER_REGION = 50;

    public static final String HTTP_SERVER_EXCHANGE = "HTTP_SERVER_EXCHANGE";

    public static final AttachmentKey<List<?>> OBJECT_BUFFER = AttachmentKey.create(List.class);

    // TODO - the problem here is one of thread model.
    // When you encode a Resource tree into a byte array you can either do it on I/O thread without blocking.
    // Or you can do it on a worker thread with blocking.
    // If you do it on an I/O thread you have to avoid pushing to outgoing channel - as it can block - therefore you have to do it
    // in-memory.
    // Another use case would be if there are interceptors wanting to intercept generated encoded bytes and modify them before
    // writing it to out. That should really be done in a 'stream wrapper' fashion, not by generating a full in-memory body
    public static final Pool<ByteBuffer> buffers = new ByteBufferSlicePool(BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR, MAX_RESPONSE_SIZE, MAX_RESPONSE_SIZE * BUFFERS_PER_REGION);

}
