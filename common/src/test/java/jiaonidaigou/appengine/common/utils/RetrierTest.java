package jiaonidaigou.appengine.common.utils;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RetrierTest {
    private static class TheCall {
        void callAndFail() {
            throw new RuntimeException();
        }

        void callWaitAndFail(final long waitMillis) {
            Uninterruptibles.sleepUninterruptibly(waitMillis, TimeUnit.MILLISECONDS);
            throw new RuntimeException();
        }

        int returnOne() {
            System.out.println("return 1");
            return 1;
        }
    }

    @Test
    public void testCall_retryOnResult() {
        TheCall theCall = spy(new TheCall());
        int maxAttempts = 3;
        Retrier underTest = Retrier.builder()
                .<Integer>retryOnResult(t -> t == 1)
                .exponentialBackOffWaiting(0)
                .stopWithMaxAttempts(maxAttempts)
                .build();
        try {
            underTest.call(theCall::returnOne);
        } catch (Exception e) {
        }

        verify(theCall, times(maxAttempts)).returnOne();
    }

    @Test
    public void testCall_stopAtMaxAttempts() {
        TheCall theCall = spy(new TheCall());

        int maxAttempts = 3;

        Retrier underTest = Retrier.builder()
                .retryOnAnyException()
                .exponentialBackOffWaiting(0)
                .stopWithMaxAttempts(maxAttempts)
                .build();

        try {
            underTest.call(theCall::callAndFail);
        } catch (Exception e) {
        }

        verify(theCall, times(maxAttempts)).callAndFail();
    }

    @Test
    public void testCall_stopAtMaxElapsedTime() {
        TheCall theCall = new TheCall();

        long maxElapsedMs = 3000L;

        Retrier underTest = Retrier.builder()
                .retryOnAnyException()
                .exponentialBackOffWaiting(0)
                .stopWithMaxElapseTime(maxElapsedMs, TimeUnit.MILLISECONDS)
                .build();
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            underTest.call(() -> theCall.callWaitAndFail(500L));
        } catch (Exception e) {
        }
        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        assertTrue(elapsed >= 3000L && elapsed < 4000L);
    }
}
