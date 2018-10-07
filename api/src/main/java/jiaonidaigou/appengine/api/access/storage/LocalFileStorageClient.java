package jiaonidaigou.appengine.api.access.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import jiaoni.common.model.InternalIOException;
import jiaoni.common.utils.Environments;
import jiaoni.common.utils.StringUtils2;
import org.joda.time.DateTime;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class LocalFileStorageClient implements StorageClient {
    @Override
    public boolean exists(String path) {
        return toFile(path).exists();
    }

    @Override
    public boolean delete(String path) {
        return toFile(path).delete();
    }

    @Override
    public Metadata getMetadata(String path) {
        File file = toFile(path);
        return Metadata.builder()
                .withLength(file.length())
                .withLastModified(new DateTime(file.lastModified()))
                .withPath(path)
                .build();
    }

    @Override
    public byte[] read(String path) {
        System.out.println("read: " + path);
        try (InputStream inputStream = new FileInputStream(toFile(path))) {
            return ByteStreams.toByteArray(inputStream);
        } catch (Exception e) {
            throw new InternalIOException(e);
        }
    }

    @Override
    public void write(String path, String mediaType, byte[] bytes) {
        System.out.println("write: " + path);
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(toFile(path)))) {
            outputStream.write(bytes);
        } catch (Exception e) {
            throw new InternalIOException(e);
        }
    }

    @Override
    public void copy(String fromPath, String toPath) {
        try {
            Files.copy(toFile(fromPath), toFile(toPath));
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    @Override
    public URL getSignedUploadUrl(String path, String mediaType, DateTime expiration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> listAll(String bucketPath) {
        File folder = toFile(bucketPath);
        String[] toReturn = folder.list();
        return toReturn == null ? ImmutableList.of() : Arrays.asList(toReturn);
    }

    @Override
    public URL getSignedDownloadUrl(String path, String mediaType, DateTime expiration) {
        throw new UnsupportedOperationException();
    }

    private File toFile(String path) {
        return new File(Environments.LOCAL_TEMP_DIR_ENDSLASH +
                StringUtils2.replaceNonCharTypesWith(path,
                        new StringUtils2.CharType[]{ StringUtils2.CharType.A2Z }, "_"));
    }
}
