package jiaonidaigou.appengine.api.access.storage;

import com.google.common.net.MediaType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public interface StorageClient {
    Duration DEFAULT_EXPIRATION_DURATION = Duration.standardHours(12);

    /**
     * If a file or a directory exists.
     */
    boolean exists(final String path)
            throws IOException;

    Metadata getMetadata(final String path)
            throws IOException;

    InputStream inputStream(final String path)
            throws IOException;

    OutputStream outputStream(final String path, final MediaType mediaType);

    default Reader read(final String path)
            throws IOException {
        return new InputStreamReader(inputStream(path));
    }

    default Writer write(final String path, final MediaType mediaType) {
        return new OutputStreamWriter(outputStream(path, mediaType));
    }

    void copy(final String fromPath, final String toPath)
            throws IOException;

    String getSignedUploadUrl(final String path, final MediaType mediaType, final DateTime expiration)
            throws UnsupportedEncodingException;

    default String getSignedUploadUrl(final String path, final MediaType mediaType, final Duration duration)
            throws UnsupportedEncodingException {
        return getSignedUploadUrl(path, mediaType, DateTime.now(DateTimeZone.UTC).plus(duration));
    }

    default String getSignedUploadUrl(final String path, final MediaType mediaType)
            throws UnsupportedEncodingException {
        return getSignedUploadUrl(path, mediaType, DEFAULT_EXPIRATION_DURATION);
    }

    String getSignedDownloadUrl(final String path, final MediaType mediaType, final DateTime expiration)
            throws UnsupportedEncodingException;

    default String getSignedDownloadUrl(final String path, final MediaType mediaType, final Duration duration)
            throws UnsupportedEncodingException {
        return getSignedDownloadUrl(path, mediaType, DateTime.now(DateTimeZone.UTC).plus(duration));
    }

    default String getSignedDownloadUrl(final String path, final MediaType mediaType)
            throws UnsupportedEncodingException {
        return getSignedDownloadUrl(path, mediaType, DEFAULT_EXPIRATION_DURATION);
    }

    class Metadata {
        private String path;
        private DateTime lastModified;
        private Long length;

        static Builder builder() {
            return new Builder();
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }

        @Override
        public boolean equals(Object o) {
            return EqualsBuilder.reflectionEquals(this, o);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        public String getPath() {
            return path;
        }

        public DateTime getLastModified() {
            return lastModified;
        }

        public Long getLength() {
            return length;
        }

        static final class Builder {
            private String path;
            private DateTime lastModified;
            private Long length;

            Builder withPath(String path) {
                this.path = path;
                return this;
            }

            Builder withLastModified(DateTime lastModified) {
                this.lastModified = lastModified;
                return this;
            }

            Builder withLength(Long length) {
                this.length = length;
                return this;
            }

            public Metadata build() {
                Metadata metadata = new Metadata();
                metadata.path = this.path;
                metadata.lastModified = this.lastModified;
                metadata.length = this.length;
                return metadata;
            }
        }
    }
}
