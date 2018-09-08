package jiaonidaigou.appengine.api.access.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.google.protobuf.Message;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.model.InternalRuntimeException;
import org.joda.time.DateTime;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

public class DatastoreEntityExtractor {
    private final Entity entity;

    private DatastoreEntityExtractor(final Entity entity) {
        this.entity = entity;
    }

    static DatastoreEntityExtractor of(final Entity entity) {
        checkNotNull(entity);
        return new DatastoreEntityExtractor(entity);
    }

    public String getKeyStringName() {
        return entity.getKey().getName();
    }

    public String getKeyLongId() {
        return String.valueOf(entity.getKey().getId());
    }

    public String getAsString(final String prop) {
        if (!entity.hasProperty(prop)) {
            return null;
        }
        return (String) entity.getProperty(prop);
    }

    Boolean getAsBoolean(final String prop) {
        if (!entity.hasProperty(prop)) {
            return false;
        }
        return (Boolean) entity.getProperty(prop);
    }

    Long getAsLong(final String prop) {
        if (!entity.hasProperty(prop)) {
            return null;
        }
        return (Long) entity.getProperty(prop);
    }

    DateTime getAsTimestamp(final String prop) {
        if (!entity.hasProperty(prop)) {
            return null;
        }
        return new DateTime((Date) entity.getProperty(prop));
    }

    @SuppressWarnings("unchecked")
    <T extends Message> T getAsProtobuf(final String prop, final Class<T> type) {
        if (!entity.hasProperty(prop)) {
            return null;
        }
        Blob blob = (Blob) entity.getProperty(prop);
        if (blob == null) {
            return null;
        }
        try {
            Method method = type.getMethod("parseFrom", byte[].class);
            return (T) method.invoke(type, (Object) blob.getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    DateTime getAsDateTime(final String prop) {
        if (!entity.hasProperty(prop)) {
            return null;
        }
        Date date = (Date) entity.getProperty(prop);
        return new DateTime(date);
    }

    <T> T getAsJson(final String prop,
                    final Class<T> type) {
        if (!entity.hasProperty(prop)) {
            return null;
        }
        Object obj = entity.getProperty(prop);
        String json;
        if (obj instanceof Text) {
            json = ((Text) obj).getValue();
        } else if (obj instanceof String) {
            json = (String) obj;
        } else {
            throw new IllegalStateException("Entity cannot be translate to JSON: " + obj.getClass().getName());
        }
        try {
            return ObjectMapperProvider.get().readValue(json, type);
        } catch (IOException e) {
            throw new InternalRuntimeException(e);
        }
    }

    <T> T getAsJson(final String prop,
                    final TypeReference<T> type) {
        String json = getAsText(prop);
        if (json == null) {
            return null;
        }
        try {
            return ObjectMapperProvider.get().readValue(json, type);
        } catch (IOException e) {
            throw new InternalRuntimeException(e);
        }
    }

    String getAsText(final String prop) {
        if (!entity.hasProperty(prop)) {
            return null;
        }
        Text text = (Text) entity.getProperty(prop);
        return text.getValue();
    }
}
