package nablarch.fw.web;

import static nablarch.test.support.tool.Hereis.string;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.common.handler.threadcontext.InternalRequestIdAttribute;
import nablarch.common.handler.threadcontext.RequestIdAttribute;
import nablarch.common.handler.threadcontext.ThreadContextAttribute;
import nablarch.common.handler.threadcontext.ThreadContextHandler;
import nablarch.common.util.ShortRequestIdExtractor;
import nablarch.core.ThreadContext;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.Builder;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.Request;
import nablarch.fw.RequestHandlerEntry;
import nablarch.fw.Result;
import nablarch.fw.web.handler.ForwardingHandler;
import nablarch.fw.web.handler.HttpErrorHandler;
import nablarch.test.support.tool.Hereis;
import org.junit.Before;
import org.junit.Test;


public class RequestProcessorTest {

	private ExecutionContext     context;
    private HttpErrorHandler errorHandler;
    private HttpRequest req;

    @Before
    public void setUp() {
        errorHandler = new HttpErrorHandler();
        errorHandler.setDefaultPage("404", "/jsp/notFound.jsp");
        context = new ExecutionContext()
                    .setMethodBinder(new HttpMethodBinding.Binder())
                    .addHandler(errorHandler)
                    .addHandler("/baseUriA//"   , handlerA)
                    .addHandler("/baseUriB//"   , handlerB)
                    .addHandler("/baseUriA/app//", application)
                    .addHandler("/baseUriB/app//", application);
        
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                return new HashMap<String, Object>() {{
                    put("requestIdExtractor", new ShortRequestIdExtractor());
                }};
            }
        });
    }
    
    static HttpRequestHandler handlerA = new HttpRequestHandler() {
        public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
            ctx.setRequestScopedVar("handlerA", "passed");
            return ctx.handleNext(req);
        }
    };
    
    static HttpRequestHandler handlerB = new HttpRequestHandler() {
        public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
            ctx.setRequestScopedVar("handlerB", "passed");
            return ctx.handleNext(req);
        }
    };
    
    public static HttpRequestHandler application = new HttpMethodBinding(new Object() {
        public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
            Object comment = ctx.getSessionScopedVar("comment");
            return new HttpResponse()
                   .setContentType("text/html")
                   .write(Hereis.string(comment));
            /**********************************
            <html>
              <head>
                <title>Recent comment</title>
              </head>
              <body>
                <h1>${comment}</h1>
              </body>
            </html>
            ***********************************/
        }

        public HttpResponse postComment(HttpRequest req, ExecutionContext ctx) {
            ctx.setSessionScopedVar("comment", req.getParam("comment")[0]);
            return new HttpResponse(303).setLocation("index.html");
        }
    });

    @Test
    public void testDefaultpageSetting() {
        req = new MockHttpRequest("GET /unknown/path/index.html HTTP/1.1");
        HttpResponse res = context.handleNext(req);
        assertEquals(404, res.getStatusCode());
        assertEquals("servlet:///jsp/notFound.jsp", res.getContentPath().toString());
        
        setUp();
        errorHandler.setDefaultPages(new HashMap<String, String>() {{
            put("4..", "file:///www/userError.html");
        }});
        
        res = context.handleNext(req);
        assertEquals(404, res.getStatusCode());
        assertEquals("file:///www/userError.html", res.getContentPath().toString());
        
        ExecutionContext ctx = new ExecutionContext()
        .addHandler("//", new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                    return new HttpResponse(100);
                }
            }
        );
        res = ctx.handleNext(req);
        assertEquals(100, res.getStatusCode());
        assertNull(res.getContentPath());
    }

    @Test
    public void testForwarding() {
        final ArrayList<String> requestId = new ArrayList<String>();
        final ArrayList<String> internalRequestId = new ArrayList<String>();
        Object userService = new Object() {
            // 内部フォーワード先
            public HttpResponse doRegisterFormHtml(HttpRequest req, ExecutionContext ctx) {
                requestId.add(ThreadContext.getRequestId());
                internalRequestId.add(ThreadContext.getInternalRequestId());
                return new HttpResponse(200, "servlet://registerForm.jsp");
            }
            // 相対パスによる内部フォーワード
            public HttpResponse postNew(HttpRequest req, ExecutionContext ctx) {
                requestId.add(ThreadContext.getRequestId());
                internalRequestId.add(ThreadContext.getInternalRequestId());                
                throw new HttpErrorResponse(
                     400
                    ,"forward://registerForm.html"
                    , new IllegalStateException(
                          "I'm very sorry but I suddenly get so sleepy " +
                          "that I'm not able to process your request."
                      )
                );
            }
            // 絶対パスによる内部フォーワード
            public HttpResponse postUpdated(HttpRequest req, ExecutionContext ctx) {
                requestId.add(ThreadContext.getRequestId());
                internalRequestId.add(ThreadContext.getInternalRequestId());
                return new HttpResponse(200, "forward:///app/user/registerForm.html");
            }
        };
        
        ExecutionContext ctx = new ExecutionContext()
            .setMethodBinder(new HttpMethodBinding.Binder())
            .addHandler(new ThreadContextHandler().setAttributes(new ArrayList<ThreadContextAttribute>() {{
                this.add(new RequestIdAttribute());
                this.add(new InternalRequestIdAttribute());
            }}))
            .addHandler(new ForwardingHandler())
            .addHandler(new HttpErrorHandler())
            .addHandler("/app/user//", userService);

        req = new MockHttpRequest("POST /app/user/new HTTP/1.1");
        requestId.clear();
        internalRequestId.clear();
        
        HttpResponse res = ctx.handleNext(req);
        assertEquals(400, res.getStatusCode());
        assertEquals("servlet://registerForm.jsp", res.getContentPath().toString());
        
        assertEquals(2, requestId.size());
        assertEquals(2, internalRequestId.size());
        
        assertEquals("new", requestId.get(0));
        assertEquals("new", requestId.get(1));
        
        assertEquals("new", internalRequestId.get(0));
        assertEquals("registerForm", internalRequestId.get(1));
        

        requestId.clear();
        internalRequestId.clear();

        req = new MockHttpRequest("POST /app/user/updated HTTP/1.1");
        
        ctx = new ExecutionContext()
             .setMethodBinder(new HttpMethodBinding.Binder())
             .addHandler(new ThreadContextHandler().setAttributes(new ArrayList<ThreadContextAttribute>() {{
                this.add(new RequestIdAttribute());
                this.add(new InternalRequestIdAttribute());
              }}))             
             .addHandler(new ForwardingHandler())
             .addHandler(new HttpErrorHandler())
             .addHandler("/app/user//", userService);
        
        res = ctx.handleNext(req);
        assertEquals(200, res.getStatusCode());
        assertEquals("servlet://registerForm.jsp", res.getContentPath().toString());
        
        assertEquals(2, requestId.size());
        assertEquals(2, internalRequestId.size());
        
        assertEquals("updated", requestId.get(0));
        assertEquals("updated", requestId.get(1));
        
        assertEquals("updated", internalRequestId.get(0));
        assertEquals("registerForm", internalRequestId.get(1));
    }
    
    /**
     * 実行時例外時は500エラーレスポンスを返す。
     */
    @Test
    public void testHandleWhenAnRuntimeExceptionThrown() {
        ExecutionContext ctx = new ExecutionContext()
        .addHandler("//", new HttpErrorHandler() {{setDefaultPage("500", "/jsp/systemError.jsp");}})
        .addHandler("//", new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                    throw new RuntimeException();
                }
            }
        );

        req = new MockHttpRequest("GET / HTTP/1.1");
        
        HttpResponse res = ctx.handleNext(req);
        assertEquals(500, res.getStatusCode());
        assertEquals(
            "servlet:///jsp/systemError.jsp",
            res.getContentPath().toString()
        );
    }
    
    
    /**
     * HTTPエラーレスポンスが送出された場合は例外に設定されたレスポンスオブジェクトが返される。
     */
    @Test
    public void testHandleWhenAnHttpErrorResponseThrown() {
        context = new ExecutionContext()
        .addHandler("//", new HttpErrorHandler() {{setDefaultPage("400", "/jsp/userError.jsp");}})
        .addHandler("//", new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                    throw new HttpErrorResponse(400);
                }
            }
        );
        
        HttpResponse res = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        assertEquals(400, res.getStatusCode());
        assertEquals(
            "servlet:///jsp/userError.jsp",
            res.getContentPath().toString()
        );
    }
        
    /**
     * スタックオーバーフローはアプリ側の無限ループの疑いがあるので、
     * 通常エラー扱い。
     */
    @Test
    public void testHandleWhenAnStackOverFlowIsThrown() {
        context = new ExecutionContext()
        .addHandler("//", new HttpErrorHandler() {{setDefaultPage("500", "/jsp/systemError.jsp");}})
        .addHandler("//", new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                    throw new StackOverflowError();
                }
            }
        );
        
        HttpResponse res = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        assertEquals(500, res.getStatusCode());
        assertEquals(
            "servlet:///jsp/systemError.jsp",
            res.getContentPath().toString()
        );
    }
    
    /**
     * スタックオーバーフロー以外のJVMエラーはエントリポイント側で処理するので、
     * リクエストプロセッサ側では単にリスローする。
     */
    @Test
    public void testHandleWhenAVirtialMachineErrorIsThrown() {
        final Error jvmError = new InternalError();
        context = new ExecutionContext()
        .addHandler("//", new HttpErrorHandler())
        .addHandler("//", new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                    throw jvmError;
                }
            }
        );
        

        try {
            context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
            fail();
        } catch (VirtualMachineError e){
            assertSame(jvmError, e);
        }
    }
    
    /**
     * カレントスレッドのstop()メソッドが実行されたときはそのままリスローする。
     */
    @Test
    public void testHandleWhenGetCurrentThreadStopped() {
        final Error threadDeath = new ThreadDeath();
        context = new ExecutionContext()
        .addHandler("//", new HttpErrorHandler())
        .addHandler("//",  new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                    throw threadDeath;
                }
            }
        );
        
        try {
            context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
            fail();
        } catch (ThreadDeath e){
            assertSame(threadDeath, e);
        }
    }
    
    /** @since JDK1.6
     * java.lang.IOError が発生したときはそのままリスローする。
    public void testHandleWhenIOErrorIsThrown() {
        final Error ioError = new IOError(new RuntimeException());
        processor = new HttpRequestProcessor()
        .addHandler("//", new HttpErrorHandler())
        .addHandler("//", new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                    throw ioError;
                }
            }
        );
        
        req = new HttpRequest("GET / HTTP/1.1");
        ctx = new ExecutionContext();
        
        try {
            HttpResponse res = processor.handle(req, ctx);
            fail();
        } catch (IOError e){
            assertSame(ioError, e);
        }
    }
    */
    
    /**
     * その他のエラーが発生したときは、通常エラー扱い。
     */
    @Test
    public void testHandleWhenAnErrorOfAnyOtherTypeIsThrown() {
        context = new ExecutionContext()
        .addHandler("//", new HttpErrorHandler() {{setDefaultPage("500", "/jsp/systemError.jsp");}})
        .addHandler("//", new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                throw new Error();
            }
        });

        HttpResponse res = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        assertEquals(500, res.getStatusCode());
        assertEquals(
            "servlet:///jsp/systemError.jsp",
            res.getContentPath().toString()
        );
    }

    @Test
    public void testHandle() {
        HttpResponse res = context.handleNext(new MockHttpRequest(string()));
        /*************************************
        GET /baseUriA/app/index.html HTTP/1.1
        *************************************/
        
        assertEquals("passed", context.getRequestScopedVar("handlerA"));
        assertNull(context.getRequestScopedVar("handlerB"));
        assertEquals(200, res.getStatusCode());
        assertEquals(
            Hereis.string().trim()
            /********************************
            HTTP/1.1 200 OK
            Content-Length: 101
            Content-Type: text/html
    
            <html>
              <head>
                <title>Recent comment</title>
              </head>
              <body>
                <h1></h1>
              </body>
            </html>
            ********************************/
          , res.toString().trim().replaceAll("\r\n", Builder.LS)
        );
        
        setUp();

        res = context.handleNext(new MockHttpRequest(string()));
        /*************************************
        GET /baseUriB/app/index.html HTTP/1.1
        *************************************/
        
        assertNull(context.getRequestScopedVar("handlerA"));
        assertEquals("passed", context.getRequestScopedVar("handlerB"));
        assertEquals(200, res.getStatusCode());
        
        
        setUp();
        req = new MockHttpRequest(string());
        /*************************************
        GET /hoge/app/index.html HTTP/1.1
        *************************************/
        res = context.handleNext(req);
        
        assertNull  (context.getRequestScopedVar("handlerA"));
        assertNull  (context.getRequestScopedVar("handlerB"));
        assertEquals(404, res.getStatusCode());
        
        setUp();
        req = new MockHttpRequest(string());
        /*************************************
        GET /baseUriB/app//index.html HTTP/1.1
        *************************************/
        res = context.handleNext(req);
        assertEquals(200, res.getStatusCode());
        
        setUp();
        req = new MockHttpRequest(string());
        /*************************************
        GET /baseUriB//app/index.html HTTP/1.1
        *************************************/
        res = context.handleNext(req);
        assertEquals(404, res.getStatusCode());
    }

    @Test
    public void testHandleWithIllegalArgs() {
        req = new MockHttpRequest().setRequestUri(
                "GET /invalid/\u0000path/ HTTP/1.1"
        );
        
        try {
            new ExecutionContext().addHandler("//", new Handler<HttpRequest, HttpResponse>() {
                public HttpResponse handle(HttpRequest data, ExecutionContext context) {
                    return new HttpResponse(200);
                }
            }).handleNext(req);
            fail();
        } catch (Result.NotFound e) {
            assertEquals(404, e.getStatusCode());
        }
    }
       
    /**
     *         processor = new HttpRequestProcessor()
     *              .addHandler(errorHandler)
     *              .addHandler("/baseUriA//"   , handlerA)
     *              .addHandler("/baseUriB//"   , handlerB)
     *              .addHandler("/baseUriA/app/", application)
     *              .addHandler("/baseUriB/app/", application)
     *              .setDirectoryIndex("index.html");
     */
    @Test
    public void testAddHandler() {
        ExecutionContext ctx = new ExecutionContext();
        req = new MockHttpRequest(string());
        /*************************************
        GET /baseUriA/app/index.html HTTP/1.1
        *************************************/
        List<Handler> handlerQueue = context.getHandlerQueue();
        assertTrue ( handlerQueue.get(0) instanceof HttpErrorHandler);
        assertTrue (((RequestHandlerEntry)handlerQueue.get(1)).isAppliedTo(req, ctx));
        assertFalse(((RequestHandlerEntry)handlerQueue.get(2)).isAppliedTo(req, ctx));
        assertTrue (((RequestHandlerEntry)handlerQueue.get(3)).isAppliedTo(req, ctx));
        assertFalse(((RequestHandlerEntry)handlerQueue.get(4)).isAppliedTo(req, ctx));
        
        ctx = new ExecutionContext();
        req = new MockHttpRequest(string());
        /*************************************
        GET /baseUriB/app/index.html HTTP/1.1
        *************************************/
        handlerQueue = context.getHandlerQueue();
        assertTrue ( handlerQueue.get(0) instanceof HttpErrorHandler);
        assertFalse(((RequestHandlerEntry)handlerQueue.get(1)).isAppliedTo(req, ctx));
        assertTrue (((RequestHandlerEntry)handlerQueue.get(2)).isAppliedTo(req, ctx));
        assertFalse(((RequestHandlerEntry)handlerQueue.get(3)).isAppliedTo(req, ctx));
        assertTrue (((RequestHandlerEntry)handlerQueue.get(4)).isAppliedTo(req, ctx));
        
        
        ctx = new ExecutionContext();
        req = new MockHttpRequest(string());
        /*************************************
        POST /baseUriB/app/comment HTTP/1.1
        *************************************/
        handlerQueue = context.getHandlerQueue();
        assertTrue ( handlerQueue.get(0) instanceof HttpErrorHandler);
        assertFalse(((RequestHandlerEntry)handlerQueue.get(1)).isAppliedTo(req, ctx));
        assertTrue (((RequestHandlerEntry)handlerQueue.get(2)).isAppliedTo(req, ctx));
        assertFalse(((RequestHandlerEntry)handlerQueue.get(3)).isAppliedTo(req, ctx));
        assertTrue (((RequestHandlerEntry)handlerQueue.get(4)).isAppliedTo(req, ctx));
        
        
        
        ctx = new ExecutionContext();
        req = new MockHttpRequest(string());
        /*************************************
        POST /baseUriA/hoge/ HTTP/1.1
        *************************************/
        handlerQueue = context.getHandlerQueue();
        assertTrue ( handlerQueue.get(0) instanceof HttpErrorHandler);
        assertTrue (((RequestHandlerEntry)handlerQueue.get(1)).isAppliedTo(req, ctx));
        assertFalse(((RequestHandlerEntry)handlerQueue.get(2)).isAppliedTo(req, ctx));
        assertFalse(((RequestHandlerEntry)handlerQueue.get(3)).isAppliedTo(req, ctx));
        assertFalse(((RequestHandlerEntry)handlerQueue.get(4)).isAppliedTo(req, ctx));

        
        ctx = new ExecutionContext();
        req = new MockHttpRequest(string());
        /*************************************
        POST /hoge/app/ HTTP/1.1
        **************************************/        
        assertTrue ( handlerQueue.get(0) instanceof HttpErrorHandler);
        assertFalse(((RequestHandlerEntry)handlerQueue.get(1)).isAppliedTo(req, ctx));
        assertFalse(((RequestHandlerEntry)handlerQueue.get(2)).isAppliedTo(req, ctx));
        assertFalse(((RequestHandlerEntry)handlerQueue.get(3)).isAppliedTo(req, ctx));
        assertFalse(((RequestHandlerEntry)handlerQueue.get(4)).isAppliedTo(req, ctx));
    }

    @Test
    public void testRegistrationWithIllegalArgs() {
        try {
            context.addHandler("//", errorHandler)
                   .addHandler("", handlerA);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        
        try {
            HttpRequestHandler handler = null;
            context.addHandler("/path/to/handler/", handler);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        
        //try {
        //    // 制御文字はNG
        //    context.addHandler("/invalid/\u0000path/", handlerA);
        //    fail();
        //
        //} catch (IllegalArgumentException e) {
        //    assertEquals("Invalid pattern format: /invalid/\u0000path/", e.getMessage());
        //}
        
        // マルチバイト文字を含んだリクエストパスは登録可能(内部的にエスケープされる。)
        context.addHandler("/画像フォルダ//*.png", handlerA);        
    }
    
    public static class SimpleRequest implements Request<String[]> {
        public SimpleRequest(String path) {
            this.path = path;
        }
        String path;
        public String getRequestPath() {
            return path;
        }

        public Request setRequestPath(String requestPath) {
            this.path = requestPath;
            return this;
        }

        public String[] getParam(String name) {
            return null;
        }

        public Map<String, String[]> getParamMap() {
            return null;
        }
    }

    @Test
    public void testHandlerEntry() throws Exception {
        RequestHandlerEntry<SimpleRequest, ?> entry = null;
        ExecutionContext ctx = new ExecutionContext();
        entry = new RequestHandlerEntry<SimpleRequest, Object>().setRequestPattern("/baseUriA//");
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/"), ctx));
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/foo.html"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriB/"), ctx));
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/"), ctx));
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/foo.html"), ctx));    
        
        entry = new RequestHandlerEntry<SimpleRequest, Object>().setRequestPattern("/baseUriA/");
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/foo.html"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriB/"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/foo.html"), ctx));
        
        entry = new RequestHandlerEntry<SimpleRequest, Object>().setRequestPattern("/baseUriA/*.html");
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/"), ctx));
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/foo.html"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/foo.txt"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriB/"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/foo.html"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/foo.txt"), ctx));
        
        entry = new RequestHandlerEntry<SimpleRequest, Object>().setRequestPattern("/baseUriA//*.html");
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/"), ctx));
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/foo.html"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/foo.txt"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriB/"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/"), ctx));
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/foo.html"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/foo.txt"), ctx));
        
        entry = new RequestHandlerEntry<SimpleRequest, Object>().setRequestPattern("/baseUriA//*.txt");
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/foo.html"), ctx));
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/foo.txt"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriB/"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/foo.html"), ctx));
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/foo.txt"), ctx));
        
        entry = new RequestHandlerEntry<SimpleRequest, Object>().setRequestPattern("/baseUriA//foo.*");
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/"), ctx));
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/foo.html"), ctx));
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/foo.txt"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriB/"), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/"), ctx));
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/foo.html"), ctx));
        assertTrue (entry.isAppliedTo(new SimpleRequest("/baseUriA/hoge/fuga/foo.txt"), ctx));
        
        // マルチバイト文字を含んだリクエストパス
        entry = new RequestHandlerEntry<SimpleRequest, Object>().setRequestPattern("/baseUriA/お宝画像//*.png");
        assertTrue (entry.isAppliedTo(new SimpleRequest(escape("/baseUriA/お宝画像/pic1.png")), ctx));        
        assertFalse(entry.isAppliedTo(new SimpleRequest(escape("/baseUriA/お宝画像/pic1.jpg")), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest(escape("/baseUriA/がっかり画像/pic1.png")), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest(escape("/baseUriA/がっかり画像/pic1.jpg")), ctx));
        assertTrue(entry.isAppliedTo(new SimpleRequest(escape("/baseUriA/お宝画像/work/田嶋.png")), ctx));
        assertFalse(entry.isAppliedTo(new SimpleRequest(escape("/baseUriA/お宝画像/work/田嶋.ピング")), ctx));

    }
    
    private String escape(String uri) throws Exception {
        return new URI(uri).toASCIIString();
    } 
}
