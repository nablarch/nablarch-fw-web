package nablarch.fw.web.handler;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.secure.FrameOptionsHeader;
import nablarch.fw.web.handler.secure.XssProtectionHeader;
import nablarch.fw.web.servlet.ServletExecutionContext;
import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link SecureHandler}のテストクラス。
 */
public class SecureHandlerTest {

    private final HttpServletRequest mockServletRequest = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);

    private final HttpServletResponse mockServletResponse = mock(HttpServletResponse.class);

    private final ServletContext mockServletContext = mock(ServletContext.class);

    private final HttpRequest mockHttpRequest = mock(HttpRequest.class);

    /** テスト対象 */
    private SecureHandler sut = new SecureHandler();

    private ServletExecutionContext context;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Handler<HttpRequest, HttpResponse> handler = new Handler<HttpRequest, HttpResponse>() {
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

        final HttpResponse result = sut.handle(mockHttpRequest, context);

        assertThat(result.getHeaderMap().size(), is(6));
        assertThat(result.getHeaderMap(), CoreMatchers.<Map<String, String>>allOf(
                IsMapContaining.hasEntry("Content-Length", "0"),
                IsMapContaining.hasEntry("X-Frame-Options", "SAMEORIGIN"),
                IsMapContaining.hasEntry("X-XSS-Protection", "1; mode=block"),
                IsMapContaining.hasEntry("X-Content-Type-Options", "nosniff"),
                IsMapContaining.hasEntry("Referrer-Policy", "strict-origin-when-cross-origin"),
                IsMapContaining.hasEntry("Cache-Control", "no-store")
        ));
        assertNull(result.getHeaderMap().get("Content-Type"));

    }

    /**
     * 出力対象外の場合、そのヘッダは出力されないこと
     */
    @Test
    public void withoutFrameOptions() {

        final FrameOptionsHeader frameOption = new FrameOptionsHeader();
        frameOption.setOption("NONE");
        sut.setSecureResponseHeaderList(Arrays.asList(frameOption, new XssProtectionHeader()));

        final HttpResponse response = sut.handle(mockHttpRequest, context);

        assertThat(response.getHeaderMap(), CoreMatchers.<Map<String, String>>allOf(
                IsMapContaining.hasEntry("X-XSS-Protection", "1; mode=block"),
                not(IsMapContaining.hasKey("X-Frame-Options"))));
    }
}