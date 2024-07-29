package nablarch.fw.web.handler;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mockit.Verifications;
import nablarch.core.util.Base64Util;
import nablarch.fw.web.handler.secure.ContentSecurityPolicyHeader;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
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

import mockit.Expectations;
import mockit.Mocked;

/**
 * {@link SecureHandler}のテストクラス。
 */
public class SecureHandlerTest {

    @Mocked
    private HttpServletRequest mockServletRequest;

    @Mocked
    private HttpServletResponse mockServletResponse;

    @Mocked
    private ServletContext mockServletContext;

    @Mocked
    private HttpRequest mockHttpRequest;

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
        new Expectations() {{
            mockServletRequest.getRequestURI();
            result = "/sampleapp/action/sample";
            mockServletRequest.getContextPath();
            result = "sampleapp";
        }};

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
        new Expectations() {{
            mockServletRequest.getAttribute(SecureHandler.CSP_NONCE_KEY);
            result = "abcde";
        }};

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

        new Verifications() {{
            mockServletRequest.setAttribute(SecureHandler.CSP_NONCE_KEY, withArgThat(new TypeSafeMatcher<String>() {
                @Override
                protected boolean matchesSafely(String item) {
                    byte[] binary = Base64Util.decode(item);
                    return binary.length == 16;
                }

                @Override
                public void describeTo(Description description) {
                    description.appendText("Base64でエンコードされた16バイト（128ビット）の文字列をnonceとして指定する必要があります");
                }
            }));
            times = 1;
        }};
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

        new Verifications() {{
            mockServletRequest.setAttribute(SecureHandler.CSP_NONCE_KEY, any);
            times = 0;
        }};
    }
}