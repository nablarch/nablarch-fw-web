package nablarch.fw.web.handler.secure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import nablarch.fw.web.handler.SecureHandler;
import nablarch.fw.web.servlet.MockServletContext;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.MockServletResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;
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

    /**
     * リクエストスコープにnonceが設定されている場合、nonceのプレースホルダーが置換されてContent-Security-Policyヘッダーが構築されること
     */
    @Test
    public void replaceCspNonceSourcePlaceHolder() {
        sut.setPolicy("script-src 'self' '$cspNonceSource$'; style-src '$cspNonceSource$'");

        assertThat(sut.getValue(), is("script-src 'self' '$cspNonceSource$'; style-src '$cspNonceSource$'"));

        ServletExecutionContext context = new ServletExecutionContext(new MockServletRequest(), new MockServletResponse(), new MockServletContext());
        context.setRequestScopedVar(SecureHandler.CSP_NONCE_KEY, "abcde");

        assertThat(sut.getFormattedValue(context), is("script-src 'self' 'nonce-abcde'; style-src 'nonce-abcde'"));
    }

    /**
     * リクエストスコープにnonceが設定されていない場合、nonceのプレースホルダーが置換されてContent-Security-Policyヘッダーが構築されないこと
     */
    @Test
    public void notReplaceCspNonceSourcePlaceHolder() {
        sut.setPolicy("script-src 'self' '$cspNonceSource$'; style-src '$cspNonceSource$'");

        assertThat(sut.getValue(), is("script-src 'self' '$cspNonceSource$'; style-src '$cspNonceSource$'"));

        ServletExecutionContext context = new ServletExecutionContext(new MockServletRequest(), new MockServletResponse(), new MockServletContext());

        // getValueと同じ値になる
        assertThat(sut.getValue(), is("script-src 'self' '$cspNonceSource$'; style-src '$cspNonceSource$'"));
    }
}
