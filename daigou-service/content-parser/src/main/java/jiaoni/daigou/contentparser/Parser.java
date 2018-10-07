package jiaoni.daigou.contentparser;

public interface Parser<T> {
    Answers<T> parse(final String input);
}
