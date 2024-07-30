package nablarch.fw.web.handler;

import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import nablarch.core.util.Base64Util;
import nablarch.fw.web.handler.secure.ContentSecurityPolicyHeader;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;


/**
 * {@link SecureHandler}のテストクラス。
 */
public class SecureHandlerTest {

    private final HttpServletRequest mockServletRequest = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);

    private final HttpServletResponse mockServletResponse = mock(HttpServletResponse.class);

    private final ServletContext mockServletContext = mock(ServletContext.class);

    private final HttpRequest mockHttpRequest = mock(HttpRequest.class);

    private ServletExecutionContext context;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final Handler<HttpRequest, HttpResponse> handler = new Handler<HttpRequest, HttpResponse>() {
        @Override
        public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {
            return new HttpResponse(200);
        }
    };

    @Before
    public void setUp() {
        when(mockServletRequest.getRequestURI()).thenReturn("/sampleapp/action/sample");
        when(mockServletRequest.getContextPath()).thenReturn("sampleapp");
        context = new ServletExecutionContext(mockServletRequest, mockServletResponse, mockServletContext);
        context.addHandler(handler);
    }

    /**
     * デフォルト設定のテスト。
     */
    @Test
    public void defaultSettings() {
        final SecureHandler sut = new SecureHandler();

        final HttpResponse result = sut.handle(mockHttpRequest, context);

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

        final HttpResponse response = sut.handle(mockHttpRequest, context);

        assertThat(response.getHeaderMap(), CoreMatchers.<Map<String, String>>allOf(
                IsMapContaining.hasEntry("X-XSS-Protection", "1; mode=block"),
                not(IsMapContaining.hasKey("X-Frame-Options"))));
    }

    /**
     * generateCspNonceをtrueにしている場合、リクエストスコープに生成されたnonceが保存されること。
     * またContentSecurityPolicyHeaderを設定している場合は、プレースホルダーが生成されたnonceで置換されること。
     */
    @Test
    public void enableGenerateCspNonce() {
        when(mockServletRequest.getAttribute(SecureHandler.CSP_NONCE_KEY)).thenReturn("abcde");

        final SecureHandler sut = new SecureHandler();
        sut.setGenerateCspNonce(true);

        final ContentSecurityPolicyHeader contentSecurityPolicy = new ContentSecurityPolicyHeader();
        contentSecurityPolicy.setPolicy("script-src 'self' '$cspNonceSource$'; style-src '$cspNonceSource$'");
        sut.setSecureResponseHeaderList(Arrays.asList(new XssProtectionHeader(), contentSecurityPolicy));

        final HttpResponse response = sut.handle(mockHttpRequest, context);

        assertThat(response.getHeaderMap(), CoreMatchers.<Map<String, String>>allOf(
                IsMapContaining.hasEntry("X-XSS-Protection", "1; mode=block"),
                IsMapContaining.hasEntry("Content-Security-Policy", "script-src 'self' 'nonce-abcde'; style-src 'nonce-abcde'")
        ));
        verify(mockServletRequest, times(2)).getAttribute(SecureHandler.CSP_NONCE_KEY);
        verify(mockServletRequest, times(1)).setAttribute(eq(SecureHandler.CSP_NONCE_KEY), assertArg(item -> {
            assertThat("Base64でエンコードされた16バイト（128ビット）の文字列をnonceとして指定する必要があります", Base64Util.decode(item.toString()).length, is(16));
        }));
    }

    /**
     * デフォルト設定の場合、nonceが生成されないこと。
     */
    @Test
    public void disableGenerateCspNonce(){

        final SecureHandler sut = new SecureHandler();
        assertThat(sut.isGenerateCspNonce(), is(false));

        final ContentSecurityPolicyHeader contentSecurityPolicy = new ContentSecurityPolicyHeader();
        contentSecurityPolicy.setPolicy("script-src 'self' '$cspNonceSource$'; style-src '$cspNonceSource$'");
        sut.setSecureResponseHeaderList(Arrays.asList(new XssProtectionHeader(), contentSecurityPolicy));

        final HttpResponse response = sut.handle(mockHttpRequest, context);

        assertThat(response.getHeaderMap(), CoreMatchers.<Map<String, String>>allOf(
                IsMapContaining.hasEntry("X-XSS-Protection", "1; mode=block"),
                IsMapContaining.hasEntry("Content-Security-Policy", "script-src 'self' '$cspNonceSource$'; style-src '$cspNonceSource$'")
        ));
        verify(mockServletRequest, times(1)).getAttribute(SecureHandler.CSP_NONCE_KEY);
        verify(mockServletRequest, never()).setAttribute(eq(SecureHandler.CSP_NONCE_KEY), any());
    }
}