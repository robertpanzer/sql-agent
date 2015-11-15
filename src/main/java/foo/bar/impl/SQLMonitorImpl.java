package foo.bar.impl;

import foo.bar.api.SQLMonitor;

import java.util.List;
import java.util.logging.Logger;


/**
 * This is the "user" code that reacts on SQL executions.
 * The whole management of PreparedStatements etc is done in the SQLAgent* classes.
 */
public class SQLMonitorImpl implements SQLMonitor {

    private static final Logger LOG = Logger.getLogger("SQLLogger");

    @Override
    public void notifyExecution(String sql, long duration) {
        LOG.info(String.format("SQL %s took %d ns\n", sql, duration));
    }

    @Override
    public void notifyExecution(List<String> sqls, long duration) {
        LOG.info(String.format("SQL %s took %d ns\n", sqls, duration));
    }
}
