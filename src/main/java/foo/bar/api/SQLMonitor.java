package foo.bar.api;

import java.util.List;

public interface SQLMonitor {

    void notifyExecution(String sql, long duration);

    void notifyExecution(List<String> sqls, long duration);

    public static class NoOp implements SQLMonitor {

        @Override
        public void notifyExecution(String sql, long duration) {
        }

        @Override
        public void notifyExecution(List<String> sqls, long duration) {
        }
    }
}
