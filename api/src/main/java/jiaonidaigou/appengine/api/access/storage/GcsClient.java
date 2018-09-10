package jiaonidaigou.appengine.api.access.storage;

import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.StorageObject;
import jiaonidaigou.appengine.common.model.InternalIOException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
public class GcsClient implements StorageClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClient.class);

    /**
     * Used below to determine the size of chucks to read in. Should be > 1kb and < 10MB
     */
    private static final int BUFFER_SIZE = 2 * 1024 * 1024;

    private static final String BASE_URL = "https://storage.googleapis.com";

    private final Storage storage;

    @Inject
    public GcsClient(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public boolean exists(String path) {
        return getMetadata(path) != null;
    }

    @Override
    public Metadata getMetadata(final String path) {
        BucketPath bucketPath = BucketPath.of(path);
        StorageObject obj;
        try {
            obj = storage.objects()
                    .get(bucketPath.getBucket(), bucketPath.getObject())
                    .execute();
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
        if (obj == null) {
            return null;
        }
        return Metadata.builder()
                .withPath(path)
                .withLastModified(new DateTime(obj.getUpdated().getValue()))
                .withLength(obj.getSize().longValue())
                .build();
    }

    @Override
    public byte[] read(String path) {
        return new byte[0];
    }

    @Override
    public void write(String path, byte[] bytes) {

    }

    @Override
    public void copy(String fromPath, String toPath) {

    }

    @Override
    public String getSignedUploadUrl(String path, MediaType mediaType, DateTime expiration) {
        return null;
    }

    @Override
    public String getSignedDownloadUrl(String path, MediaType mediaType, DateTime expiration) {
        return null;
    }
//
//    /**
//     * Path is in format of
//     * gcs://bucket_a/bucket_b/file.ext
//     * bucket is 'bucket_a/bucket_b'
//     * object is 'file.ext'
//     */
//    private static GcsFilename toGcsFilename(final String path) {
//        checkArgument(StringUtils.startsWithIgnoreCase(path, "gs://"), path + " is not a valid GCS path.");
//        int lastSlash = StringUtils.lastIndexOf(path, '/');
//        String bucket = StringUtils.substring(path, "gs://".length(), lastSlash);
//        String object = StringUtils.substring(path, lastSlash + 1);
//        return new GcsFilename(bucket, object);
//    }
//
//    @Override
//    public boolean exists(final String path) {
//        meterOn(this.getClass());
//        boolean toReturn;
//        try {
//            toReturn = gcsService.getMetadata(toGcsFilename(path)) != null;
//        } catch (IOException e) {
//            throw new InternalIOException(e);
//        }
//        meterOff();
//        return toReturn;
//    }
//
//    @Override
//    public Metadata getMetadata(final String path) {
//        meterOn(this.getClass());
//        GcsFileMetadata metadata;
//        try {
//            metadata = gcsService.getMetadata(toGcsFilename(path));
//        } catch (IOException e) {
//            throw new InternalIOException(e);
//        }
//        if (metadata == null) {
//            meterOff();
//            return null;
//        }
//        meterOff();
//        return Metadata.builder()
//                .withPath(path)
//                .withLastModified(new DateTime(metadata.getLastModified()))
//                .withLength(metadata.getLength())
//                .build();
//    }
//
//    @Override
//    public byte[] read(String path) {
//        return new byte[0];
//    }
//
//    @Override
//    public void write(String path, byte[] bytes) {
//
//    }
//
//    @Override
//    public InputStream inputStream(final String path) {
//        meterOn(this.getClass());
//        GcsFilename filename = toGcsFilename(path);
//        LOGGER.info("InputStream from {}", filename);
//        GcsInputChannel channel = gcsService.openPrefetchingReadChannel(filename, 0, BUFFER_SIZE);
//        meterOff();
//        return Channels.newInputStream(channel);
//    }
//
//    @Override
//    public OutputStream outputStream(final String path, final MediaType mediaType) {
//        meterOn(this.getClass());
//        GcsFilename filename = toGcsFilename(path);
//        LOGGER.info("OutputStream from {}", filename);
//        GcsFileOptions options = new GcsFileOptions.Builder()
//                .mimeType(mediaType.toString())
//                .build();
//        GcsOutputChannel channel;
//        try {
//            channel = gcsService.createOrReplace(filename, options);
//        } catch (IOException e) {
//            throw new InternalIOException(e);
//        }
//        meterOff();
//        return Channels.newOutputStream(channel);
//    }
//
//    @Override
//    public void copy(String fromPath, String toPath) {
//        meterOn(this.getClass());
//        GcsFilename fromFilename = toGcsFilename(fromPath);
//        GcsFilename toFilename = toGcsFilename(toPath);
//        try {
//            gcsService.copy(fromFilename, toFilename);
//        } catch (IOException e) {
//            throw new InternalIOException(e);
//        }
//        meterOff();
//    }
//
//    @Override
//    public String getSignedUploadUrl(final String path, final MediaType mediaType, final DateTime expiration) {
//        return getSignedUrl(HTTPMethod.PUT, toGcsFilename(path), mediaType, expiration);
//    }
//
//    @Override
//    public String getSignedDownloadUrl(final String path, final MediaType mediaType, final DateTime expiration) {
//        return getSignedUrl(HTTPMethod.GET, toGcsFilename(path), mediaType, expiration);
//    }
//
//    private String getSignedUrl(final HTTPMethod httpMethod,
//                                final GcsFilename filename,
//                                final MediaType mediaType,
//                                final DateTime expiration) {
//        try {
//            meterOn(this.getClass());
//            String unsigned = stringToSign(httpMethod, filename, mediaType.toString(), expiration);
//            String signature = sign(unsigned);
//            meterOff();
//            return String.format("%s/%s/%s?GoogleAccessId=%s&Expires=%d&Signature=%s",
//                    BASE_URL,
//                    filename.getBucketName(),
//                    filename.getObjectName(),
//                    appIdentityService.getServiceAccountName(),
//                    expiration.getMillis() / 1000,
//                    URLEncoder.encode(signature, UTF_8.name()));
//        } catch (UnsupportedEncodingException e) {
//            throw new InternalIOException(e);
//        }
//    }
//
//    private String sign(final String unsigned)
//            throws UnsupportedEncodingException {
//        // Note that the algorithm used by AppIdentity.signForApp() is "SHA256withRSA"
//        AppIdentityService.SigningResult signingResult = appIdentityService.signForApp(unsigned.getBytes(UTF_8));
//        return new String(Base64.encodeBase64(signingResult.getSignature(), false), UTF_8);
//    }
//
//    private String stringToSign(final HTTPMethod httpMethod,
//                                final GcsFilename filename,
//                                final String contentType,
//                                final DateTime expiration) {
//        String contentMD5 = "";
//        String canonicalizedExtensionHeaders = "";
//        String canonicalizedResource = String.format("/%s/%s", filename.getBucketName(), filename.getObjectName());
//        // expiration needs to be in seconds
//        return String.format("%s%n%s%n%s%n%d%n%s%s",
//                httpMethod,
//                contentMD5,
//                contentType,
//                expiration.getMillis() / 1000,
//                canonicalizedExtensionHeaders,
//                canonicalizedResource);
//    }
}
