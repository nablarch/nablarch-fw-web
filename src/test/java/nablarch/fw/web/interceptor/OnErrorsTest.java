package nablarch.fw.web.interceptor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import nablarch.core.message.ApplicationException;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpMethodBinding;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import org.junit.Test;

/**
 * {@link OnErrors}テスト。
 * @author Kiyohito Itoh
 */
public class OnErrorsTest {
    
    @Test
    public void testErrorHandling() {

        HttpRequest req = new MockHttpRequest("GET /index.html HTTP/1.1");
        ExecutionContext context = null;
        HttpResponse res = null;
        
        Object action = new Object() {
            @OnErrors({
                @OnError(type = ApplicationException.class, path = "servlet://input.jsp"),
                @OnError(type = IllegalArgumentException.class, path = "servlet://error.jsp", statusCode = 404),
                @OnError(type = RuntimeException.class, path = "servlet://runtime.jsp", statusCode = 500)
            })
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                RuntimeException re = (RuntimeException) ctx.getSessionScopedVar("re");
                if (re != null) {
                    throw re;
                }
                return new HttpResponse("servlet://success.jsp");
                
            }
        };
        HttpRequestHandler handler = new HttpMethodBinding(action);
        
        // 例外が発生しない場合
        context = new ExecutionContext()
                        .addHandler("//", handler);
        res = context.handleNext(req);
        assertThat(res.getStatusCode(), is(200));
        assertThat(res.getContentPath().toString(), is("servlet://success.jsp"));
        
        // IllegalStateExceptionが発生する場合
        context = new ExecutionContext()
                        .addHandler("//", handler)
                        .setSessionScopedVar("re", new IllegalStateException());
        try {
            res = context.handleNext(req);
            fail();
        } catch (HttpErrorResponse e) {
            res = e.getResponse();
            assertThat(res.getStatusCode(), is(500));
            assertThat(res.getContentPath().toString(), is("servlet://runtime.jsp"));
        }
        
        // RuntimeExceptionが発生する場合
        context = new ExecutionContext()
                        .addHandler("//", handler)
                        .setSessionScopedVar("re", new RuntimeException());
        try {
            res = context.handleNext(req);
            fail();
        } catch (HttpErrorResponse e) {
            res = e.getResponse();
            assertThat(res.getStatusCode(), is(500));
            assertThat(res.getContentPath().toString(), is("servlet://runtime.jsp"));
        }
        
        // Errorが発生する場合
        context = new ExecutionContext()
                        .addHandler("//", handler)
                        .setSessionScopedVar("re", new Error());
        try {
            res = context.handleNext(req);
            fail();
        } catch (HttpErrorResponse e) {
            res = e.getResponse();
            assertThat(res.getStatusCode(), is(500));
            assertThat(res.getContentPath().toString(), is("servlet://runtime.jsp"));
        }
        
        // IllegalArgumentExceptionが発生する場合
        context = new ExecutionContext()
                        .addHandler("//", handler)
                        .setSessionScopedVar("re", new IllegalArgumentException());
        try {
            res = context.handleNext(req);
            fail();
        } catch (HttpErrorResponse e) {
            res = e.getResponse();
            assertThat(res.getStatusCode(), is(404));
            assertThat(res.getContentPath().toString(), is("servlet://error.jsp"));
        }
        
        // ApplicationExceptionが発生する場合
        context = new ExecutionContext()
                        .addHandler("//", handler)
                        .setSessionScopedVar("re", new ApplicationException());
        try {
            res = context.handleNext(req);
            fail();
        } catch (HttpErrorResponse e) {
            res = e.getResponse();
            assertThat(res.getStatusCode(), is(400));
            assertThat(res.getContentPath().toString(), is("servlet://input.jsp"));
        }
        
        // 処理対象でない例外が発生した場合
        action = new Object() {
            @OnErrors({
                @OnError(type = ApplicationException.class, path = "servlet://input.jsp"),
                @OnError(type = IllegalArgumentException.class, path = "servlet://error.jsp", statusCode = 404)
            })
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                RuntimeException re = (RuntimeException) ctx.getSessionScopedVar("re");
                if (re != null) {
                    throw re;
                }
                return new HttpResponse("servlet://success.jsp");
                
            }
        };
        handler = new HttpMethodBinding(action);
        context = new ExecutionContext()
                        .addHandler("//", handler)
                        .setSessionScopedVar("re", new RuntimeException("test_no_target"));
        try {
            res = context.handleNext(req);
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("test_no_target"));
        }
    }
    
    @Test
    public void testOrder() {
        
        Object action = new Object() {
            @OnErrors({
                @OnError(type = RuntimeException.class, path = "servlet://runtime.jsp", statusCode = 500),
                @OnError(type = ApplicationException.class, path = "servlet://input.jsp"),
                @OnError(type = IllegalArgumentException.class, path = "servlet://error.jsp", statusCode = 404)
            })
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                RuntimeException re = (RuntimeException) ctx.getSessionScopedVar("re");
                if (re != null) {
                    throw re;
                }
                return new HttpResponse("servlet://success.jsp");
                
            }
        };
        HttpRequestHandler handler = new HttpMethodBinding(action);

        HttpRequest req = new MockHttpRequest("GET /index.html HTTP/1.1");
        ExecutionContext context = null;
        HttpResponse res = null;
        
        // 例外が発生しない場合
        context = new ExecutionContext()
                        .addHandler("//", handler);
        res = context.handleNext(req);
        assertThat(res.getStatusCode(), is(200));
        assertThat(res.getContentPath().toString(), is("servlet://success.jsp"));
        
        /*
         * 全部RuntimeExceptionで処理される。
         */
        
        // IllegalStateExceptionが発生する場合
        context = new ExecutionContext()
                        .addHandler("//", handler)
                        .setSessionScopedVar("re", new IllegalStateException());
        try {
            res = context.handleNext(req);
            fail();
        } catch (HttpErrorResponse e) {
            res = e.getResponse();
            assertThat(res.getStatusCode(), is(500));
            assertThat(res.getContentPath().toString(), is("servlet://runtime.jsp"));
        }
        
        // RuntimeExceptionが発生する場合
        context = new ExecutionContext()
                        .addHandler("//", handler)
                        .setSessionScopedVar("re", new RuntimeException());
        try {
            res = context.handleNext(req);
            fail();
        } catch (HttpErrorResponse e) {
            res = e.getResponse();
            assertThat(res.getStatusCode(), is(500));
            assertThat(res.getContentPath().toString(), is("servlet://runtime.jsp"));
        }
        
        // Errorが発生する場合
        context = new ExecutionContext()
                        .addHandler("//", handler)
                        .setSessionScopedVar("re", new Error());
        try {
            res = context.handleNext(req);
            fail();
        } catch (HttpErrorResponse e) {
            res = e.getResponse();
            assertThat(res.getStatusCode(), is(500));
            assertThat(res.getContentPath().toString(), is("servlet://runtime.jsp"));
        }

        // IllegalArgumentExceptionが発生する場合
        context = new ExecutionContext()
                        .addHandler("//", handler)
                        .setSessionScopedVar("re", new IllegalArgumentException());
        try {
            res = context.handleNext(req);
            fail();
        } catch (HttpErrorResponse e) {
            res = e.getResponse();
            assertThat(res.getStatusCode(), is(500));
            assertThat(res.getContentPath().toString(), is("servlet://runtime.jsp"));
        }
        
        // ApplicationExceptionが発生する場合
        context = new ExecutionContext()
                        .addHandler("//", handler)
                        .setSessionScopedVar("re", new ApplicationException());
        try {
            res = context.handleNext(req);
            fail();
        } catch (HttpErrorResponse e) {
            res = e.getResponse();
            assertThat(res.getStatusCode(), is(500));
            assertThat(res.getContentPath().toString(), is("servlet://runtime.jsp"));
        }
    }
    
    // サンプルコード確認用
    @OnError(
        type = ApplicationException.class
      , path ="servlet://registerForm.jsp"
    )
    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
        registerUser(req.getParamMap());
        return new HttpResponse(200, "servlet://registrationCompleted.jsp");
    }
    
    public void registerUser(Object obj) {
    }
    
    public HttpResponse handle2(HttpRequest req, ExecutionContext ctx) {
        try {
            registerUser(req.getParamMap());
                return new HttpResponse(200, "servlet://registrationCompleted.jsp");
        } catch(ApplicationException ae) {
            throw new HttpErrorResponse(400, "servlet://registerForm.jsp", ae);
        }
    }    
}
