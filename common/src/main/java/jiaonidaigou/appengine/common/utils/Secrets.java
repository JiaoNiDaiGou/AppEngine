package jiaonidaigou.appengine.common.utils;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.common.model.InternalIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class Secrets {
    private static final Logger LOGGER = LoggerFactory.getLogger(Secrets.class);

    private static final String SECRETS_RESOURCE_PATH = "secrets/";

    private final String value;

    private Secrets(final String name) {
        LOGGER.info("Loading secrets for {}.", name);
        try (Reader reader = new InputStreamReader(
                Resources.getResource(SECRETS_RESOURCE_PATH + name).openStream(), Charsets.UTF_8)) {
            this.value = CharStreams.toString(reader);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    public static Secrets of(final String name) {
        return new Secrets(name);
    }

    public String getAsString() {
        return value;
    }

    public <T> T getAsJson(final Class<T> type) {
        try {
            return ObjectMapperProvider.get()
                    .readValue(value, type);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }
}