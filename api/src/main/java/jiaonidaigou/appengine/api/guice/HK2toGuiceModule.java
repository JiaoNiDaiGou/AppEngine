package jiaonidaigou.appengine.api.guice;

import com.google.inject.Injector;
import com.google.protobuf.Message;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;

import static com.google.common.base.Preconditions.checkNotNull;

public class HK2toGuiceModule extends AbstractBinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(HK2toGuiceModule.class);
    private static final String INTERFACES_PACKAGE_NAME = "jiaonidaigou.appengine.api.interfaces";

    private Injector guiceInjector;

    public HK2toGuiceModule(final Injector guiceInjector) {
        this.guiceInjector = checkNotNull(guiceInjector);
    }

    @Override
    protected void configure() {
        bindInterfaces();
    }

    private void bindInterfaces() {
        Reflections reflections = new Reflections();
        for (Class<? extends Message> clazz : reflections.getSubTypesOf(Message.class)) {
            int modifiers = clazz.getModifiers();
            if (Modifier.isAbstract(modifiers)
                    || Modifier.isInterface(modifiers)
                    || !Modifier.isPublic(modifiers)
                    || !clazz.getSimpleName().endsWith("Interface")) {
                continue;
            }
            LOGGER.info("Bind interface {}", clazz.getName());
            bindInterface(clazz);
        }
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
