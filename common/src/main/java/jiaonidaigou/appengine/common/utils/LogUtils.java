package jiaonidaigou.appengine.common.utils;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;
import java.util.concurrent.TimeUnit;

public class LogUtils {
    private static class Context {
        private final Stopwatch stopwatch;
        private final String name;

        private Context(String name) {
            this.name = name;
            this.stopwatch = Stopwatch.createStarted();
        }
    }

    private static final String UNKNOWN_OP = "unknownOp";

    private static final ThreadLocal<Stack<Context>> THREAD_LOCAL_CONTEXTS = new ThreadLocal<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

    public synchronized static void startOp(final String opName) {
        Stack<Context> stack = THREAD_LOCAL_CONTEXTS.get();
        if (stack == null) {
            stack = new Stack<>();
            THREAD_LOCAL_CONTEXTS.set(stack);
        }
        stack.push(new Context(opName));
        LOGGER.info("{} START.", opName);
    }

    public synchronized static void startOp(final Class opClazz) {
        startOp(callerMethodName(opClazz));
    }

    public synchronized static void startOp() {
        startOp(callerMethodName(null));
    }

    public synchronized static void endOp() {
        Stack<Context> stack = THREAD_LOCAL_CONTEXTS.get();
        if (stack == null) {
            stack = new Stack<>();
            THREAD_LOCAL_CONTEXTS.set(stack);
        }
        if (stack.isEmpty()) {
            return;
        }
        Context context = stack.pop();
        context.stopwatch.stop();
        long taken = context.stopwatch.elapsed(TimeUnit.MILLISECONDS);
        LOGGER.info("{} END. take {} ms.", context.name, taken);
    }

    private static String callerMethodName(final Class clazz) {
        // 0: Thread.getStackTrace
        // 1: LogUtils.callerMethodName
        // 2: LogUtils.startOp/endOp
        // 3: Actual caller
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if (elements.length < 4) {
            return UNKNOWN_OP;
        }
        StackTraceElement element = elements[3];
        return (clazz == null ? element.getClassName() : clazz.getName()) + "#" + element.getMethodName();
    }
}
