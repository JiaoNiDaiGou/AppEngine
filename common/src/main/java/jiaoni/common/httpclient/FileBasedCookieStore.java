package jiaoni.common.httpclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jiaoni.common.model.InternalIOException;
import jiaoni.common.utils.Environments;
import jiaoni.common.json.ObjectMapperProvider;
import org.apache.http.cookie.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class FileBasedCookieStore implements CookieDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileBasedCookieStore.class);

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperProvider.get();

    private static String cookieFile(final String appName) {
        return Environments.LOCAL_TEMP_DIR_ENDSLASH + appName + ".cookie.json";
    }

    @Override
    public void save(final String appName,
                     final List<Cookie> cookies) {
        String toWrite = cookieFile(appName);
        checkNotNull(toWrite);
        try {
            LOGGER.info("Save cookies to {}: {}", toWrite, cookies);
            OBJECT_MAPPER.writeValue(new File(toWrite), cookies);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    @Override
    public List<Cookie> load(final String appName) {
        String toRead = cookieFile(appName);
        checkNotNull(toRead);
        File file = new File(toRead);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            List<Cookie> toReturn = OBJECT_MAPPER.readValue(file, new TypeReference<List<Cookie>>() {
            });
            LOGGER.info("Load cookies from {}: {}", toRead, toReturn);
            return toReturn;
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }
}
