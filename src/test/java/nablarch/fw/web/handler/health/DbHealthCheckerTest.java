package nablarch.fw.web.handler.health;

import nablarch.core.db.dialect.Dialect;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link DbHealthChecker}のテスト。
 */
public class DbHealthCheckerTest {

    private final DataSource dataSource = mock(DataSource.class);

    private final Dialect dialect = mock(Dialect.class);

    private final Connection connection = mock(Connection.class);

    private final PreparedStatement statement = mock(PreparedStatement.class);

    /**
     * ヘルスチェックに成功した場合。
     */
    @Test
    public void success() throws Exception {

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(dialect.getPingSql())).thenReturn(statement);

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

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(dialect.getPingSql())).thenReturn(statement);
        when(statement.execute()).thenThrow(new SQLException());

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

        when(dataSource.getConnection()).thenThrow(new SQLException());

        DbHealthChecker sut = new DbHealthChecker();
        sut.setDataSource(dataSource);
        sut.setDialect(dialect);

        assertThat(sut.check(null, null), is(false));
    }
}