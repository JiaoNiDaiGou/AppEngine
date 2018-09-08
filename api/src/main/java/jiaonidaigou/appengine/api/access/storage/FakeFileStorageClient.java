package jiaonidaigou.appengine.api.access.storage;

import com.google.common.net.MediaType;
import jiaonidaigou.appengine.common.model.InternalIOException;
import jiaonidaigou.appengine.common.utils.Environments;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class FakeFileStorageClient implements StorageClient {
    @Override
    public boolean exists(String path) {
        return toFile(path).exists();
    }

    @Override
    public Metadata getMetadata(String path) {
        File file = toFile(path);
        if (!file.exists()) {
            return null;
        }
        return Metadata.builder()
                .withPath(path)
                .withLastModified(new DateTime(file.lastModified()))
                .withLength(file.length())
                .build();
    }

    @Override
    public InputStream inputStream(String path) {
        try {
            return new FileInputStream(toFile(path));
        } catch (FileNotFoundException e) {
            throw new InternalIOException(e);
        }
    }

    @Override
    public OutputStream outputStream(String path, final MediaType mediaType) {
        try {
            return new FileOutputStream(toFile(path));
        } catch (FileNotFoundException e) {
            throw new InternalIOException(e);
        }
    }

    @Override
    public void copy(String fromPath, String toPath) {
        try {
            Files.copy(toFile(fromPath).toPath(), toFile(toPath).toPath());
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    @Override
    public String getSignedUploadUrl(String path, MediaType mediaType, DateTime expiration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSignedDownloadUrl(String path, MediaType mediaType, DateTime expiration) {
        throw new UnsupportedOperationException();
    }

    private static File toFile(String path) {
        return new File(Environments.LOCAL_TEMP_DIR_ENDSLASH + "fakegcs/" + path);
    }
}
