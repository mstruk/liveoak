package io.liveoak.container;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public interface DispatcherFactory<T> {

    Dispatcher createDispatcher(T delegate);
}
