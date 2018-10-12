package jiaoni.common.appengine.access.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import jiaoni.common.json.ObjectMapperProvider;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static jiaoni.common.appengine.access.db.DatastoreEntityBuilder.COMMON_PROP_LAST_UPDATE;

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

    public Boolean getAsBoolean(final String prop) {
        if (!entity.hasProperty(prop)) {
            return false;
        }
        return (Boolean) entity.getProperty(prop);
    }

    public List<String> getAsStringList(final String prop) {
        if (!entity.hasProperty(prop)) {
            return ImmutableList.of();
        }
        return (List<String>) entity.getProperty(prop);
    }

    public Long getAsLong(final String prop) {
        if (!entity.hasProperty(prop)) {
            return null;
        }
        return (Long) entity.getProperty(prop);
    }

    public Integer getAsInteger(final String prop) {
        if (!entity.hasProperty(prop)) {
            return null;
        }
        return (Integer) entity.getProperty(prop);
    }

    public DateTime getAsTimestamp(final String prop) {
        if (!entity.hasProperty(prop)) {
            return null;
        }
        return new DateTime((Date) entity.getProperty(prop));
    }

    @SuppressWarnings("unchecked")
    public <T extends Message> T getAsProtobuf(final String prop, final Parser<T> parser) {
        if (!entity.hasProperty(prop)) {
            return null;
        }
        Blob blob = (Blob) entity.getProperty(prop);
        if (blob == null) {
            return null;
        }
        try {
            return parser.parseFrom(blob.getBytes());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    public DateTime getAsDateTime(final String prop) {
        if (!entity.hasProperty(prop)) {
            return null;
        }
        Date date = (Date) entity.getProperty(prop);
        return new DateTime(date);
    }

    public <T> T getAsJson(final String prop,
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
            throw new RuntimeException(e);
        }
    }

    public <T> T getAsJson(final String prop,
                           final TypeReference<T> type) {
        String json = getAsText(prop);
        if (json == null) {
            return null;
        }
        try {
            return ObjectMapperProvider.get().readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getAsText(final String prop) {
        if (!entity.hasProperty(prop)) {
            return null;
        }
        Text text = (Text) entity.getProperty(prop);
        return text.getValue();
    }

    public DateTime getLastUpdatedTimestamp() {
        return getAsDateTime(COMMON_PROP_LAST_UPDATE);
    }
}
