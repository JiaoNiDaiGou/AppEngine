package jiaonidaigou.appengine.api.access.storage;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import java.net.URL;
import java.util.List;

public interface StorageClient {
    Duration DEFAULT_EXPIRATION_DURATION = Duration.standardHours(12);

    /**
     * If a file or a directory exists.
     */
    boolean exists(final String path);

    /**
     * Delete the file given path. Returns true means file get deleted. false means file not found.
     */
    boolean delete(final String path);

    Metadata getMetadata(final String path);

    byte[] read(final String path);

    void write(final String path, final String mediaType, final byte[] bytes);

    void copy(final String fromPath, final String toPath);

    URL getSignedUploadUrl(final String path, final String mediaType, final DateTime expiration);

    List<String> listAll(final String bucketPath);

    default URL getSignedUploadUrl(final String path, final String mediaType, final Duration duration) {
        return getSignedUploadUrl(path, mediaType, DateTime.now(DateTimeZone.UTC).plus(duration));
    }

    default URL getSignedUploadUrl(final String path, final String mediaType) {
        return getSignedUploadUrl(path, mediaType, DEFAULT_EXPIRATION_DURATION);
    }

    URL getSignedDownloadUrl(final String path, final String mediaType, final DateTime expiration);

    default URL getSignedDownloadUrl(final String path, final String mediaType, final Duration duration) {
        return getSignedDownloadUrl(path, mediaType, DateTime.now(DateTimeZone.UTC).plus(duration));
    }

    default URL getSignedDownloadUrl(final String path, final String mediaType) {
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
