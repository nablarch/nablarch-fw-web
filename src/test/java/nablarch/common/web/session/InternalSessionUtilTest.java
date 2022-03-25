package nablarch.common.web.session;

import nablarch.fw.ExecutionContext;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link InternalSessionUtil}の単体テスト。
 * @author Tanaka Tomoyuki
 */
public class InternalSessionUtilTest {
    /**
     * テスト用のセッションストアID。
     */
    private static final String SESSION_STORE_ID = "test-session-store-id";

    /**
     * 実行コンテキスト。
     */
    private final ExecutionContext context = new ExecutionContext();

    @Test
    public void リクエストスコープにセッションストアIDを保存できることを確認() {
        assertThat(context.getRequestScopedVar(InternalSessionUtil.SESSION_STORE_ID_KEY),
                is(nullValue()));

        InternalSessionUtil.setId(context, SESSION_STORE_ID);

        assertThat((String)context.getRequestScopedVar(InternalSessionUtil.SESSION_STORE_ID_KEY),
                is(SESSION_STORE_ID));
    }

    @Test
    public void リクエストスコープに保存したセッションストアIDを取得できることを確認() {
        assertThat(InternalSessionUtil.getId(context), is(nullValue()));

        context.setRequestScopedVar(InternalSessionUtil.SESSION_STORE_ID_KEY, SESSION_STORE_ID);

        assertThat(InternalSessionUtil.getId(context), is(SESSION_STORE_ID));
    }
}