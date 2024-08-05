package nablarch.fw.web.handler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Map;

import nablarch.core.util.Base64Util;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.handler.secure.ContentSecurityPolicyHeader;
import nablarch.test.support.web.servlet.MockServletRequest;
import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsMapContaining;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.secure.FrameOptionsHeader;
import nablarch.fw.web.handler.secure.XssProtectionHeader;
import nablarch.fw.web.servlet.ServletExecutionContext;

import org.junit.Before;
import org.junit.Test;

/**
 * {@link SecureHandler}のテストクラス。
 */
public class SecureHandlerTest {

    private ServletExecutionContext context;

    private final Handler<HttpRequest, HttpResponse> dummyHandler = new Handler<HttpRequest, HttpResponse>() {
        @Override
        public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {
            return new HttpResponse(200);
        }
    };

    @Before
    public void setUp() {
        context = new ServletExecutionContext(new MockServletRequest(), null, null);
        context.addHandler(dummyHandler);
    }

    /**
     * デフォルト設定のテスト。
     */
    @Test
    public void defaultSettings() {
        final SecureHandler sut = new SecureHandler();

        final HttpResponse result = sut.handle(null, context);

        assertThat(result.getHeaderMap().size(), is(6));
        assertThat(result.getHeaderMap(), CoreMatchers.<Map<String, String>>allOf(
                IsMapContaining.hasEntry("Content-Length", "0"),
                IsMapContaining.hasEntry("X-Frame-Options", "SAMEORIGIN"),
                IsMapContaining.hasEntry("X-XSS-Protection", "1; mode=block"),
                IsMapContaining.hasEntry("X-Content-Type-Options", "nosniff"),
                IsMapContaining.hasEntry("Referrer-Policy", "strict-origin-when-cross-origin"),
                IsMapContaining.hasEntry("Cache-Control", "no-store"),
                not(IsMapContaining.hasKey("Content-Security-Policy"))
        ));
        assertNull(result.getHeaderMap().get("Content-Type"));
    }

    /**
     * 出力対象外の場合、そのヘッダは出力されないこと。
     */
    @Test
    public void withoutFrameOptions() {
        final SecureHandler sut = new SecureHandler();

        final FrameOptionsHeader frameOption = new FrameOptionsHeader();
        frameOption.setOption("NONE");
        sut.setSecureResponseHeaderList(Arrays.asList(frameOption, new XssProtectionHeader()));

        final HttpResponse response = sut.handle(null, context);

        assertThat(response.getHeaderMap(), CoreMatchers.<Map<String, String>>allOf(
                IsMapContaining.hasEntry("X-XSS-Protection", "1; mode=block"),
                not(IsMapContaining.hasKey("X-Frame-Options"))));
    }

    /**
     * 次のハンドラへリクエストを伝搬し、ハンドラからのレスポンスを伝搬している。
     */
    @Test
    public void nextHandler() {
        final SecureHandler sut = new SecureHandler();
        final HttpRequest mockRequest = new MockHttpRequest();
        final HttpResponse mockResponse = new HttpResponse(200);
        context.clearHandlers();
        context.addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {
                assertThat(request, sameInstance(mockRequest));
                return mockResponse;
            }
        });

        final HttpResponse result = sut.handle(mockRequest, context);

        assertThat(context.getHandlerQueue(), is(empty()));
        assertThat(result, sameInstance(mockResponse));
    }

    /**
     * generateCspNonceをtrueにしている場合、リクエストスコープに生成されたnonceが保存されること。
     */
    @Test
    public void enableGenerateCspNonce() {
        final SecureHandler sut = new SecureHandler();
        sut.setGenerateCspNonce(true);

        sut.handle(null, context);

        String nonce = context.getRequestScopedVar(SecureHandler.CSP_NONCE_KEY);
        assertThat(nonce, is(notNullValue()));
        assertThat("Base64でエンコードされた16バイト（128ビット）の文字列をnonceとして指定する必要があります", Base64Util.decode(nonce).length, is(16));
    }

    /**
     * nonceを生成する場合、同じ値で生成されないこと。
     */
    @Test
    public void randomGenerateCspNonce() {
        final SecureHandler sut = new SecureHandler();
        sut.setGenerateCspNonce(true);

        sut.handle(null, context);

        ServletExecutionContext context2 = new ServletExecutionContext(new MockServletRequest(), null, null);
        context2.addHandler(dummyHandler);
        sut.handle(null, context2);

        String firstNonce = context.getRequestScopedVar(SecureHandler.CSP_NONCE_KEY);
        String secondNonce = context2.getRequestScopedVar(SecureHandler.CSP_NONCE_KEY);
        assertThat(firstNonce, not(is(secondNonce)));
    }

    /**
     * nonceが生成されている場合は、ContentSecurityPolicyHeaderのプレースホルダーが生成されたnonceで置換されること。
     */
    @Test
    public void replacePlaceholderIfEnabledGenerateCspNonce() {
        final SecureHandler sut = new SecureHandler();
        sut.setGenerateCspNonce(true);

        final ContentSecurityPolicyHeader contentSecurityPolicy = new ContentSecurityPolicyHeader();
        contentSecurityPolicy.setPolicy("script-src 'self' '$cspNonceSource$'; style-src '$cspNonceSource$'");
        sut.setSecureResponseHeaderList(Arrays.asList(new XssProtectionHeader(), contentSecurityPolicy));

        final HttpResponse response = sut.handle(null, context);

        String nonce = context.getRequestScopedVar(SecureHandler.CSP_NONCE_KEY);
        assertThat(response.getHeaderMap(), CoreMatchers.<Map<String, String>>allOf(
                IsMapContaining.hasEntry("X-XSS-Protection", "1; mode=block"),
                IsMapContaining.hasEntry("Content-Security-Policy", "script-src 'self' 'nonce-" + nonce + "'; style-src 'nonce-" + nonce + "'")
        ));
    }

    /**
     * デフォルト設定の場合、nonceが生成されないこと。
     */
    @Test
    public void disableGenerateCspNonceIfDefault() {
        final SecureHandler sut = new SecureHandler();

        sut.handle(null, context);

        assertThat(context.getRequestScopeMap(),  not(hasKey(SecureHandler.CSP_NONCE_KEY)));
    }

    /**
     * nonceが生成されていない場合は、ContentSecurityPolicyHeaderのプレースホルダーが置換されないこと。
     */
    @Test
    public void notReplacePlaceholderIfDefault() {
        final SecureHandler sut = new SecureHandler();

        final ContentSecurityPolicyHeader contentSecurityPolicy = new ContentSecurityPolicyHeader();
        contentSecurityPolicy.setPolicy("script-src 'self' '$cspNonceSource$'; style-src '$cspNonceSource$'");
        sut.setSecureResponseHeaderList(Arrays.asList(new XssProtectionHeader(), contentSecurityPolicy));

        final HttpResponse response = sut.handle(null, context);

        String nonce = context.getRequestScopedVar(SecureHandler.CSP_NONCE_KEY);
        assertThat(response.getHeaderMap(), CoreMatchers.<Map<String, String>>allOf(
                IsMapContaining.hasEntry("X-XSS-Protection", "1; mode=block"),
                IsMapContaining.hasEntry("Content-Security-Policy", "script-src 'self' '$cspNonceSource$'; style-src '$cspNonceSource$'")
        ));
    }
}