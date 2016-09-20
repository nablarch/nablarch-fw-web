package nablarch.fw.web.handler.secure;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link FrameOptionsHeader}のテスト。
 */
public class FrameOptionsHeaderTest {

    private final FrameOptionsHeader sut = new FrameOptionsHeader();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * デフォルト状態でのテスト。
     *
     * 設定される値は、SAMEORIGINとなること。
     */
    @Test
    public void defaultSettings() throws Exception {
        assertThat(sut.isOutput(null, null), is(true));

        assertThat(sut.getName(), is("X-Frame-Options"));
        assertThat(sut.getValue(), is("SAMEORIGIN"));
    }

    /**
     * 設定値を変更できること。
     */
    @Test
    public void customSettings() throws Exception {
        sut.setOption("DENY");
        assertThat(sut.isOutput(null, null), is(true));
        assertThat(sut.getValue(), is("DENY"));
    }

    /**
     * NONEを設定した場合、出力対象ではないこと。
     */
    @Test
    public void none() throws Exception {
        sut.setOption("NONE");
        assertThat(sut.isOutput(null, null), is(false));
    }

    @Test
    public void invalidOption() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("invalid X-Frame-Options. value = [invalid]");

        sut.setOption("invalid");
    }
}