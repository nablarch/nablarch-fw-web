package nablarch.fw.web.handler;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    public void setUp() throws Exception {
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
     * X-Frame-Optionsヘッダが設定されること。
     */
    @Test
    public void defaultFrameOptions() throws Exception {

        final HttpResponse result = sut.handle(mockHttpRequest, context);

        assertThat(result.getHeaderMap(), CoreMatchers.<Map<String, String>>allOf(
                IsMapContaining.hasEntry("X-Frame-Options", "SAMEORIGIN"),
                IsMapContaining.hasEntry("X-XSS-Protection", "1; mode=block"),
                IsMapContaining.hasEntry("X-Content-Type-Options", "nosniff")
        ));
    }

    /**
     * 出力対象外の場合、そのヘッダは出力されないこと
     */
    @Test
    public void withoutFrameOptions() throws Exception {

        final FrameOptionsHeader frameOption = new FrameOptionsHeader();
        frameOption.setOption("NONE");
        sut.setSecureResponseHeaderList(Arrays.asList(frameOption, new XssProtectionHeader()));

        final HttpResponse response = sut.handle(mockHttpRequest, context);

        assertThat(response.getHeaderMap(), CoreMatchers.<Map<String, String>>allOf(
                IsMapContaining.hasEntry("X-XSS-Protection", "1; mode=block"),
                not(IsMapContaining.hasKey("X-Frame-Options"))));
    }
}