package jiaoni.common.appengine.guice;

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
import static jiaoni.common.utils.Preconditions2.checkNotBlank;

public class HK2toGuiceModule extends AbstractBinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(HK2toGuiceModule.class);

    private final String interfacesPackageName;
    private final Injector guiceInjector;

    public HK2toGuiceModule(final String interfacesPackageName,
                            final Injector guiceInjector) {
        this.interfacesPackageName = checkNotBlank(interfacesPackageName);
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
            LOGGER.info("Bind interface: {}", clazz.getName());
            bindInterface(clazz);
        }
    }

    private <T> void bindInterface(final Class<T> interfaceType) {
        bindFactory(new ServiceFactory<>(guiceInjector, interfaceType)).to(interfaceType);
    }

    @VisibleForTesting
    List<Class> findInterfaceClasses() {
        List<Class> toReturn = new ArrayList<>();

        Reflections reflections = new Reflections(interfacesPackageName);
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
