package jiaoni.daigou.contentparser;

import com.google.common.base.MoreObjects;

public class Answer<T> {
    private T target;
    private String rawStringAfterExtraction;
    private int confidence = Conf.ZERO;

    public Answer<T> setTarget(T target, final int confidence) {
        this.target = target;
        this.confidence = confidence;
        return this;
    }

    public boolean hasTarget() {
        return target != null;
    }

    public T getTarget() {
        return target;
    }

    public String getRawStringAfterExtraction() {
        return rawStringAfterExtraction;
    }

    public Answer<T> setRawStringAfterExtraction(String rawStringAfterExtraction) {
        this.rawStringAfterExtraction = rawStringAfterExtraction;
        return this;
    }

    public int getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("target", target)
                .add("rawStringAfterExtraction", rawStringAfterExtraction)
                .add("confidence", confidence)
                .toString();
    }
}
