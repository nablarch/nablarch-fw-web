package nablarch.fw.web.handler.secure;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 *{@link XssProtectionHeader}のテストクラス。
 */
public class XssProtectionHeaderTest {

    private final XssProtectionHeader sut = new XssProtectionHeader();

    /**
     * デフォルト設定でのテスト。
     */
    @Test
    public void defaultSettings() throws Exception {
        assertThat(sut.isOutput(null, null), is(true));
        assertThat(sut.getName(), is("X-XSS-Protection"));
        assertThat(sut.getValue(), is("1; mode=block"));
    }

    /**
     * X-XSS-Protectionを抑制した場合、値には0が出力されること。
     */
    @Test
    public void disabled() throws Exception {
        sut.setEnable(false);
        sut.setMode("");
        assertThat(sut.getValue(), is("0"));
    }

    /**
     * モードに任意の値を設定した場合、その値がモード値として出力されること。
     */
    @Test
    public void customMode() throws Exception {
        sut.setMode("custom");

        assertThat(sut.getValue(), is("1; mode=custom"));
    }
}