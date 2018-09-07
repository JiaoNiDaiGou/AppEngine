package jiaonidaigou.appengine.lib.ocrspace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

/**
 * Response from OCR.Space API.
 * See https://ocr.space/ocrapi
 */
public class ParseResponse {
    @JsonProperty("ParsedResults")
    private List<ParsedResult> parsedResults;

    /**
     * The exit code shows if OCR completed successfully, partially or failed with error
     * <p>
     * 1: Parsed Successfully (Image / All pages parsed successfully)
     * 2: Parsed Partially (Only few pages out of all the pages parsed successfully)
     * 3: Image / All the PDF pages failed parsing (This happens mainly because the OCR engine fails to parse an image)
     * 4: Error occurred when attempting to parse (This happens when a fatal error occurs during parsing )
     */
    @JsonProperty("OCRExitCode")
    private int ocrExitCode;

    @JsonProperty("IsErroredOnProcessing")
    private boolean erroredOnProcessing;

    @JsonProperty("ErrorMessage")
    private List<String> errorMessage;

    @JsonProperty("ErrorDetails")
    private String errorDetails;

    @JsonProperty("SearchablePDFURL")
    private String searchablePdfUrl;

    @JsonProperty("ProcessingTimeInMilliseconds")
    private long processingTimeInMilliseconds;

    @JsonIgnore
    public String getAllParsedText() {
        if (CollectionUtils.isEmpty(parsedResults)) {
            return null;
        }
        return parsedResults.stream()
                .filter(t -> t.getFileParseExitCode() == 1)
                .map(ParsedResult::getParsedText)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    public List<ParsedResult> getParsedResults() {
        return parsedResults;
    }

    public int getOcrExitCode() {
        return ocrExitCode;
    }

    public boolean isErroredOnProcessing() {
        return erroredOnProcessing;
    }

    public List<String> getErrorMessage() {
        return errorMessage;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public String getSearchablePdfUrl() {
        return searchablePdfUrl;
    }

    public long getProcessingTimeInMilliseconds() {
        return processingTimeInMilliseconds;
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
