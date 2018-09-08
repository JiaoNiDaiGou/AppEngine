package jiaonidaigou.appengine.api.access.storage;

import org.apache.commons.lang3.StringUtils;

import static com.google.api.client.util.Preconditions.checkArgument;
import static jiaonidaigou.appengine.common.utils.Preconditions2.checkNotBlank;

class BucketPath {
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
        int lastSlash = StringUtils.lastIndexOf(path, '/');
        String bucket = StringUtils.substring(path, GS_SCHEME.length(), lastSlash);
        String object = StringUtils.substring(path, lastSlash + 1);
        return new BucketPath(GS_SCHEME, bucket, object);
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
}
