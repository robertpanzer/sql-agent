package foo.bar;

import foo.bar.api.SQLMonitor;

import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;


/**
 * The internal callback for JDBC calls.
 * This class contains all internal information to map Statement executions to their SQL strings,
 * manage closed statements etc.
 */
public class SQLListener {


    /**
     * @return The service implementation of the SQLMonitor, which is the users callback for JDBC calls.
     */
    public SQLMonitor getSqlMonitor() {
        Iterator<SQLMonitor> iter = ServiceLoader.load(SQLMonitor.class).iterator();
        if (iter.hasNext()) {
            return iter.next();
        }
        return new SQLMonitor() {
            @Override
            public void notifyExecution(String sql, long duration) {
            }

            @Override
            public void notifyExecution(List<String> sqls, long duration) {

            }
        };
    }

    /**
     * This ThreadLocal stores the mapping between Statements and the corresponding SQL strings.
     */
    private static ThreadLocal<LinkedList<StatementHolder>> statements = new ThreadLocal<LinkedList<StatementHolder>>() {
        @Override
        protected LinkedList initialValue() {
            return new LinkedList<>();
        }
    };

    public void prepareStatement(Statement stmt, String sql) {

        // Hibernate uses c3p0 which invokes Connection.prepareStatement by reflection.
        // So we have to get the inner statement from the c3p0 proxy statement.
        Statement realStatement = unwrapStatement(stmt);

        StatementHolder statementHolder = getStatementHolder(realStatement);
        if (statementHolder == null) {
            statements.get().addFirst(new StatementHolder(sql, realStatement));
        } else {
            statementHolder.addSql(sql);
        }
    }

    private Statement unwrapStatement(Statement stmt) {
        Statement realStatement = null;

        if ("com.mchange.v2.c3p0.impl.NewProxyPreparedStatement".equals(stmt.getClass().getName())
                || "com.mchange.v2.c3p0.impl.NewProxyCallableStatement".equals(stmt.getClass().getName())
                || "com.mchange.v2.c3p0.impl.NewProxyStatement".equals(stmt.getClass().getName())) {
            try {
                realStatement = (Statement) ReflectionUtil.getField(stmt, "inner");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            realStatement = stmt;
        }
        return realStatement;
    }


    public void notifyExecution(String statement, long duration) {
        getSqlMonitor().notifyExecution(statement, duration);
    }

    public void notifyExecution(Statement stmt, long duration) {

        getSqlMonitor().notifyExecution(getSql(unwrapStatement(stmt)), duration);

    }

    public void notifyClose(Statement statement) {
        LinkedList<StatementHolder> stmts = statements.get();
        stmts.remove(getStatementHolder(statement));
    }

    private static List<String> getSql(Statement stmt) {
        for (StatementHolder statementHolder: statements.get()) {
            if (statementHolder.getStatment() == stmt) {
                return statementHolder.getSql();
            }
        }
        return null;
    }

    private StatementHolder getStatementHolder(Statement statement) {
        LinkedList<StatementHolder> stmts = statements.get();
        Iterator<StatementHolder> holderIterator = stmts.iterator();
        while (holderIterator.hasNext()) {
            StatementHolder holder = holderIterator.next();
            if (holder.getStatment() == statement) {
                return holder;
            }
        }
        return null;
    }

    public void clearStatement(Statement stmt) {
        getStatementHolder(stmt).clearSqls();
    }
}
