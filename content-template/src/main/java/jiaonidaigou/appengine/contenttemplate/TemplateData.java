package jiaonidaigou.appengine.contenttemplate;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

public class TemplateData {
    private final Map<String, Object> map;

    public TemplateData() {
        map = new HashMap<>();
    }

    public <T> TemplateData add(final String prop, final T val, final T defaultVal) {
        if (StringUtils.isNotBlank(prop)) {
            if (val != null) {
                map.put(prop, val);
            } else {
                map.put(prop, defaultVal);
            }
        }
        return this;
    }

    public <T> TemplateData add(final String prop, final T val) {
        if (StringUtils.isNotBlank(prop) && val != null) {
            map.put(prop, val);
        }
        return this;
    }

    public TemplateData addAsDate(final String prop, final DateTime time, final String defaultVal) {
        return add(prop, time == null ? null : time.toString("yyyy-MM-dd"), defaultVal);
    }

    public TemplateData addAsDateTime(final String prop, final DateTime time, final String defaultVal) {
        return add(prop, time == null ? null : time.toString("yyyy-MM-dd HH:mm:ss"), defaultVal);
    }

    public TemplateData addAsDateTime(final String prop, final DateTime time) {
        return add(prop, time == null ? null : time.toString("yyyy-MM-dd HH:mm:ss"), null);
    }

    public TemplateData addAsDateTime(final String prop, final long millis) {
        return addAsDateTime(prop, millis == 0 ? null : new DateTime(millis), null);
    }

    public TemplateData addAsDateTime(final String prop, final long millis, String defaultVal) {
        return addAsDateTime(prop, millis == 0 ? null : new DateTime(millis), defaultVal);
    }

    public Map<String, Object> build() {
        return map;
    }
}
