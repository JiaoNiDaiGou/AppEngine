package jiaonidaigou.appengine.common.utils;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class Retrier {
    private static final Logger LOGGER = LoggerFactory.getLogger(Retrier.class);

    public interface WaitingPolicy {
        long getWaitPeriod(int nextRetryAttempt, long elapsedMillis);
    }

    public interface StopPolicy {
        boolean stop(int nextRetryAttempt, long elapsedMillis);
    }

    private final Predicate<Exception> retryOnError;
    private final Predicate retryOnResult;
    private final WaitingPolicy waitingPolicy;
    private final StopPolicy stopPolicy;
    private final VoidCallable beforeRetry;

    private Retrier(final Builder builder) {
        retryOnError = builder.retryOnError;
        retryOnResult = builder.retryOnResult;
        waitingPolicy = builder.waitingPolicy;
        stopPolicy = builder.stopPolicy;
        beforeRetry = builder.beforeRetry;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unchecked")
    public <T> T call(final Callable<T> theCall)
            throws Exception {
        int attempt = 0;
        long elapsed = 0;

        Stopwatch stopwatch = Stopwatch.createStarted();
        Exception lastCaughtException;
        do {
            try {
                if (attempt > 0) {
                    long waitPeriodMillis = waitingPolicy.getWaitPeriod(attempt, elapsed);
                    LOGGER.info("Try Attempt {}: will retry after {}ms.", attempt, waitPeriodMillis);
                    Uninterruptibles.sleepUninterruptibly(waitPeriodMillis, TimeUnit.MILLISECONDS);
                    if (beforeRetry != null) {
                        beforeRetry.call();
                    }
                }
                T toReturn = theCall.call();
                if (retryOnResult == null || !retryOnResult.test(toReturn)) {
                    return toReturn;
                }
                LOGGER.warn("Try Attempt {} error on result.", attempt);
                lastCaughtException = new RuntimeException("Result not valid: " + toReturn);
            } catch (Exception e) {
                if (retryOnError == null || !retryOnError.test(e)) {
                    throw e;
                }
                LOGGER.warn("Try Attempt {} error.", attempt, e);
                lastCaughtException = e;
            } finally {
                attempt++;
                elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            }
        } while (!stopPolicy.stop(attempt, elapsed));
        LOGGER.error("Failed after {} retries.", attempt - 1, lastCaughtException);
        throw lastCaughtException;
    }

    public void call(final VoidCallable theCall)
            throws Exception {
        call(() -> {
            theCall.call();
            return null;
        });
    }

    @FunctionalInterface
    public interface VoidCallable {
        void call() throws Exception;
    }

    public static class Builder {
        private Predicate<Exception> retryOnError;
        private Predicate<?> retryOnResult;
        private WaitingPolicy waitingPolicy;
        private StopPolicy stopPolicy;
        private VoidCallable beforeRetry;

        @SafeVarargs
        public final Builder retryOnError(final Class<? extends Exception>... exceptions) {
            return retryOnError(Arrays.asList(exceptions));
        }

        public Builder retryOnError(final List<Class<? extends Exception>> exceptions) {
            retryOnError = t -> exceptions.stream().anyMatch(ex -> ex.isInstance(t));
            return this;
        }

        public <T> Builder retryOnResult(final Predicate<T> retryOnResult) {
            this.retryOnResult = retryOnResult;
            return this;
        }

        public Builder retryOnAnyException() {
            return retryOnError(Exception.class);
        }

        public Builder waitingPolicy(final WaitingPolicy waitingPolicy) {
            this.waitingPolicy = waitingPolicy;
            return this;
        }

        public Builder exponentialBackOffWaiting(final int startMillis) {
            this.waitingPolicy = (nextRetryAttempt, elapsedMillis) ->
                    Math.min(startMillis * (1 << (nextRetryAttempt - 1)), Integer.MAX_VALUE);
            return this;
        }

        public Builder exponentialJitteredBackOffWaiting(final int startMillis, final int maxWaitTime) {
            this.waitingPolicy = (nextRetryAttempt, elapsedMillis) -> {
                int maxWait = (int) Math.min((long) startMillis * (1 << (nextRetryAttempt - 1)), maxWaitTime);
                return (long) (Math.random() * maxWait);
            };
            return this;
        }

        public Builder exponentialJitteredBackOffWaiting(final int startMillis) {
            return exponentialJitteredBackOffWaiting(startMillis, Integer.MAX_VALUE);
        }

        public Builder stopPolicy(final StopPolicy stopPolicy) {
            this.stopPolicy = stopPolicy;
            return this;
        }

        public Builder stopWithMaxAttempts(final int maxAttempts) {
            this.stopPolicy = ((nextRetryAttempt, elapsedMillis) -> nextRetryAttempt >= maxAttempts);
            return this;
        }

        public Builder stopWithMaxElapseTime(final long time, final TimeUnit unit) {
            long maxElapseMs = unit.toMillis(time);
            this.stopPolicy = ((nextRetryAttempt, elapsedMillis) -> elapsedMillis > maxElapseMs);
            return this;
        }

        public Builder stopWithMaxAttemptsOrMaxElapseTime(final int maxAttempts, final long time, final TimeUnit unit) {
            long maxElapseMs = unit.toMillis(time);
            this.stopPolicy = (nextRetryAttempt, elapsedMillis) ->
                    nextRetryAttempt >= maxAttempts || elapsedMillis > maxElapseMs;
            return this;
        }

        public Builder beforeRetry(Callable<Void> beforeRetry) {
            VoidCallable voidCallable = beforeRetry::call;
            return beforeRetry(voidCallable);
        }

        public Builder beforeRetry(VoidCallable beforeRetry) {
            this.beforeRetry = beforeRetry;
            return this;
        }

        public Retrier build() {
            checkState(retryOnError != null || retryOnResult != null);
            checkNotNull(waitingPolicy);
            checkNotNull(stopPolicy);
            return new Retrier(this);
        }
    }
}
