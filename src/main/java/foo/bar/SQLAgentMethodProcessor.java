package foo.bar;

import javassist.CannotCompileException;
import javassist.expr.MethodCall;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * This class contains all pointcuts and advices for JDBC calls.
 * <p>In the future this could be a service implementation so that multiple classes could be
 */
public class SQLAgentMethodProcessor {

    private static ThreadLocal<SQLAgentThreadContext> threadContext = new ThreadLocal<SQLAgentThreadContext>() {
        @Override
        protected SQLAgentThreadContext initialValue() {
            return new SQLAgentThreadContext();
        }
    };


    private static SQLListener sqlListener = new SQLListener();

    @ProcessMethodCall(targetClass = Connection.class, methodName = "prepareStatement")
    public void replacePrepareStatement(MethodCall methodCall) throws CannotCompileException {
        methodCall.replace(getMethodBody(methodCall, "foo.bar.SQLAgentMethodProcessor.prepareStatement($_, $1);"));
    }

    @ProcessMethodCall(targetClass = Connection.class, methodName = "prepareCall")
    public void replacePrepareCall(MethodCall methodCall) throws CannotCompileException {
        replacePrepareStatement(methodCall);
    }

    @ProcessMethodCall(targetClass = Statement.class, methodName = "addBatch")
    public void replaceAddBatch(MethodCall methodCall) throws CannotCompileException {
        methodCall.replace(getMethodBody(methodCall, "foo.bar.SQLAgentMethodProcessor.prepareStatement($0, $1);"));
    }

    public static void prepareStatement(Statement stmt, String sql) {

        sqlListener.prepareStatement(stmt, sql);

    }

    @ProcessMethodCall(targetClass = Statement.class, methodName = "clearBatch")
    public void replaceClearBatch(MethodCall methodCall) throws CannotCompileException {
        methodCall.replace(getMethodBody(methodCall, "foo.bar.SQLAgentMethodProcessor.clearStatements($0);"));
    }

    public static void clearStatements(Statement stmt) {

        sqlListener.clearStatement(stmt);

    }

    @ProcessMethodCall(targetClass = Statement.class, methodName = "execute(?!Batch).*")
    public void replaceExecuteStatement(MethodCall methodCall) throws Exception {
            methodCall.replace(getMethodBody(methodCall, "foo.bar.SQLAgentMethodProcessor.notifyExecution($1);"));
    }


    @ProcessMethodCall(targetClass = Statement.class, methodName = "executeBatch")
    public void replaceExecuteBatch(MethodCall methodCall) throws Exception {
        methodCall.replace(getMethodBody(methodCall, "foo.bar.SQLAgentMethodProcessor.notifyExecution($0);"));
    }

    public static void notifyExecution(Statement stmt) {
        sqlListener.notifyExecution(stmt, threadContext.get().getDuration());
    }

    public static void notifyExecution(String stmt) {
        sqlListener.notifyExecution(stmt, threadContext.get().getDuration());
    }


    @ProcessMethodCall(targetClass = PreparedStatement.class, methodName = "execute.*")
    public void replaceExecutePreparedStatement(MethodCall methodCall) throws CannotCompileException {
        methodCall.replace(getMethodBody(methodCall, "foo.bar.SQLAgentMethodProcessor.notifyExecution($0);"));
    }


    @ProcessMethodCall(targetClass = PreparedStatement.class, methodName = "close")
    public void replaceClosePreparedStatement(MethodCall methodCall) throws CannotCompileException {
        methodCall.replace(getMethodBody(methodCall, "foo.bar.SQLAgentMethodProcessor.notifyClose($0);"));
    }


    public static void notifyClose(PreparedStatement statement) {
        sqlListener.notifyClose(statement);
    }


    public static void preInvoke(String methodName, Object[] args) {
        SQLAgentThreadContext tc = threadContext.get();
        tc.start(methodName);
    }

    /**
     *
     * @return {@code true} if this call was the top level call.
     */
    public static boolean postInvoke() {
        SQLAgentThreadContext tc = threadContext.get();
        tc.endCall();
        return tc.getCallStackSize() == 0;
    }

    private String getMethodBody(MethodCall methodCall, String afterAction) {
        return                     "{" +
                "  foo.bar.SQLAgentMethodProcessor.preInvoke(\"" + methodCall.getMethodName() + "\", $args);" +
                "  try { " +
                "    $_ = $proceed($$); " +
                "  } finally {" +
                "    if (foo.bar.SQLAgentMethodProcessor.postInvoke()) {" +
                "      " + afterAction +
                "    }" +
                "  }" +
                "}";

    }

}
