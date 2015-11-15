package foo.bar;

import java.util.LinkedList;

/**
 * Thread context called from {@link SQLAgentMethodProcessor#preInvoke(String, Object[])} and
 * {@link SQLAgentMethodProcessor#postInvoke()}.
 * It stores execution times and is used to detect recursive calls, for example
 * when the JDBC driver internally also invokes {@link java.sql.Statement#execute(String)}.
 */
public class SQLAgentThreadContext {


    private LinkedList<String> callStack = new LinkedList<>();

    private long startTime;

    private long endTime;


    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void startCall(String methodName) {
        callStack.addLast(methodName);
    }

    public void endCall() {
        callStack.removeLast();
    }

    public int getCallStackSize() {
        return callStack.size();
    }


    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getDuration() {
        return endTime - startTime;
    }
}
