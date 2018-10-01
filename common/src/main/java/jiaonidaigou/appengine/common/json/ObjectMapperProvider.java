package jiaonidaigou.appengine.common.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public class ObjectMapperProvider {
    public static ObjectMapper get() {
        return LazyHolder.MAPPER;
    }

    private static class LazyHolder {
        private static ObjectMapper MAPPER = new ObjectMapper()
                .registerModule(new JodaModule())
                .registerModule(new GuavaModule())
                .registerModule(new CookieJsonModule())
                .registerModule(new ProtobufJsonModule().registerAllKnownMessages())
                .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static String prettyToJson(Object object) {
        if (object == null) {
            return "null";
        }
        try {
            return get().writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "<error>: " + e.getMessage();
        }
    }

    public static String compactToJson(Object object) {
        if (object == null) {
            return "null";
        }
        try {
            return get().writer().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "<error>: " + e.getMessage();
        }
    }
}
