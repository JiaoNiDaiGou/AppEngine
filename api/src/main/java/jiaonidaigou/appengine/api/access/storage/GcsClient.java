package jiaonidaigou.appengine.api.access.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;

@Singleton
public class GcsClient implements StorageClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClient.class);
    private static final String GS_SCHEME = "gs://";

    private final Storage storage;

    @Inject
    public GcsClient(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public boolean exists(final String path) {
        return storage.get(blobId(path)).exists();
    }

    @Override
    public Metadata getMetadata(final String path) {


        return null;
    }

    @Override
    public byte[] read(final String path) {
        return storage.readAllBytes(blobId(path));
    }

    @Override
    public void write(final String path, final String mediaType, byte[] bytes) {
        storage.create(
                BlobInfo.newBuilder(blobId(path))
                        .setContentType(mediaType)
                        .build(),
                bytes);
    }

    @Override
    public void copy(final String fromPath, final String toPath) {
        storage.copy(Storage.CopyRequest.of(blobId(fromPath), blobId(toPath)));
    }

    @Override
    public URL getSignedUploadUrl(final String path, final String mediaType, final DateTime expiration) {
        return storage.signUrl(
                BlobInfo.newBuilder(blobId(path)).setContentType(mediaType).build(),
                expiration.getMillis(),
                TimeUnit.MILLISECONDS,
                Storage.SignUrlOption.httpMethod(HttpMethod.POST)
//                Storage.SignUrlOption.withContentType()
        );
    }

    @Override
    public URL getSignedDownloadUrl(final String path, final String mediaType, final DateTime expiration) {
        return storage.signUrl(
                BlobInfo.newBuilder(blobId(path)).setContentType(mediaType).build(),
                expiration.getMillis(),
                TimeUnit.MILLISECONDS,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET)
//                Storage.SignUrlOption.withContentType()
        );
    }

    /**
     * Path is in format of
     * gcs://bucket_a/bucket_b/file.ext
     * bucket is 'bucket_a/bucket_b'
     * object is 'file.ext'
     */
    private static BlobId blobId(final String path) {
        checkArgument(StringUtils.startsWithIgnoreCase(path, GS_SCHEME));
        String pathWithoutScheme = path.substring(GS_SCHEME.length());
        int firstSlash = StringUtils.indexOf(pathWithoutScheme, '/');
        String bucket = StringUtils.substring(pathWithoutScheme, 0, firstSlash);
        String object = StringUtils.substring(pathWithoutScheme, firstSlash + 1);
        BlobId blobId = BlobId.of(bucket, object);
        LOGGER.info("convert path {} to {}", path, blobId);
        return blobId;
    }
}
