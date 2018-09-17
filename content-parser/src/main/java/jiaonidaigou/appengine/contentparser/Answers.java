package jiaonidaigou.appengine.contentparser;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Answers<T> implements Iterable<Answer<T>> {
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

    public static <M> Answers<M> useInputIfNoAnswer(final Answers<M> answers,
                                                    final String input) {
        if (answers.size() != 0) {
            return answers;
        } else {
            Answer<M> answerOfInput = new Answer<M>().setTarget(null, Conf.CONFIRMED)
                    .setRawStringAfterExtraction(input);
            return Answers.of(answerOfInput);
        }
    }

    private static final Answers NO_ANSWER = new Answers<>(ImmutableList.of());

    private final List<Answer<T>> results;

    private Answers(List<Answer<T>> results) {
        this.results = results;
    }

    public static <T> Answers<T> noAnswer() {
        return NO_ANSWER;
    }

    public static <T> Answers of(final Iterable<Answer<T>> answers) {
        return new Answers<>(ImmutableList.copyOf(answers));
    }

    public static <T> Answers of(final Answer<T> answer) {
        return new Answers<>(ImmutableList.of(answer));
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
}
