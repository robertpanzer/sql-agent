package foo.bar;

import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

/**
 * This class contains the SQL string for a Statement, as this
 * may not be available during execution of of PreparedStatements.
 */
public class StatementHolder {

    private Statement statment;

    private List<String> sqls = new LinkedList<>();

    public StatementHolder(String sql, Statement statment) {
        this.sqls.add(sql);
        this.statment = statment;
    }

    public Statement getStatment() {
        return statment;
    }

    public List<String> getSql() {
        return sqls;
    }

    public void addSql(String sql) {
        sqls.add(sql);
    }

    public void clearSqls() {
        sqls.clear();
    }
}
