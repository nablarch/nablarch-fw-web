package nablarch.fw.web.handler;

import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.secure.FrameOptionsHeader;
import nablarch.fw.web.servlet.ServletExecutionContext;

import org.junit.Before;
import org.junit.Test;

import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

/**
 * {@link HttpResponseHandler}と{@link SecureHandler}を併用した場合に、
 * レスポンスヘッダが正しく書き込めることを確認するテスト。
 */
public class HttpResponseHandlerWithSecureHandlerTest {

    @Mocked
    private HttpServletRequest mockServletRequest;

    @Mocked
    private HttpServletResponse mockServletResponse;

    @Mocked
    private ServletContext mockServletContext;

    @Mocked
    private HttpRequest mockHttpRequest;

    /** テスト対象 */
    private HttpResponseHandler sut = new HttpResponseHandler();

    private ServletExecutionContext context;

    @Before
    public void setUp() throws Exception {
        new Expectations() {{
            mockServletRequest.getRequestURI();
            result = "/sampleapp/action/sample";
            mockServletRequest.getContextPath();
            result = "sampleapp";
            mockServletResponse.encodeRedirectURL(anyString);
            minTimes = 0;
            result = new Delegate<String>() {
                String delegate(String to) {
                    return to;
                }
            };

        }};
        context = new ServletExecutionContext(mockServletRequest, mockServletResponse, mockServletContext);
        context.addHandler(new SecureHandler());
    }

    /**
     * デフォルト設定＆サーブレットフォワードの組み合わせで、ヘッダが移送されることのテスト。
     */
    @Test
    public void defaultConfig_ServletForward() throws Exception {
        context.addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {
                final HttpResponse response = new HttpResponse(200, "servlet://test.jsp");
                response.setHeader("oreore-header", "oreore");
                return response;
            }
        });

        sut.handle(mockHttpRequest, context);

        new Verifications() {
            {
                mockServletRequest.getRequestDispatcher("test.jsp");
                // セキュア関連のヘッダ及び後続ハンドラで設定したヘッダが移送されること
                mockServletResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
                mockServletResponse.setHeader("X-XSS-Protection", "1; mode=block");
                mockServletResponse.setHeader("X-Content-Type-Options", "nosniff");
                mockServletResponse.setHeader("oreore-header", "oreore");
            }
        };
    }

    /**
     * デフォルト設定＆リダイレクトの組み合わせで、ヘッダが移送されることのテスト。
     */
    @Test
    public void defaultConfig_Redirect() throws Exception {
        context.addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {
                final HttpResponse response = new HttpResponse(302, "http://oreore.com");
                response.setHeader("oreore-header", "oreore");
                return response;
            }
        });

        sut.handle(mockHttpRequest, context);

        new Verifications() {{
            // リダイレクト
            mockServletResponse.sendRedirect("http://oreore.com");

            // ヘッダが設定されること
            mockServletResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
            mockServletResponse.setHeader("X-XSS-Protection", "1; mode=block");
            mockServletResponse.setHeader("X-Content-Type-Options", "nosniff");
            mockServletResponse.setHeader("oreore-header", "oreore");
        }};
    }

    /**
     * デフォルト設定で、レスポンスボディを直接書き込む場合のテスト
     */
    @Test
    public void defaultConfig_Direct() throws Exception {
        context.addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {
                final HttpResponse response = new HttpResponse(200);
                response.write("Hello World!!");
                response.setHeader("oreore-header", "oreore");
                return response;
            }
        });

        final HttpResponse response = sut.handle(mockHttpRequest, context);

        new Verifications() {{
            final ServletOutputStream stream = mockServletResponse.getOutputStream();
            stream.write(withInstanceOf(byte[].class), anyInt, anyInt);
            // ヘッダが設定されること
            mockServletResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
            mockServletResponse.setHeader("X-XSS-Protection", "1; mode=block");
            mockServletResponse.setHeader("X-Content-Type-Options", "nosniff");
            mockServletResponse.setHeader("oreore-header", "oreore");
        }};
    }

    /**
     * カスタム設定でのテスト。
     * 後続のハンドラで設定したものだけ移送されること。
     */
    @Test
    public void customConfig_ServletForward() throws Exception {
        context.clearHandlers();
        final SecureHandler secureHandler = new SecureHandler();
        secureHandler.setSecureResponseHeaderList(Collections.singletonList(new FrameOptionsHeader()));
        context.addHandler(secureHandler);
        context.addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {
                final HttpResponse response = new HttpResponse(200, "servlet://test.jsp");
                response.setHeader("oreore-header", "oreore");
                return response;
            }
        });

        sut.handle(mockHttpRequest, context);

        new Verifications() {
            {
                mockServletRequest.getRequestDispatcher("test.jsp");
                // 後続のハンドラで設定したものだけ移送されること
                mockServletResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
                mockServletResponse.setHeader("oreore-header", "oreore");

                // 以下は後続で設定していないもの（デフォルトの場合にだけ設定されるもの）
                mockServletResponse.setHeader("X-XSS-Protection", "1; mode=block");
                times = 0;
                mockServletResponse.setHeader("X-Content-Type-Options", "nosniff");
                times = 0;
            }
        };
    }
}

