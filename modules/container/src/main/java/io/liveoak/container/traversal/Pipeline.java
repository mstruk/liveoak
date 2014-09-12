package io.liveoak.container.traversal;

import io.liveoak.spi.RequestContext;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class Pipeline {

    public static final String PIPELINE = "Pipeline";

    private Processor head;
    private Processor current;

    public static Pipeline instance(RequestContext ctx) {
        return (Pipeline) ctx.requestAttributes().getAttribute(PIPELINE);
    }

    public Pipeline() {
    }

    public void associate(RequestContext ctx) {
        ctx.requestAttributes().setAttribute(PIPELINE, this);
    }

    public void head(Processor processor) {
        head = processor;
    }

    public void proceed() throws Exception {
        proceed(null);
    }

    public void proceed(Object obj) throws Exception {
        if (current == null) {
            current = head;
        } else {
            current = current.next();
        }
        if (current != null) {
            current.process(obj);
        } else {
            head = null; // pipeline run is complete
        }
    }

    public static abstract class Processor<T, V> {

        private Pipeline pipeline;
        private Processor<V, ?> next;
        private V result;

        public Processor(Pipeline pipeline) {
            this.pipeline = pipeline;
        }

        protected Pipeline pipeline() {
            return pipeline;
        }

        /**
         * Never call this method directly from another processor.
         * Use pipeline().proceed(o) instead.
         *
         * @param obj
         * @throws Exception
         */
        public abstract void process(T obj) throws Exception;

        public V result() {
            return result;
        }

        protected void result(V obj) {
            result = obj;
        }

        public Processor<V, ?> next() {
            return next;
        }

        public Processor<V, ?> next(Processor<V, ?> processor) {
            next = processor;
            return next;
        }
    }
}
