package nablarch.fw.web.handler.health;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * {@link HealthChecker}のテスト。
 */
public class HealthCheckerTest {

    /**
     * ヘルスチェックに成功した場合。
     */
    @Test
    public void success() {
        HealthChecker sut = new HealthChecker() {
            @Override
            protected boolean tryOut(HttpRequest request, ExecutionContext context) {
                return true;
            }
        };
        assertThat(sut.check(null, null), is(true));
    }

    /**
     * ヘルスチェックに失敗した場合。
     */
    @Test
    public void failureByFalse() {
        HealthChecker sut = new HealthChecker() {
            @Override
            protected boolean tryOut(HttpRequest request, ExecutionContext context) {
                return false;
            }
        };
        assertThat(sut.check(null, null), is(false));
    }

    /**
     * ヘルスチェックで例外が発生した場合。
     */
    @Test
    public void failureByException() {
        HealthChecker sut = new HealthChecker() {
            @Override
            protected boolean tryOut(HttpRequest request, ExecutionContext context) {
                throw new IllegalStateException();
            }
        };
        assertThat(sut.check(null, null), is(false));
    }

    /**
     * 名前を設定できること。
     */
    @Test
    public void name() {
        HealthChecker sut = new HealthChecker() {
            @Override
            protected boolean tryOut(HttpRequest request, ExecutionContext context) {
                return true;
            }
        };
        sut.setName("test");
        assertThat(sut.getName(), is("test"));
    }
}