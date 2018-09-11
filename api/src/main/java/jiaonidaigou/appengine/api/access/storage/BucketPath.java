package jiaonidaigou.appengine.api.access.storage;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.api.client.util.Preconditions.checkArgument;
import static jiaonidaigou.appengine.common.utils.Preconditions2.checkNotBlank;

class BucketPath {
    private static final Logger LOGGER = LoggerFactory.getLogger(BucketPath.class);

    private static final String GS_SCHEME = "gs://";

    private final String scheme;
    private final String bucket;
    private final String object;

    /**
     * Path is in format of
     * gcs://bucket_a/bucket_b/file.ext
     * bucket is 'bucket_a/bucket_b'
     * object is 'file.ext'
     */
    static BucketPath of(final String path) {
        checkArgument(StringUtils.startsWithIgnoreCase(path, GS_SCHEME));
        String pathWithoutScheme = path.substring(GS_SCHEME.length());
        int firstSlash = StringUtils.indexOf(pathWithoutScheme, '/');
        String bucket = StringUtils.substring(pathWithoutScheme, 0, firstSlash);
        String object = StringUtils.substring(pathWithoutScheme, firstSlash + 1);
        BucketPath bucketPath = new BucketPath(GS_SCHEME, bucket, object);
        LOGGER.info("convert path {} to {}", path, bucketPath);
        return bucketPath;
    }

    private BucketPath(final String scheme, final String bucket, final String object) {
        this.scheme = checkNotBlank(scheme);
        this.bucket = checkNotBlank(bucket);
        this.object = checkNotBlank(object);
    }

    public String getScheme() {
        return scheme;
    }

    public String getBucket() {
        return bucket;
    }

    public String getObject() {
        return object;
    }

    public String fullPath() {
        return scheme + bucket + "/" + object;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("scheme", scheme)
                .add("bucket", bucket)
                .add("object", object)
                .toString();
    }
}
