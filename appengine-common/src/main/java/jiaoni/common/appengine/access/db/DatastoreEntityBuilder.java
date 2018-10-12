package jiaoni.common.appengine.access.db;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import jiaoni.common.appengine.utils.TimestampUtils;
import jiaoni.common.json.ObjectMapperProvider;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import javax.annotation.Nullable;

public class DatastoreEntityBuilder {
    static final String COMMON_PROP_LAST_UPDATE = "_ts";

    private final Entity entity;

    DatastoreEntityBuilder(@Nullable final Key key, final String kind) {
        if (key != null) {
            this.entity = new Entity(key);
        } else if (StringUtils.isNotBlank(kind)) {
            this.entity = new Entity(kind);
        } else {
            throw new IllegalStateException();
        }
    }

    public DatastoreEntityBuilder indexedEnum(final String prop, final Enum<?> enumm) {
        return setProp(prop, enumm.name(), true);
    }

    public DatastoreEntityBuilder indexedString(final String prop, final String str) {
        return setProp(prop, StringUtils.trimToNull(str), true);
    }

    public DatastoreEntityBuilder indexedStringList(final String prop, final List<String> strings) {
        if (CollectionUtils.isNotEmpty(strings)) {
            return setProp(prop, strings, true);
        }
        return this;
    }

    public DatastoreEntityBuilder indexedBoolean(final String prop, final Boolean bool) {
        return setProp(prop, bool, true);
    }

    public DatastoreEntityBuilder unindexedString(final String prop, final String str) {
        return setProp(prop, str, false);
    }

    public DatastoreEntityBuilder unindexedJson(final String prop, final Object object) {
        String json;
        try {
            json = ObjectMapperProvider.get().writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return unindexedText(prop, json);
    }

    public DatastoreEntityBuilder unindexedText(final String prop, final String str) {
        if (StringUtils.isNotBlank(str)) {
            Text text = new Text(str);
            return setProp(prop, text, false);
        }
        return this;
    }

    public DatastoreEntityBuilder unindexedBytes(final String prop, final byte[] bytes) {
        return setProp(prop, ArrayUtils.isEmpty(bytes) ? null : new Blob(bytes), false);
    }

    public DatastoreEntityBuilder unindexedProto(final String prop, final Message message) {
        byte[] bytes = message == null ? null : message.toByteArray();
        return unindexedBytes(prop, bytes);
    }

    public DatastoreEntityBuilder indexedTimestamp(final String prop, final DateTime dateTime) {
        return setProp(prop, dateTime == null ? null : dateTime.toDateTime(), true);
    }

    public DatastoreEntityBuilder indexedLong(final String prop, final long value) {
        return setProp(prop, value, true);
    }

    public DatastoreEntityBuilder indexedInteger(final String prop, final int value) {
        return setProp(prop, value, true);
    }

    public DatastoreEntityBuilder unindexedInteger(final String prop, final int value) {
        return setProp(prop, value, false);
    }

    public DatastoreEntityBuilder indexedTimestamp(final String prop, final Timestamp timestamp) {
        return indexedTimestamp(prop, TimestampUtils.toJodaTime(timestamp));
    }

    public DatastoreEntityBuilder unindexedTimestamp(final String prop, final DateTime dateTime) {
        return setProp(prop, dateTime == null ? null : dateTime.toDate(), false);
    }

    public DatastoreEntityBuilder unindexedLastUpdatedTimestampAsNow() {
        return unindexedTimestamp(COMMON_PROP_LAST_UPDATE, DateTime.now(DateTimeZone.UTC));
    }

    public DatastoreEntityBuilder unindexedBoolean(final String prop, final Boolean bool) {
        return setProp(prop, bool, false);
    }

    public DatastoreEntityBuilder unindexedLong(final String prop, final Long val) {
        return setProp(prop, val, false);
    }

    public Entity build() {
        return entity;
    }

    private DatastoreEntityBuilder setProp(final String prop, final Object val, final boolean indexed) {
        if (val != null) {
            if (indexed) {
                entity.setIndexedProperty(prop, val);
            } else {
                entity.setUnindexedProperty(prop, val);
            }
        }
        return this;
    }
}
