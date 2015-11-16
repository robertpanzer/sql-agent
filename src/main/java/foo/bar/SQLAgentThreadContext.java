package foo.bar;

import java.util.LinkedList;

/**
 * Thread context called from {@link SQLAgentMethodProcessor#preInvoke(String, Object[])} and
 * {@link SQLAgentMethodProcessor#postInvoke()}.
 * It stores execution times and is used to detect recursive calls, for example
 * when the JDBC driver internally also invokes {@link java.sql.Statement#execute(String)}.
 */
public class SQLAgentThreadContext {

    private class StackFrame {

        private final String methodName;

        private final long startTime = System.nanoTime();

        public StackFrame(String methodName) {
            this.methodName = methodName;
        }
    }

    private LinkedList<StackFrame> callStack = new LinkedList<>();

    private long lastDuration;

    public void start(String methodName) {

        callStack.push(new StackFrame(methodName));
    }

    public void endCall() {

        StackFrame stackFrame = callStack.pop();
        lastDuration = System.nanoTime() - stackFrame.startTime;

    }

    public int getCallStackSize() {
        return callStack.size();
    }


    public long getDuration() {
        return lastDuration;
    }
}
