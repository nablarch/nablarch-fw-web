package nablarch.fw.web.servlet;

import nablarch.TestUtil;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpCookie;
import nablarch.fw.web.MockHttpRequest;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link HttpRequestWrapper}のサーブレットコンテナ上でのテストクラス。
 *
 * @author Naoki Yamamoto
 */
public class HttpRequestWrapperOnContainerTest {

    @Before
    public void setUp() throws Exception {
        SystemRepository.clear();
    }

    /**
     * {@link HttpRequestWrapper}の各メソッドがサーブレットコンテナ上でも想定通りに動作することを、
     * {@link HttpServer}を使用して確認するケース。
     *
     * @throws Exception
     */
    @Test
    public void testOnServletContainer() throws Exception {

        HttpServer server = TestUtil.createHttpServer();
        server.setWarBasePath("classpath://");
        server.addHandler(new Object() {
            public HttpResponse getTest(HttpRequestWrapper req, ExecutionContext ctx) {
                assertThat(req.getMethod(), is("GET"));
                assertThat(req.getHttpVersion(), is("HTTP/1.1"));
                assertThat(req.getHost(), is("127.0.0.1"));
                assertThat(req.getRequestUri(), is("/test"));
                assertThat(req.getRequestPath(), is("/test"));
                assertThat(req.getParam("param_key"), is(new String[]{"param_value"}));
                assertThat(req.getParamMap(), is(notNullValue()));
                assertThat(req.getParamMap().size(), is(1));
                assertThat(req.getParamMap().get("param_key"), is(new String[]{"param_value"}));
                assertThat(req.getHeader("header_key"), is("header_value"));
                assertThat(req.getHeaderMap(), is(notNullValue()));
                assertThat(req.getHeaderMap().size(), is(6));
                assertThat(req.getHeaderMap().get("header_key"), is("header_value"));
                assertThat(req.getHeaderMap().get("Content-Length"), is("0"));
                assertThat(req.getHeaderMap().get("Host"), is("127.0.0.1"));
                assertThat(req.getHeaderMap().get("Cookie"), is("cookie_key=cookie_value"));
                assertThat(req.getHeaderMap().get("User-Agent"), is("user_agent_value"));
                assertThat(req.getUserAgent(), is(notNullValue()));
                assertThat(req.getUserAgent().getText(), is("user_agent_value"));
                assertThat(req.getCookie(), is(notNullValue()));
                assertThat(req.getCookie().size(), is(1));
                assertThat(req.getCookie().get("cookie_key"), is("cookie_value"));
                assertThat(req.getCharacterEncoding(), is("UTF-8"));
                assertThat(req.getContentLength(), is(0));
                assertThat(req.getContentType(), is("application/x-www-form-urlencoded; charset=UTF-8"));
                return new HttpResponse();
            }
        }).startLocal();

        MockHttpRequest mockReq = new MockHttpRequest();
        mockReq.setMethod("GET");
        mockReq.setRequestUri("/test?param_key=param_value");
        mockReq.setHttpVersion("HTTP/1.1");
        mockReq.setHeaderMap(new HashMap<String, String>() {{
            put("header_key", "header_value");
            put("User-Agent", "user_agent_value");
            put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        }});
        mockReq.setCookie(new MockHttpCookie() {{
            put("cookie_key", "cookie_value");
        }});

        HttpResponse res = server.handle(mockReq, new ExecutionContext());
        assertThat("Handler内のAssertが全て正常に完了していること", res.getStatusCode(), is(200));
    }

    /**
     * {@link HttpRequestWrapper#getInputStream()}がサーブレットコンテナ上でも想定通りに動作することを、
     * {@link HttpServer}を使用して確認するケース。
     *
     * @throws Exception
     */
    @Test
    public void testInputStream() throws Exception {

        HttpServer server = TestUtil.createHttpServer();
        server.setWarBasePath("classpath://");
        server.addHandler(new Object() {
            public HttpResponse postTest(HttpRequestWrapper req, ExecutionContext ctx) throws Exception {
                assertThat(req.getContentLength(), is("param_key=param_value".length()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
                assertThat(reader.readLine(), is("param_key=param_value"));
                return new HttpResponse();
            }
        }).startLocal();

        MockHttpRequest mockReq = new MockHttpRequest();
        mockReq.setMethod("POST");
        mockReq.setRequestUri("/test");
        mockReq.setParam("param_key", "param_value");

        HttpResponse res = server.handle(mockReq, new ExecutionContext());
        assertThat("Handler内のAssertが全て正常に完了していること", res.getStatusCode(), is(200));
    }
}