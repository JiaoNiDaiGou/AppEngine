package jiaoni.common.utils;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.joda.time.DateTime;
import org.joda.time.Duration;

public class TimestampUtils {
    private TimestampUtils() {
    }

    public static Timestamp now() {
        return Timestamps.fromMillis(System.currentTimeMillis());
    }

    public static Timestamp fromJodaTime(final DateTime dateTime) {
        return Timestamps.fromMillis(dateTime.getMillis());
    }

    public static DateTime toJodaTime(final Timestamp timestamp) {
        return new DateTime(timestamp.getSeconds() * 1000);
    }

    public static Duration diff(final DateTime a, final DateTime b) {
        return Duration.millis(Math.abs(a.getMillis() - b.getMillis()));
    }
}
