package jiaoni.common.appengine.access.db;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.joda.time.DateTime;

class TimestampUtils {

    static Timestamp now() {
        return Timestamps.fromMillis(System.currentTimeMillis());
    }

    static Timestamp fromJodaTime(final DateTime dateTime) {
        return Timestamps.fromMillis(dateTime.getMillis());
    }

    static DateTime toJodaTime(final Timestamp timestamp) {
        return new DateTime(timestamp.getSeconds() * 1000);
    }
}
