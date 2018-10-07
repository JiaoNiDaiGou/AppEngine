package jiaoni.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Serialize/Deserialize protobuf objects to json.
 */
public class ProtobufJsonModule extends SimpleModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtobufJsonModule.class);
    private static final String[] KNOWN_PROTO_PACKAGES = {
            "jiaonidaigou.appengine.wiremodel"
    };

    @SuppressWarnings("unchecked")
    public <T extends Message> ProtobufJsonModule register(final T defaultInstance) {
        Class<T> type = (Class<T>) defaultInstance.getClass();
        addSerializer(type, new ProtobufSerializer<>());
        addDeserializer(type, new ProtobufDeserializer<>(defaultInstance));
        return this;
    }

    public ProtobufJsonModule register(final List<? extends Message> defaultInstances) {
        for (Message message : defaultInstances) {
            register(message);
        }
        return this;
    }

    public ProtobufJsonModule registerAllKnownMessages() {
        return register(loadAllMessageDefaultInstances());
    }

    public static class ProtobufSerializer<T extends Message> extends JsonSerializer<T> {
        private final JsonFormat.Printer printer;

        private ProtobufSerializer() {
            this.printer = JsonFormat.printer();
        }

        @Override
        public void serialize(T value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException, JsonProcessingException {
            String raw = printer.print(value);

            // Remove beginning '{' and end '}', since we will writeStart/EndObject around it.
            raw = raw.substring(1, raw.length() - 1);

            gen.writeStartObject();
            gen.writeRaw(raw);
            gen.writeEndObject();
        }
    }

    public static class ProtobufDeserializer<T extends Message> extends JsonDeserializer<T> {
        private final JsonFormat.Parser parser;
        private final T defaultInstance;

        private ProtobufDeserializer(final T defaultInstance) {
            this.defaultInstance = checkNotNull(defaultInstance);
            this.parser = JsonFormat.parser();
        }

        @Override
        @SuppressWarnings("unchecked")
        public T deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            String raw = p.getCodec().readTree(p).toString();
            Message.Builder builder = defaultInstance.toBuilder();
            parser.merge(raw, builder);
            return (T) builder.build();
        }
    }

    private static List<? extends Message> loadAllMessageDefaultInstances() {
        try {
            List<Message> toReturn = new ArrayList<>();
            for (String packageName : KNOWN_PROTO_PACKAGES) {
                LOGGER.info("register ProtobufJsonModule for package {}", packageName);

                Reflections reflections = new Reflections(packageName);
                for (Class<? extends Message> clazz : reflections.getSubTypesOf(Message.class)) {
                    int modifiers = clazz.getModifiers();
                    if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers) || !Modifier.isPublic(modifiers)) {
                        continue;
                    }
                    Method method = clazz.getMethod("getDefaultInstance");
                    Message instance = (Message) method.invoke(clazz);
                    toReturn.add(instance);
                }
            }
            return toReturn;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
