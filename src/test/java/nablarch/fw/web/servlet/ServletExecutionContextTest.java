package nablarch.fw.web.servlet;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;

import mockit.Expectations;
import mockit.Mocked;
import nablarch.TestUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import org.junit.Assert;
import org.junit.Test;

public class ServletExecutionContextTest {

    @Test
    public void testSessionInvalidation() {
        final List<String> holder = new ArrayList<String>();
        HttpServer server = TestUtil.createHttpServer()
                .addHandler("//1", new HttpRequestHandler() {
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        assertFalse("初期状態はセッションなし。", ctx.hasSession());
                        assertFalse("hasSessionでは作成されない。", ctx.hasSession());
                        assertThat("セッションがなければnull", ctx.getSessionScopedVar("test"), nullValue());
                        assertFalse("getSessionScopedVarでは作成されない。", ctx.hasSession());
                        ctx.getSessionScopeMap();
                        assertTrue("getSessionScopeMapでは生成される。", ctx.hasSession());

                        ctx.invalidateSession();

                        ctx.setSessionScopedVar("loginid", "anonymous");
                        assertTrue("setSessionでは生成される。", ctx.hasSession());
                        Assert.assertEquals(1, ctx.getSessionScopeMap().size());
                        Assert.assertEquals(2, ctx.getRequestScopeMap().size()); //このタイミングでは、org.mortbay.jetty.newSessionId と nablarch_http_status_convert_mode が存在する。
                        ctx.setRequestScopedVar("reqId", "req1");
                        Assert.assertEquals(3, ctx.getRequestScopeMap().size());

                        ctx.invalidateSession();
                        Assert.assertEquals(0, ctx.getSessionScopeMap().size());
                        ctx.setSessionScopedVar("loginid", "0001");
                        Assert.assertEquals(1, ctx.getSessionScopeMap().size());
                        String newId = ctx.getSessionScopedVar("loginid");
                        holder.add(newId);

                        Assert.assertEquals(3, ctx.getRequestScopeMap().size());
                        Assert.assertEquals("req1", ctx.getRequestScopedVar("reqId"));

                        return new HttpResponse();
                    }
                })
                .addHandler("//2", new HttpRequestHandler() {
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {

                        Assert.assertNull(ctx.getRequestScopedVar("reqId"));

                        return new HttpResponse();
                    }
                })
                .startLocal();

        HttpResponse res = server.handle(
                new MockHttpRequest("GET /1 HTTP/1.1"), new ExecutionContext()
        );
        Assert.assertEquals(200, res.getStatusCode());
        Assert.assertEquals(1, holder.size());
        Assert.assertEquals("0001", holder.get(0));

        res = server.handle(
                new MockHttpRequest("GET /2 HTTP/1.1"), new ExecutionContext()
        );
        Assert.assertEquals(200, res.getStatusCode());
    }


    @Test
    public void testIsNewSession(
            @Mocked final NablarchHttpServletRequestWrapper servletReq,
            @Mocked final HttpServletResponse servletRes,
            @Mocked final ServletContext servletCtx) {

        new Expectations() {{
            servletReq.getRequestURI(); result = "test";
            servletReq.getContextPath(); result = "test";
        }};
        ServletExecutionContext ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);

        // セッションが新規の場合、trueが返されることを確認
        new Expectations() {{
            servletReq.getSession(true).isNew(); result = true;
        }};
        assertThat(ctx.isNewSession(), is(true));
        
        // セッションが新規でない場合、falseが返されることを確認
        new Expectations() {{
            servletReq.getSession(true).isNew(); result = false;
        }};
        assertThat(ctx.isNewSession(), is(false));
    }
    
    @Test
    public void testSetRequestScopeMap(
            @Mocked final NablarchHttpServletRequestWrapper servletReq,
            @Mocked final HttpServletResponse servletRes,
            @Mocked final ServletContext servletCtx) {

        new Expectations() {{
            servletReq.getRequestURI(); result = "test";
            servletReq.getContextPath(); result = "test";
        }};
        ServletExecutionContext ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        
        final Map<String, Object> scope = new HashMap<String, Object>();
        scope.put("test1", "12345");
        scope.put("test2", "67890");
        
        // 同一の引数が渡されることを確認
        new Expectations() {{
            servletReq.setScope(scope);
        }};
        ExecutionContext result = ctx.setRequestScopeMap(scope);
        
        // ServletExecutionContextのインスタンスであることを確認
        assertThat(result, instanceOf(ServletExecutionContext.class));
    }

}
