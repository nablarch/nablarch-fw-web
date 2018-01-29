package nablarch.fw.web.handler;

import nablarch.core.ThreadContext;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.MockHttpRequest;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class HttpRequestRewriteRuleTest {
    
    HttpRequestRewriteRule rule    = null;
    ExecutionContext       context = null;
    HttpRequest            request = null;
    
    /**
     * リクエストの単純書き換え
     */
    @Test
    public void testRewritingPath() {
        
        rule = new HttpRequestRewriteRule()
                  .setPattern("^(/app)/m/(.*)")
                  .setRewriteTo("${1}/${2}");
        
        context = new ExecutionContext();
        request = new MockHttpRequest("GET /app/m/index.html HTTP/1.1");
        
        rule.rewrite(request, context);
        
        assertEquals("/app/index.html", request.getRequestPath());
        
        
        //パターンに合致しなければ置換は発生しない。
        context = new ExecutionContext();
        request = new MockHttpRequest("GET /app/pages/list.html HTTP/1.1");

        rule.rewrite(request, context);

        assertEquals("/app/pages/list.html", request.getRequestPath());
    }
    
    @Test
    public void testExecutesRewritingIfItSatisfiesConditions() {
        rule = new HttpRequestRewriteRule()
                  .addCondition("%{paramNames}  nablarch_uri_override_(.[^,]+)")
                  .addCondition("%{httpVersion} HTTP/(1.1)")
                  .addCondition("%{httpMethod}  (GET|POST)")
                  .setPattern(".*")
                  .setRewriteTo("${paramNames:1}");

        context = new ExecutionContext();
        request = new MockHttpRequest("GET /app/index.html?nablarch_uri_override_mobile-login.html= HTTP/1.1");

        rule.rewrite(request, context);
        assertEquals("mobile-login.html", request.getRequestPath());

        // パターンに合致しなければ置換は発生しない。
        context = new ExecutionContext();
        request = new MockHttpRequest("GET /app/index.html?param1=val1 HTTP/1.1");
      
        rule.rewrite(request, context);
        assertEquals("/app/index.html", request.getRequestPath());
    }

    @Test
    public void testInvertMatch() {
        rule = new HttpRequestRewriteRule()
                  .addCondition("!%{thread:USER_ID} ^admin.*$")
                  .setPattern("^/admin/.*")
                  .setRewriteTo("servlet://jsp/unauthorized.jsp")
                  .addExport("%{session:trace}  ${thread:USER_ID} > ${0}");

        request = new MockHttpRequest("GET /admin/page HTTP/1.1");
        context = new ExecutionContext();
        ThreadContext.setUserId("oreore");
        
        String rewrittenPath = rule.rewrite(request, context);
        
        assertEquals("servlet://jsp/unauthorized.jsp", rewrittenPath);
        assertEquals("oreore > /admin/page", context.getSessionScopedVar("trace"));
        
        
        
        ThreadContext.setUserId("admin");
        context = new ExecutionContext();
        rewrittenPath = rule.rewrite(request, context);
        
        assertNull(rewrittenPath); // 置換なし
        assertNull(context.getSessionScopedVar("trace")); //変数展開もなし
        
    }
    

    
    /**
     * 各種変数への書き出し
     */
    @Test
    public void testExportingVariables() {
        
        Map<String, Object> sessionScope = new HashMap<String, Object>();
        
        rule = new HttpRequestRewriteRule()
              .setPattern("/app/user/([_A-Z]+)/([a-z]+)\\.do")
              .setRewriteTo("${0}") //そのまま
              .addExport("%{request:prevPage}  ${header:Referer}")
              .addExport("%{thread:USER_ID}    ${1}")
              .addExport("%{session:actionLog} ${session:actionLog}|${1}/${2}")
              .addExport("%{param:misc}        ${request:reqVar}/${session:sessVar}/${thread:threadVar}/${unknown}")
              .addExport("%{param:param}       ${param:paramVar}")
              .addExport("%{unknown}           unknown")
              .addExport("%{unknown:nothing}   nothing")    // 存在しないスコープ・変数を指定した場合は何もしない。(エラーにはならない。)
              .addExport("%{request:nothing}   ${unknown:nothing}"); // 存在しない変数を埋め込んだ場合は空文字に展開される。
        
        ThreadContext.clear();
        context = new ExecutionContext()
                     .setSessionScopeMap(sessionScope)
                     .setRequestScopedVar("reqVar", "request")
                     .setSessionScopedVar("sessVar", "session");
        ThreadContext.setObject("threadVar", "thread");

        request = new MockHttpRequest("POST /app/user/IWAUO/update.do HTTP/1.1");
        request.getHeaderMap().put("Referer", "http://example.com/app/user/confirm.jsp");
        request.setParam("paramVar", "param");
        
        rule.rewrite(request, context);
        
        assertEquals("/app/user/IWAUO/update.do", request.getRequestPath());
        assertEquals("http://example.com/app/user/confirm.jsp",
                     context.getRequestScopedVar("prevPage"));
        assertEquals("IWAUO", ThreadContext.getUserId());
        assertEquals("|IWAUO/update", context.getSessionScopedVar("actionLog"));
        assertEquals("", context.getRequestScopedVar("nothing"));
        assertEquals("request/session/thread/", request.getParam("misc")[0]);
        
        
        
        ThreadContext.clear();
        context = new ExecutionContext()
                     .setSessionScopeMap(sessionScope);
        request = new MockHttpRequest("POST /app/user/KATUO/delete.do HTTP/1.1");
        request.getHeaderMap().put("Referer", "http://example.com/app/user/list.jsp");
        
        rule.rewrite(request, context);
        
        assertEquals("/app/user/KATUO/delete.do",  request.getRequestPath());
        assertEquals("http://example.com/app/user/list.jsp",
                     context.getRequestScopedVar("prevPage"));
        assertEquals("KATUO",                      ThreadContext.getUserId());
        assertEquals("|IWAUO/update|KATUO/delete", context.getSessionScopedVar("actionLog"));
        
        
        // パターンがマッチしなかった場合は変数の設定は行われない。
        ThreadContext.clear();
        context = new ExecutionContext()
                     .setSessionScopeMap(sessionScope);
        request = new MockHttpRequest("GET /app/user/list.do HTTP/1.1"); //マッチしない
        
        rule.rewrite(request, context);
        
        assertEquals("/app/user/list.do",  request.getRequestPath());
        assertNull  (context.getRequestScopedVar("action"));
        assertNull  (ThreadContext.getUserId());
        assertEquals("|IWAUO/update|KATUO/delete", context.getSessionScopedVar("actionLog"));
    }
    
    @Test
    public void testThrowsErrorIfIllegalPropertyIsPassed() {
        try {
            rule = new HttpRequestRewriteRule()
                  .addCondition("%{invalid:format"); //とじてない。
            fail();
        } catch (Throwable e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
        
        try {
            rule = new HttpRequestRewriteRule()
                  .addExport("%{invalid:format:1} ${hoge:fuga}"); //変数名にバックトラック番号はダメ
            fail();
        } catch (Throwable e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
   
        
        try {
            rule = new HttpRequestRewriteRule()
                      .setPattern("");
            fail();
        } catch (Throwable e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
        
        try {
            rule = new HttpRequestRewriteRule()
                      .setRewriteTo(null);
            fail();
        } catch (Throwable e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
        
        try {
            rule = new HttpRequestRewriteRule()
                      .addExport("!%{httpVersion} HTTP/1\\1"); //ExportにinversionMatchは無い。
            fail();
        } catch (Throwable e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
        
    }


    /**
     * リクエストURLに$が含まれる場合でも、リクエストパスの書き換えが行われること。
     * (#11699)
     */
    @Test
    public void testRequestContainsDollarMark() {
        HttpRequestRewriteRule rule = new HttpRequestRewriteRule()
                .setPattern("^(/app)/ignore/(.*)")
                .setRewriteTo("${1}/${2}");
        HttpRequest request = new MockHttpRequest("GET /app/ignore/in$dex.html HTTP/1.1");
        String rewritten = rule.rewrite(request, new ExecutionContext());

        String expected = "/app/in$dex.html";
        assertThat(rewritten, is(expected));
        assertThat(request.getRequestPath(), is(expected));
    }

}
