package jiaoni.common.appengine.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jiaoni.common.json.ObjectMapperProvider;

import javax.ws.rs.ext.ContextResolver;

public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
    @Override
    public ObjectMapper getContext(Class<?> type) {
        return ObjectMapperProvider.get();
    }
}
