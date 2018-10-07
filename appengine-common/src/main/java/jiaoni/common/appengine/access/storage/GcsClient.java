package jiaoni.common.appengine.access.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class GcsClient implements StorageClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClient.class);
    private static final String GS_SCHEME = "gs://";

    private final Storage storage;

    public GcsClient(final Storage storage) {
        this.storage = storage;
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

    @Override
    public boolean exists(final String path) {
        Blob blob = storage.get(blobId(path));
        return blob != null && blob.exists();
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
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT)
        );
    }

    @Override
    public URL getSignedDownloadUrl(final String path, final String mediaType, final DateTime expiration) {
        return storage.signUrl(
                BlobInfo.newBuilder(blobId(path)).setContentType(mediaType).build(),
                expiration.getMillis(),
                TimeUnit.MILLISECONDS,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET)
        );
    }

    @Override
    public List<String> listAll(final String bucketPath) {
        BlobId blobId = blobId(bucketPath);
        return Streams.stream(
                storage.list(blobId.getBucket(),
                        Storage.BlobListOption.prefix(blobId.getName()))
                        .iterateAll())
                .filter(t -> t.getName().endsWith(".json"))
                .map(t -> "gs://" + t.getBucket() + "/" + t.getName())
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(final String path) {
        return storage.delete(blobId(path));
    }
}
