package jiaonidaigou.appengine.contentparser;

public interface Parser<T> {
    Answers<T> parse(final String input);
}
