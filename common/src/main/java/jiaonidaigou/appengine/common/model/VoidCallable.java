package jiaonidaigou.appengine.common.model;

@FunctionalInterface
public interface VoidCallable {
    void call() throws Exception;
}
