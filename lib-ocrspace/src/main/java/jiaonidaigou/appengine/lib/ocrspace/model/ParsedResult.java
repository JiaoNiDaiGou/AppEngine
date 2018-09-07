package jiaonidaigou.appengine.lib.ocrspace.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class ParsedResult {
    /**
     * The exit code returned by the parsing engine
     * 0: File not found
     * 1: Success
     * -10: OCR Engine Parse Error
     * -20: Timeout
     * -30: Validation Error
     * -99: Unknown Error
     */
    @JsonProperty("FileParseExitCode")
    private int fileParseExitCode;

    @JsonProperty("ParsedText")
    private String parsedText;

    @JsonProperty("TextOverlay")
    private TextOverlay textOverlay;

    @JsonProperty("ErrorMessage")
    private String errorMessage;

    @JsonProperty("ErrorDetails")
    private String errorDetails;

    public int getFileParseExitCode() {
        return fileParseExitCode;
    }

    public String getParsedText() {
        return parsedText;
    }

    public TextOverlay getTextOverlay() {
        return textOverlay;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorDetails() {
        return errorDetails;
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
}
