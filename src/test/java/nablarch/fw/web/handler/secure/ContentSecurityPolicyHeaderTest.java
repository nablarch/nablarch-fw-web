package nablarch.fw.web.handler.secure;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *{@link ContentSecurityPolicyHeader}のテストクラス。
 */
public class ContentSecurityPolicyHeaderTest {

    private final ContentSecurityPolicyHeader sut = new ContentSecurityPolicyHeader();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * デフォルト設定でのテスト。
     */
    @Test
    public void defaultSettings() {
        assertThat(sut.isOutput(null, null), is(true));
        assertThat(sut.getName(), is("Content-Security-Policy"));
    }

    /**
     * policyを設定した場合、値として出力されること。
     */
    @Test
    public void policy() {
        sut.setPolicy("default-src 'self'");
        assertThat(sut.getValue(), is("default-src 'self'"));
    }

    /**
     * report-onlyモードに設定した場合、ヘッダ名がContent-Security-Policy-Report-Onlyになること。
     */
    @Test
    public void reportOnly() {
        sut.setReportOnly(true);
        assertThat(sut.getName(), is("Content-Security-Policy-Report-Only"));
    }

    /**
     * ポリシーが設定されていない場合(null)、例外が発生すること。
     */
    @Test
    public void invalidPolicyNull() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("invalid Content-Security-Policy. policy is null");

        sut.setPolicy(null);
        sut.getValue();
    }

    /**
     * ポリシーが設定されていない場合(空文字)、例外が発生すること。
     */
    @Test
    public void invalidPolicyEmpty() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("invalid Content-Security-Policy. policy is empty");

        sut.setPolicy("");
        sut.getValue();
    }
}
