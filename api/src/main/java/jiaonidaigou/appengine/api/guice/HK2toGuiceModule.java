package jiaonidaigou.appengine.api.guice;

import com.google.inject.Injector;
import jiaonidaigou.appengine.api.interfaces.CronInterface;
import jiaonidaigou.appengine.api.interfaces.MediaInterface;
import jiaonidaigou.appengine.api.interfaces.PingInterface;
import jiaonidaigou.appengine.api.interfaces.ShippingOrderInterface;
import jiaonidaigou.appengine.api.interfaces.TaskQueueInterface;
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
        bindInterface(CronInterface.class);
        bindInterface(MediaInterface.class);
        bindInterface(PingInterface.class);
        bindInterface(ShippingOrderInterface.class);
        bindInterface(TaskQueueInterface.class);
    }

    private <T> void bindInterface(final Class<T> interfaceType) {
        bindFactory(new ServiceFactory<>(guiceInjector, interfaceType)).to(interfaceType);
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
