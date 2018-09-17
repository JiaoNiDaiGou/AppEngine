package jiaonidaigou.appengine.contentparser;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class AnswerMatchers<T> {
    public static <T> AnswersMatcher<T> noAnswer() {
        return new AnswersMatcher<>(t -> {
            assertEquals(0, t.size());
            return true;
        }, "");
    }

    @SafeVarargs
    public static <T> AnswersMatcher<T> hasAnswers(final AnswerMatcher<T>... answerMatchers) {
        return new AnswersMatcher<>(t -> {
            assertEquals(answerMatchers.length, t.size());
            int i = 0;
            for (Answer<T> answer : t) {
                assertThat(answer, answerMatchers[i++]);
            }
            return true;
        }, "");
    }

    @SafeVarargs
    public static <A, B> AnswersMatcher<A> hasAnswers(final Function<A, B> transform,
                                                      final AnswerMatcher<B>... answerMatchers) {
        return new AnswersMatcher<>(t -> {
            assertEquals(answerMatchers.length, t.size());
            int i = 0;
            for (Answer<A> answer : t) {
                Answer<B> transformedAnswer = new Answer<B>()
                        .setTarget(transform.apply(answer.getTarget()), answer.getConfidence())
                        .setRawStringAfterExtraction(answer.getRawStringAfterExtraction());
                assertThat(transformedAnswer, answerMatchers[i++]);
            }
            return true;
        }, "");
    }

    public static <T> AnswerMatcher<T> atLeast(final T expectedVal, final int minimumConf) {
        return new AnswerMatcher<>(
                t -> expectedVal.equals(t.getTarget()) && t.getConfidence() >= minimumConf,
                String.format("[>= %d] %s", minimumConf, expectedVal));
    }


    public static <T> AnswerMatcher<T> is(final T expectedVal, final int expectedConf) {
        return new AnswerMatcher<>(
                t -> expectedVal.equals(t.getTarget()) && t.getConfidence() == expectedConf,
                String.format("[== %d] %s", expectedConf, expectedVal));
    }

    public static class AnswersMatcher<T> extends BaseMatcher<Answers<T>> {
        private final Function<Answers<T>, Boolean> check;
        private final String expectedMessage;

        private AnswersMatcher(Function<Answers<T>, Boolean> check,
                               String expectedMessage) {
            this.check = check;
            this.expectedMessage = expectedMessage;
        }

        @Override
        public boolean matches(Object o) {
            Answers<T> answers = (Answers<T>) o;
            return this.check.apply(answers);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(expectedMessage);
        }
    }

    public static class AnswerMatcher<T> extends BaseMatcher<Answer<T>> {
        private final Function<Answer<T>, Boolean> check;
        private final String expectedMessage;

        private AnswerMatcher(Function<Answer<T>, Boolean> check,
                              String expectedMessage) {
            this.check = check;
            this.expectedMessage = expectedMessage;
        }

        @Override
        public boolean matches(Object o) {
            Answer<T> answer = (Answer<T>) o;
            return this.check.apply(answer);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(expectedMessage);
        }
    }
}
