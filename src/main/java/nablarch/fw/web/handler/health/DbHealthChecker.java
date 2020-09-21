package nablarch.fw.web.handler.health;

import nablarch.core.db.dialect.Dialect;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * DBのヘルスチェックを行うクラス。
 *
 * SQLを発行し、例外が発生しなければヘルシと判断する。
 * {@link Dialect#getPingSql()}から取得したSQLを発行する。
 *
 * @author Kiyohito Itoh
 */
public class DbHealthChecker extends HealthChecker {

    private DataSource dataSource;
    private Dialect dialect;

    public DbHealthChecker() {
        setName("DB");
    }

    @Override
    protected boolean tryOut(HttpRequest request, ExecutionContext context) throws Exception {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(dialect.getPingSql());
            statement.execute();
            return true;
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * データソースを設定する。
     * @param dataSource データソース
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * ダイアレクトを設定する。
     * @param dialect ダイアレクト
     */
    public void setDialect(Dialect dialect) {
        this.dialect = dialect;
    }
}
