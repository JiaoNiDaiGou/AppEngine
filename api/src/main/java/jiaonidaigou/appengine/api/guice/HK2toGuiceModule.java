package jiaonidaigou.appengine.api.guice;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Injector;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.jvnet.hk2.annotations.Service;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class HK2toGuiceModule extends AbstractBinder {
    private static final String INTERFACES_PACKAGE_NAME = "jiaonidaigou.appengine.api.interfaces";

    private static final Logger LOGGER = LoggerFactory.getLogger(HK2toGuiceModule.class);

    private Injector guiceInjector;

    public HK2toGuiceModule(final Injector guiceInjector) {
        this.guiceInjector = checkNotNull(guiceInjector);
    }

    @Override
    protected void configure() {
        bindInterfaces();
    }

    @SuppressWarnings("unchecked")
    private void bindInterfaces() {
        List<Class> interfaceClasses = findInterfaceClasses();
        for (Class clazz : interfaceClasses) {
            LOGGER.info("Bind interface: {}", clazz.getSimpleName());
            bindInterface(clazz);
        }
    }

    private <T> void bindInterface(final Class<T> interfaceType) {
        bindFactory(new ServiceFactory<>(guiceInjector, interfaceType)).to(interfaceType);
    }

    @VisibleForTesting
    static List<Class> findInterfaceClasses() {
        List<Class> toReturn = new ArrayList<>();

        Reflections reflections = new Reflections(INTERFACES_PACKAGE_NAME);
        for (Class clazz : reflections.getTypesAnnotatedWith(Service.class)) {
            int modifiers = clazz.getModifiers();
            if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
                continue;
            }
            toReturn.add(clazz);
        }

        return toReturn;
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
