package nablarch.fw.web.handler.secure;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * {@link ContentTypeOptionsHeader}のテストクラス。
 */
public class ContentTypeOptionsHeaderTest {

    /** テスト対象 */
    private final ContentTypeOptionsHeader sut = new ContentTypeOptionsHeader();

    /**
     * デフォルト状態でのテスト。
     */
    @Test
    public void defaultSettings() throws Exception {
        assertThat("常に出力対象", sut.isOutput(null, null), is(true));
        assertThat("ヘッダー名", sut.getName(), is("X-Content-Type-Options"));
        assertThat("値", sut.getValue(), is("nosniff"));
    }

    /**
     * ヘッダー値に任意の値を指定した場合、その値が出力されること。
     */
    @Test
    public void customSettings() throws Exception {
        sut.setOption("custom");
        assertThat(sut.getValue(), is("custom"));
    }
}