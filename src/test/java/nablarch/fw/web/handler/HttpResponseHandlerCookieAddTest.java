package nablarch.fw.web.handler;

import nablarch.TestUtil;
import nablarch.core.ThreadContext;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpCookie;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;

/**
 * {@link HttpResponseHandler}のCookie追加のテスト。
 */
public class HttpResponseHandlerCookieAddTest {
    
    private MockedStatic<HttpResponse> mocked;

    @Before
    public void setUp() throws Exception {
        SystemRepository.clear();
        ThreadContext.clear();
        mocked = Mockito.mockStatic(HttpResponse.class, CALLS_REAL_METHODS);
        mocked.when(() -> HttpResponse.parse(anyString())).thenReturn(null);
        mocked.when(() -> HttpResponse.parse(any(byte[].class))).thenReturn(null);
    }

    @After
    public void tearDown() throws Exception {
        mocked.close();
    }

    /**
     * サーブレットフォワードした場合に、Cookieが設定されること。
     */
    @Test
    public void doServletForward() {

        TestUtil.createHttpServer()
                .setWarBasePath("classpath://nablarch/fw/web/handler/httpresponsehandler/cookie")
                .clearHandlers()
                .addHandler(new HttpResponseHandler())
                .addHandler(new HttpRequestHandler() {
                    @Override
                    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                        HttpCookie cookie1 = new HttpCookie();
                        cookie1.setPath("/cookie1");
                        cookie1.put("Test1-Name", "Test1-Value");
                        HttpCookie cookie2 = new HttpCookie();
                        cookie2.setPath("/cookie2");
                        cookie2.put("Test2-Name", "Test2-Value");
                        cookie2.put("Test3-Name", "Test3-Value");
                        return new HttpResponse("index.jsp")
                                .addCookie(cookie1)
                                .addCookie(cookie2);
                    }
                })
                .startLocal()
                .handle(new MockHttpRequest("GET / HTTP/1.1"), new ExecutionContext());

        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        mocked.verify(() -> HttpResponse.parse(captor.capture()), atLeastOnce());

        final byte[] bytes = captor.getValue();
        String messages = StringUtil.toString(bytes, StandardCharsets.UTF_8);
        assertThat(messages, is(containsString("HTTP/1.1 200 OK")));
        assertThat(messages, is(containsString("cookie add test")));
        assertThat(messages, is(containsString("Set-Cookie: Test1-Name=Test1-Value; Path=/cookie1")));
        assertThat(messages, is(containsString("Set-Cookie: Test2-Name=Test2-Value; Path=/cookie2")));
        assertThat(messages, is(containsString("Set-Cookie: Test3-Name=Test3-Value; Path=/cookie2")));
    }

    /**
     * リダイレクトした場合に、Cookieが設定されること。
     */
    @Test
    public void doRedirect() {

        TestUtil.createHttpServer()
                .setWarBasePath("classpath://nablarch/fw/web/handler/httpresponsehandler/cookie")
                .clearHandlers()
                .addHandler(new HttpResponseHandler())
                .addHandler(new HttpRequestHandler() {
                    @Override
                    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                        HttpCookie cookie1 = new HttpCookie();
                        cookie1.setPath("/cookie1");
                        cookie1.put("Test1-Name", "Test1-Value");
                        HttpCookie cookie2 = new HttpCookie();
                        cookie2.setPath("/cookie2");
                        cookie2.put("Test2-Name", "Test2-Value");
                        cookie2.put("Test3-Name", "Test3-Value");
                        return new HttpResponse("redirect://dummy")
                                .addCookie(cookie1)
                                .addCookie(cookie2);
                    }
                })
                .startLocal()
                .handle(new MockHttpRequest("GET / HTTP/1.1"), new ExecutionContext());

        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        mocked.verify(() -> HttpResponse.parse(captor.capture()), atLeastOnce());

        final byte[] bytes = captor.getValue();
        String messages = StringUtil.toString(bytes, StandardCharsets.UTF_8);
        assertThat(messages, is(containsString("HTTP/1.1 302 Found")));
        assertThat(messages, is(containsString("Location: http://127.0.0.1/dummy")));
        assertThat(messages, is(containsString("Set-Cookie: Test1-Name=Test1-Value; Path=/cookie1")));
        assertThat(messages, is(containsString("Set-Cookie: Test2-Name=Test2-Value; Path=/cookie2")));
        assertThat(messages, is(containsString("Set-Cookie: Test3-Name=Test3-Value; Path=/cookie2")));
    }
}
