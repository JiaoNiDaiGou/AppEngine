package jiaonidaigou.appengine.common.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class JsonBytesBiTransform<T> implements BiTransform<T, byte[]> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonBytesBiTransform.class);

    private final Class<T> type;

    public JsonBytesBiTransform(final Class<T> type) {
        this.type = checkNotNull(type);
    }

    @Override
    public byte[] to(T t) {
        if (t == null) {
            return null;
        }
        try {
            return ObjectMapperProvider.get().writeValueAsBytes(t);
        } catch (JsonProcessingException e) {
            LOGGER.error("failed to transform to bytes. {}", t, e);
            return null;
        }
    }

    @Override
    public T from(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return ObjectMapperProvider.get().readValue(bytes, type);
        } catch (IOException e) {
            LOGGER.error("failed to transfrom from bytes.", e);
            return null;
        }
    }
}
