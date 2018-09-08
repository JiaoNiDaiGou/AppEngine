package jiaonidaigou.appengine.api.access.storage;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.tools.cloudstorage.GcsFileMetadata;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsInputChannel;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.common.base.Charsets;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import jiaonidaigou.appengine.common.model.InternalIOException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.Channels;

import static com.google.common.base.Preconditions.checkArgument;
import static jiaonidaigou.appengine.common.utils.LocalMeter.meterOff;
import static jiaonidaigou.appengine.common.utils.LocalMeter.meterOn;

@Singleton
public class GcsClient implements StorageClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClient.class);

    /**
     * Used below to determine the size of chucks to read in. Should be > 1kb and < 10MB
     */
    private static final int BUFFER_SIZE = 2 * 1024 * 1024;

    private static final String BASE_URL = "https://storage.googleapis.com";

    private final AppIdentityService appIdentityService;
    private final GcsService gcsService;

    @Inject
    public GcsClient(final AppIdentityService appIdentityService,
                     final GcsService gcsService) {
        this.appIdentityService = appIdentityService;
        this.gcsService = gcsService;
    }

    /**
     * Path is in format of
     * gcs://bucket_a/bucket_b/file.ext
     * bucket is 'bucket_a/bucket_b'
     * object is 'file.ext'
     */
    private static GcsFilename toGcsFilename(final String path) {
        checkArgument(StringUtils.startsWithIgnoreCase(path, "gs://"), path + " is not a valid GCS path.");
        int lastSlash = StringUtils.lastIndexOf(path, '/');
        String bucket = StringUtils.substring(path, "gs://".length(), lastSlash);
        String object = StringUtils.substring(path, lastSlash + 1);
        return new GcsFilename(bucket, object);
    }

    @Override
    public boolean exists(final String path) {
        meterOn(this.getClass());
        boolean toReturn;
        try {
            toReturn = gcsService.getMetadata(toGcsFilename(path)) != null;
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
        meterOff();
        return toReturn;
    }

    @Override
    public Metadata getMetadata(final String path) {
        meterOn(this.getClass());
        GcsFileMetadata metadata;
        try {
            metadata = gcsService.getMetadata(toGcsFilename(path));
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
        if (metadata == null) {
            meterOff();
            return null;
        }
        meterOff();
        return Metadata.builder()
                .withPath(path)
                .withLastModified(new DateTime(metadata.getLastModified()))
                .withLength(metadata.getLength())
                .build();
    }

    @Override
    public InputStream inputStream(final String path) {
        meterOn(this.getClass());
        GcsFilename filename = toGcsFilename(path);
        LOGGER.info("InputStream from {}", filename);
        GcsInputChannel channel = gcsService.openPrefetchingReadChannel(filename, 0, BUFFER_SIZE);
        meterOff();
        return Channels.newInputStream(channel);
    }

    @Override
    public OutputStream outputStream(final String path, final MediaType mediaType) {
        meterOn(this.getClass());
        GcsFilename filename = toGcsFilename(path);
        LOGGER.info("OutputStream from {}", filename);
        GcsFileOptions options = new GcsFileOptions.Builder()
                .mimeType(mediaType.toString())
                .build();
        GcsOutputChannel channel;
        try {
            channel = gcsService.createOrReplace(filename, options);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
        meterOff();
        return Channels.newOutputStream(channel);
    }

    @Override
    public void copy(String fromPath, String toPath) {
        meterOn(this.getClass());
        GcsFilename fromFilename = toGcsFilename(fromPath);
        GcsFilename toFilename = toGcsFilename(toPath);
        try {
            gcsService.copy(fromFilename, toFilename);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
        meterOff();
    }

    @Override
    public String getSignedUploadUrl(final String path, final MediaType mediaType, final DateTime expiration) {
        return getSignedUrl(HTTPMethod.PUT, toGcsFilename(path), mediaType, expiration);
    }

    @Override
    public String getSignedDownloadUrl(final String path, final MediaType mediaType, final DateTime expiration) {
        return getSignedUrl(HTTPMethod.GET, toGcsFilename(path), mediaType, expiration);
    }

    private String getSignedUrl(final HTTPMethod httpMethod,
                                final GcsFilename filename,
                                final MediaType mediaType,
                                final DateTime expiration) {
        try {
            meterOn(this.getClass());
            String unsigned = stringToSign(httpMethod, filename, mediaType.toString(), expiration);
            String signature = sign(unsigned);
            meterOff();
            return String.format("%s/%s/%s?GoogleAccessId=%s&Expires=%d&Signature=%s",
                    BASE_URL,
                    filename.getBucketName(),
                    filename.getObjectName(),
                    appIdentityService.getServiceAccountName(),
                    expiration.getMillis() / 1000,
                    URLEncoder.encode(signature, Charsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            throw new InternalIOException(e);
        }
    }

    private String sign(final String unsigned)
            throws UnsupportedEncodingException {
        // Note that the algorithm used by AppIdentity.signForApp() is "SHA256withRSA"
        AppIdentityService.SigningResult signingResult = appIdentityService.signForApp(unsigned.getBytes());
        return new String(Base64.encodeBase64(signingResult.getSignature(), false), Charsets.UTF_8.name());
    }

    private String stringToSign(final HTTPMethod httpMethod,
                                final GcsFilename filename,
                                final String contentType,
                                final DateTime expiration) {
        String contentMD5 = "";
        String canonicalizedExtensionHeaders = "";
        String canonicalizedResource = String.format("/%s/%s", filename.getBucketName(), filename.getObjectName());
        // expiration needs to be in seconds
        return String.format("%s\n%s\n%s\n%d\n%s%s",
                httpMethod,
                contentMD5,
                contentType,
                expiration.getMillis() / 1000,
                canonicalizedExtensionHeaders,
                canonicalizedResource);
    }
}
