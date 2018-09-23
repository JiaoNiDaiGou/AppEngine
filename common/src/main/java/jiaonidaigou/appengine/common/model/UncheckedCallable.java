package jiaonidaigou.appengine.common.model;

@FunctionalInterface
public interface UncheckedCallable<T> {
    T call();
}
