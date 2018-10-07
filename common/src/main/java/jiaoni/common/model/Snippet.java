package jiaoni.common.model;

import com.google.common.base.MoreObjects;

public class Snippet {
    private String text;
    private double confidence;

    public Snippet(final String text, final double confidence) {
        this.text = text;
        this.confidence = confidence;
    }

    public String getText() {
        return text;
    }

    /**
     * Confidence range from [0, 1]
     */
    public double getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("text", text)
                .add("confidence", confidence)
                .toString();
    }
}
