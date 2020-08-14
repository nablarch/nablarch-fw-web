package nablarch.fw.web.handler.secure;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link SecureResponseHeaderSupport}のテスト。
 */
public class SecureResponseHeaderSupportTest {

    class TestHeader extends SecureResponseHeaderSupport {
        /**
         * コンストラクタ。
         */
        public TestHeader() {
            super("Test-Name", "test-default-value");
        }
    }

    @Test
    public void defaultBehavior() {

        SecureResponseHeader sut = new TestHeader();

        assertThat(sut.isOutput(null, null), is(Boolean.TRUE));
        assertThat(sut.getName(), is("Test-Name"));
        assertThat(sut.getValue(), is("test-default-value"));
    }
}
