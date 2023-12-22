package nablarch.fw.web.handler;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nablarch.core.ThreadContext;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.secure.FrameOptionsHeader;
import nablarch.fw.web.servlet.ServletExecutionContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link HttpResponseHandler}と{@link SecureHandler}を併用した場合に、
 * レスポンスヘッダが正しく書き込めることを確認するテスト。
 */
public class HttpResponseHandlerWithSecureHandlerTest {

    private final HttpServletRequest mockServletRequest = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);

    private final HttpServletResponse mockServletResponse = mock(HttpServletResponse.class);

    private final ServletContext mockServletContext = mock(ServletContext.class);

    private final HttpRequest mockHttpRequest = mock(HttpRequest.class);

    /** テスト対象 */
    private HttpResponseHandler sut = new HttpResponseHandler();

    private ServletExecutionContext context;

    @Before
    public void setUp() throws Exception {
        ThreadContext.clear();
        SystemRepository.clear();
        when(mockServletRequest.getRequestURI()).thenReturn("/sampleapp/action/sample");
        when(mockServletRequest.getContextPath()).thenReturn("sampleapp");
        when(mockServletResponse.encodeRedirectURL(anyString())).then(context -> context.getArgument(0));
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

        verify(mockServletRequest, atLeastOnce()).getRequestDispatcher("test.jsp");
        // セキュア関連のヘッダ及び後続ハンドラで設定したヘッダが移送されること
        verify(mockServletResponse, atLeastOnce()).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(mockServletResponse, atLeastOnce()).setHeader("X-XSS-Protection", "1; mode=block");
        verify(mockServletResponse, atLeastOnce()).setHeader("X-Content-Type-Options", "nosniff");
        verify(mockServletResponse, atLeastOnce()).setHeader("oreore-header", "oreore");
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

        // リダイレクト
        verify(mockServletResponse, atLeastOnce()).sendRedirect("http://oreore.com");

        // ヘッダが設定されること
        verify(mockServletResponse, atLeastOnce()).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(mockServletResponse, atLeastOnce()).setHeader("X-XSS-Protection", "1; mode=block");
        verify(mockServletResponse, atLeastOnce()).setHeader("X-Content-Type-Options", "nosniff");
        verify(mockServletResponse, atLeastOnce()).setHeader("oreore-header", "oreore");
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

        final ServletOutputStream stream = mock(ServletOutputStream.class);
        when(mockServletResponse.getOutputStream()).thenReturn(stream);

        final HttpResponse response = sut.handle(mockHttpRequest, context);

        verify(stream, atLeastOnce()).write(any(byte[].class), anyInt(), anyInt());
        // ヘッダが設定されること
        verify(mockServletResponse, atLeastOnce()).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(mockServletResponse, atLeastOnce()).setHeader("X-XSS-Protection", "1; mode=block");
        verify(mockServletResponse, atLeastOnce()).setHeader("X-Content-Type-Options", "nosniff");
        verify(mockServletResponse, atLeastOnce()).setHeader("oreore-header", "oreore");
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

        verify(mockServletRequest, atLeastOnce()).getRequestDispatcher("test.jsp");
        // 後続のハンドラで設定したものだけ移送されること
        verify(mockServletResponse, atLeastOnce()).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(mockServletResponse, atLeastOnce()).setHeader("oreore-header", "oreore");

        // 以下は後続で設定していないもの（デフォルトの場合にだけ設定されるもの）
        verify(mockServletResponse, never()).setHeader("X-XSS-Protection", "1; mode=block");
        verify(mockServletResponse, never()).setHeader("X-Content-Type-Options", "nosniff");
    }
}

