package jiaonidaigou.appengine.common.model;

public interface BiTransform<A, B> {
    B to(final A a);

    A from(final B b);
}
