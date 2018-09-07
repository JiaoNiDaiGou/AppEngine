package jiaonidaigou.appengine.lib.ocrspace.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.File;
import java.io.InputStream;

public class ParseRequest {
    private final String imageUrl;
    private final File imageFile;
    private final byte[] imageBytes;
    private final InputStream image;
    private final Language language;
    private final boolean overlayRequired;
    /**
     * [Optional] String value: PDF,GIF,PNG,JPG
     */
    private final FileType fileType;
    private final boolean createSearchablePdf;
    private final boolean searchablePdfHideTextLayer;

    private ParseRequest(final Builder builder) {
        this.imageUrl = builder.imageUrl;
        this.imageFile = builder.imageFile;
        this.image = builder.imageInputStream;
        this.imageBytes = builder.imageBytes;
        this.language = builder.language;
        this.overlayRequired = builder.overlayRequired;
        this.fileType = builder.fileType;
        this.createSearchablePdf = builder.createSearchablePdf;
        this.searchablePdfHideTextLayer = builder.searchablePdfHideTextLayer;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Language getLanguage() {
        return language;
    }

    public boolean isOverlayRequired() {
        return overlayRequired;
    }

    public FileType getFileType() {
        return fileType;
    }

    public boolean isCreateSearchablePdf() {
        return createSearchablePdf;
    }

    public boolean isSearchablePdfHideTextLayer() {
        return searchablePdfHideTextLayer;
    }

    public InputStream getImage() {
        return image;
    }

    public File getImageFile() {
        return imageFile;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public enum Language {
        ENGLISH("eng"),
        CHINESE_SIMPLIFIED("chs");
        private String value;

        Language(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    public enum FileType {
        PNG, JPG, GIF, PDF
    }

    public static final class Builder {
        private String imageUrl;
        private InputStream imageInputStream;
        private byte[] imageBytes;
        private File imageFile;
        private Language language;
        private boolean overlayRequired;
        private FileType fileType = FileType.PNG;
        private boolean createSearchablePdf;
        private boolean searchablePdfHideTextLayer;

        private Builder() {
        }

        public Builder withImageUrl(String url) {
            this.imageUrl = url;
            return this;
        }

        public Builder withImage(InputStream imageInputStream) {
            this.imageInputStream = imageInputStream;
            return this;
        }

        public Builder withImageBytes(final byte[] image) {
            this.imageBytes = image;
            return this;
        }

        public Builder withImageFile(final String path) {
            return withImageFile(new File(path));
        }

        public Builder withImageFile(final File imageFile) {
            this.imageFile = imageFile;
            return this;
        }

        public Builder withLanguage(Language language) {
            this.language = language;
            return this;
        }

        public Builder withOverlayRequired(boolean overlayRequired) {
            this.overlayRequired = overlayRequired;
            return this;
        }

        public Builder withFileType(FileType fileType) {
            this.fileType = fileType;
            return this;
        }

        public Builder withCreateSearchablePdf(boolean createSearchablePdf) {
            this.createSearchablePdf = createSearchablePdf;
            return this;
        }

        public Builder withSearchablePdfHideTextLayer(boolean searchablePdfHideTextLayer) {
            this.searchablePdfHideTextLayer = searchablePdfHideTextLayer;
            return this;
        }

        public ParseRequest build() {
            return new ParseRequest(this);
        }
    }
}
