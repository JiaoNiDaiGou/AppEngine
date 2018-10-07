package jiaoni.common.model;

@FunctionalInterface
public interface UncheckedCallable<T> {
    T call();
}
