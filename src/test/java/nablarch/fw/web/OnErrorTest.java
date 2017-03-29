package nablarch.fw.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.handler.HttpErrorHandler;
import nablarch.fw.web.interceptor.OnError;
import org.junit.Test;

public class OnErrorTest {

    @Test
    public void testErrorHandling() {
        // HttpMethodBinderによる委譲
        Object action = new Object() {
            @OnError(
                type = IllegalArgumentException.class
              , path = "servlet://error.jsp"
            )
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                Boolean hasError = (Boolean) ctx.getSessionScopedVar("hasError");
                if (hasError == null) {
                    throw new IllegalStateException();
                }
                if (hasError.booleanValue()) {
                    throw new IllegalArgumentException();
                }
                return new HttpResponse().write("<p>Hello world</p>");
                
            }
        };
        HttpRequestHandler handler = new HttpMethodBinding(action);
        ExecutionContext context = new ExecutionContext()
                       .addHandler("//", handler);

        HttpRequest req = new MockHttpRequest("GET /index.html HTTP/1.1");
        HttpResponse res = null;
        
        //アクションから実行時例外が送出される場合。
        //  → OnErrorインターセプタにより捕捉されHttpErrorResponseに振替られる。
        context.setSessionScopedVar("hasError", Boolean.TRUE);
        try {
            context.handleNext(req);
            fail();
        } catch(HttpErrorResponse e) {
            assertTrue(true);
            assertNotNull(e.getCause());
            assertEquals(IllegalArgumentException.class, e.getCause().getClass());
            res = e.getResponse();
        }
        assertNotNull(res);
        assertEquals(400, res.getStatusCode());
        assertEquals("servlet://error.jsp", res.getContentPath().toString());
        
        //  → そのHttpErrorResponseはエラーハンドラによって400系エラーステータス
        //    のレスポンスに振替られる。
        context = new ExecutionContext()
                 .addHandler("//", new HttpErrorHandler())
                 .addHandler("//", handler)
                 .setSessionScopedVar("hasError", Boolean.TRUE);
        
        res = context.handleNext(req);
        assertNotNull(res);
        assertEquals(400, res.getStatusCode());
        assertEquals("servlet://error.jsp", res.getContentPath().toString());
        
        
        //アクションからエラーが送出される場合。
        //  → そのまま送出される。
        context = new ExecutionContext()
                 .addHandler("//", handler)
                 .setSessionScopedVar("hasError", null);
        
        try {
            res = context.handleNext(req);
            fail();
        } catch (IllegalStateException e){
            assertTrue(true);
        }
        
        //アクションが正常終了した場合。
        //  → アクションのレスポンスをそのまま返す。
        context = new ExecutionContext()
                 .addHandler("//", handler)
                 .setSessionScopedVar("hasError", Boolean.FALSE);
        res = context.handleNext(req);
        assertNotNull(res);
        assertEquals(200, res.getStatusCode());
        assertEquals("<p>Hello world</p>", res.getBodyString());
    }

}
