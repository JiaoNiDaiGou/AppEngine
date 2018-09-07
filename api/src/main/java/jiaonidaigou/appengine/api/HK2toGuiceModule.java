package jiaonidaigou.appengine.api;

import com.google.inject.Injector;
import jiaonidaigou.appengine.api.interfaces.Ping;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import static com.google.common.base.Preconditions.checkNotNull;

public class HK2toGuiceModule extends AbstractBinder {
    private Injector guiceInjector;

    public HK2toGuiceModule(final Injector guiceInjector) {
        this.guiceInjector = checkNotNull(guiceInjector);
    }

    @Override
    protected void configure() {
        bindFactory(new ServiceFactory<>(guiceInjector, Ping.class)).to(Ping.class);
    }

    private static class ServiceFactory<T> implements Factory<T> {

        private final Injector guiceInjector;

        private final Class<T> serviceClass;

        ServiceFactory(Injector guiceInjector, Class<T> serviceClass) {

            this.guiceInjector = guiceInjector;
            this.serviceClass = serviceClass;
        }

        @Override
        public T provide() {
            return guiceInjector.getInstance(serviceClass);
        }

        @Override
        public void dispose(T versionResource) {
        }
    }
}
