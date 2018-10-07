package jiaonidaigou.appengine.api.access.db.core;

import com.google.appengine.api.memcache.ErrorHandler;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.Stats;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class DoNothingMemcache implements MemcacheService {
    @Override
    public void setNamespace(String s) {

    }

    @Override
    public Object get(Object o) {
        return null;
    }

    @Override
    public IdentifiableValue getIdentifiable(Object o) {
        return null;
    }

    @Override
    public <T> Map<T, IdentifiableValue> getIdentifiables(Collection<T> collection) {
        return null;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public <T> Map<T, Object> getAll(Collection<T> collection) {
        return null;
    }

    @Override
    public boolean put(Object o, Object o1, Expiration expiration, SetPolicy setPolicy) {
        return false;
    }

    @Override
    public void put(Object o, Object o1, Expiration expiration) {

    }

    @Override
    public void put(Object o, Object o1) {

    }

    @Override
    public <T> Set<T> putAll(Map<T, ?> map, Expiration expiration, SetPolicy setPolicy) {
        return null;
    }

    @Override
    public void putAll(Map<?, ?> map, Expiration expiration) {

    }

    @Override
    public void putAll(Map<?, ?> map) {

    }

    @Override
    public boolean putIfUntouched(Object o, IdentifiableValue identifiableValue, Object o1, Expiration expiration) {
        return false;
    }

    @Override
    public boolean putIfUntouched(Object o, IdentifiableValue identifiableValue, Object o1) {
        return false;
    }

    @Override
    public <T> Set<T> putIfUntouched(Map<T, CasValues> map) {
        return null;
    }

    @Override
    public <T> Set<T> putIfUntouched(Map<T, CasValues> map, Expiration expiration) {
        return null;
    }

    @Override
    public boolean delete(Object o) {
        return false;
    }

    @Override
    public boolean delete(Object o, long l) {
        return false;
    }

    @Override
    public <T> Set<T> deleteAll(Collection<T> collection) {
        return null;
    }

    @Override
    public <T> Set<T> deleteAll(Collection<T> collection, long l) {
        return null;
    }

    @Override
    public Long increment(Object o, long l) {
        return null;
    }

    @Override
    public Long increment(Object o, long l, Long aLong) {
        return null;
    }

    @Override
    public <T> Map<T, Long> incrementAll(Collection<T> collection, long l) {
        return null;
    }

    @Override
    public <T> Map<T, Long> incrementAll(Collection<T> collection, long l, Long aLong) {
        return null;
    }

    @Override
    public <T> Map<T, Long> incrementAll(Map<T, Long> map) {
        return null;
    }

    @Override
    public <T> Map<T, Long> incrementAll(Map<T, Long> map, Long aLong) {
        return null;
    }

    @Override
    public void clearAll() {

    }

    @Override
    public Stats getStatistics() {
        return null;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return null;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {

    }
}
