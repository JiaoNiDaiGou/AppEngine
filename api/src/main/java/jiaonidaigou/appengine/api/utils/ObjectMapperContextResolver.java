package jiaonidaigou.appengine.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;

import javax.ws.rs.ext.ContextResolver;

public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
    @Override
    public ObjectMapper getContext(Class<?> type) {
        return ObjectMapperProvider.get();
    }
}