package jiaoni.common.appengine.guice;

import org.glassfish.hk2.api.AnnotationLiteral;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ FIELD, PARAMETER, METHOD })
public @interface Authenticators {
    class Impl extends AnnotationLiteral<Authenticators> implements Authenticators {
    }
}
