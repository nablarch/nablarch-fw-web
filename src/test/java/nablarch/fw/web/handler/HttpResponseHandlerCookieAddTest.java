package nablarch.fw.web.handler;

import mockit.Mocked;
import mockit.Verifications;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpCookie;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link HttpResponseHandler}のCookie追加のテスト。
 */
public class HttpResponseHandlerCookieAddTest {

    @Mocked("parse") HttpResponse unused;

    /**
     * サーブレットフォワードした場合に、Cookieが設定されること。
     */
    @Test
    public void doServletForward() {

        new HttpServer()
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
                .handle(new MockHttpRequest("GET / HTTP/1.1"), null);

        new Verifications() {{
            byte[] bytes;
            HttpResponse.parse(bytes = withCapture());
            String messages = StringUtil.toString(bytes, Charset.forName("UTF-8"));
            assertThat(messages, is(containsString("HTTP/1.1 200 OK")));
            assertThat(messages, is(containsString("cookie add test")));
            assertThat(messages, is(containsString("Set-Cookie: Test1-Name=Test1-Value;Path=/cookie1")));
            assertThat(messages, is(containsString("Set-Cookie: Test2-Name=Test2-Value;Path=/cookie2")));
            assertThat(messages, is(containsString("Set-Cookie: Test3-Name=Test3-Value;Path=/cookie2")));
        }};
    }

    /**
     * リダイレクトした場合に、Cookieが設定されること。
     */
    @Test
    public void doRedirect() {

        new HttpServer()
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
                .handle(new MockHttpRequest("GET / HTTP/1.1"), null);

        new Verifications() {{
            byte[] bytes;
            HttpResponse.parse(bytes = withCapture());
            String messages = StringUtil.toString(bytes, Charset.forName("UTF-8"));
            assertThat(messages, is(containsString("HTTP/1.1 302 Found")));
            assertThat(messages, is(containsString("Location: http://127.0.0.1/dummy")));
            assertThat(messages, is(containsString("Set-Cookie: Test1-Name=Test1-Value;Path=/cookie1")));
            assertThat(messages, is(containsString("Set-Cookie: Test2-Name=Test2-Value;Path=/cookie2")));
            assertThat(messages, is(containsString("Set-Cookie: Test3-Name=Test3-Value;Path=/cookie2")));
        }};
    }
}
