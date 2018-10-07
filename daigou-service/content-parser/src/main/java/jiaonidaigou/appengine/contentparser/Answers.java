package jiaonidaigou.appengine.contentparser;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Answers<T> implements Iterable<Answer<T>> {
    /**
     * Calculate a confidence based on given answers using weighted average.
     */
    @SafeVarargs
    public static int weightedAverage(final Pair<Answer, Integer>... answersAndWeight) {
        if (answersAndWeight.length == 0) {
            return 0;
        }
        int sum = 0;
        int totalWeight = 0;
        for (Pair<Answer, Integer> pair : answersAndWeight) {
            Answer answer = pair.getLeft();
            int weight = pair.getRight();
            sum += answer.hasTarget() ? answer.getConfidence() * weight : 0;
            totalWeight += weight;
        }
        return sum / totalWeight;
    }

    /**
     * Create a new {@link Answers} just wrapping input if given answers has no target.
     */
    public static <M> Answers<M> useInputIfNoAnswer(final Answers<M> answers,
                                                    final String input) {
        if (answers.hasAtLeastOneTarget()) {
            return answers;
        } else {
            Answer<M> answerOfInput = new Answer<M>()
                    .setTarget(null, Conf.ZERO)
                    .setRawStringAfterExtraction(input);
            return Answers.of(answerOfInput);
        }
    }

    private static final Answers NO_ANSWER = new Answers<>(ImmutableList.of(), false);

    private final List<Answer<T>> results;

    private Answers(final List<Answer<T>> results, final boolean autoSort) {
        List<Answer<T>> sorted = new ArrayList<>(results);
        if (autoSort) {
            sorted.sort((a, b) -> Integer.compare(b.getConfidence(), a.getConfidence()));
        }
        this.results = sorted;
    }

    public static <T> Answers<T> noAnswer() {
        return NO_ANSWER;
    }

    public static <T> Answers<T> of(final Iterable<Answer<T>> answers, final boolean autoSort) {
        return new Answers<>(ImmutableList.copyOf(answers), autoSort);
    }

    public static <T> Answers<T> of(final Iterable<Answer<T>> answers) {
        return of(answers, true);
    }

    public static <T> Answers<T> of(final Answer<T> answer) {
        return new Answers<>(ImmutableList.of(answer), false);
    }

    public List<Answer<T>> getResults() {
        return results;
    }

    public int size() {
        return results.size();
    }

    public boolean hasAtLeastOneTarget() {
        return results != null && results.stream().anyMatch(Answer::hasTarget);
    }

    public List<T> topResults(int limit) {
        if (results == null) {
            return ImmutableList.of();
        }
        return results.stream()
                .filter(Answer::hasTarget)
                .map(Answer::getTarget)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("results", results)
                .toString();
    }

    @Override
    public Iterator<Answer<T>> iterator() {
        return results.iterator();
    }

    public Stream<Answer<T>> stream() {
        return results.stream();
    }
}
