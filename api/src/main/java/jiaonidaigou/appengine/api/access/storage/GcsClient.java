package jiaonidaigou.appengine.api.access.storage;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GcsClient implements StorageClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClient.class);

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
        return null;
    }

    @Override
    public byte[] read(final String path) {
        BucketPath bucketPath = BucketPath.of(path);
        return storage.readAllBytes(bucketPath.getBucket(), bucketPath.getObject());
    }

    @Override
    public void write(final String path, final String mediaType, byte[] bytes) {
        BucketPath bucketPath = BucketPath.of(path);
        storage.create(
                BlobInfo.newBuilder(bucketPath.getBucket(), bucketPath.getObject())
                        .setContentType(mediaType)
                        .build(),
                bytes);
    }

    @Override
    public void copy(final String fromPath, final String toPath) {
    }

    @Override
    public URL getSignedUploadUrl(final String path, final String mediaType, final DateTime expiration) {
        BucketPath bucketPath = BucketPath.of(path);
        return storage.signUrl(
                BlobInfo.newBuilder(bucketPath.getBucket(), bucketPath.getObject()).setContentType(mediaType).build(),
                expiration.getMillis(),
                TimeUnit.MILLISECONDS,
                Storage.SignUrlOption.httpMethod(HttpMethod.POST),
                Storage.SignUrlOption.withContentType(),
                Storage.SignUrlOption.withMd5());
    }

    @Override
    public URL getSignedDownloadUrl(final String path, final String mediaType, final DateTime expiration) {
        BucketPath bucketPath = BucketPath.of(path);
        return storage.signUrl(
                BlobInfo.newBuilder(bucketPath.getBucket(), bucketPath.getObject()).setContentType(mediaType).build(),
                expiration.getMillis(),
                TimeUnit.MILLISECONDS,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                Storage.SignUrlOption.withContentType(),
                Storage.SignUrlOption.withMd5());
    }

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
