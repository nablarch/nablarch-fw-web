package nablarch.fw.web.handler.health;

import mockit.Expectations;
import mockit.Mocked;
import nablarch.core.db.dialect.Dialect;
import org.junit.Test;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * {@link DbHealthChecker}のテスト。
 */
public class DbHealthCheckerTest {

    @Mocked
    private DataSource dataSource;

    @Mocked
    private Dialect dialect;

    @Mocked
    private Connection connection;

    @Mocked
    private PreparedStatement statement;

    /**
     * ヘルスチェックに成功した場合。
     */
    @Test
    public void success() throws Exception {

        new Expectations() {{
            dataSource.getConnection();
            result = connection;
            connection.prepareStatement(dialect.getPingSql());
            result = statement;
        }};

        DbHealthChecker sut = new DbHealthChecker();
        sut.setDataSource(dataSource);
        sut.setDialect(dialect);

        assertThat(sut.check(null, null), is(true));
    }

    /**
     * ヘルスチェックに失敗した場合。
     */
    @Test
    public void failureByException() throws Exception {

        new Expectations() {{
            dataSource.getConnection();
            result = connection;
            connection.prepareStatement(dialect.getPingSql());
            result = statement;
            statement.execute();
            result = new SQLException();
        }};

        DbHealthChecker sut = new DbHealthChecker();
        sut.setDataSource(dataSource);
        sut.setDialect(dialect);

        assertThat(sut.check(null, null), is(false));
    }

    /**
     * Connectionが取得できなくてもNPEにならないことを確認。
     */
    @Test
    public void connectionError() throws Exception {

        new Expectations() {{
            dataSource.getConnection();
            result = new SQLException();
        }};

        DbHealthChecker sut = new DbHealthChecker();
        sut.setDataSource(dataSource);
        sut.setDialect(dialect);

        assertThat(sut.check(null, null), is(false));
    }
}