package nablarch.fw.web;

import static nablarch.test.support.tool.Hereis.string;
import static org.junit.Assert.assertEquals;

import nablarch.core.util.Builder;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Result;
import nablarch.test.support.tool.Hereis;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HttpMethodBindingTest {

    // ディスパッチ対象となるオブジェクト
    private Object delegate = new Object() {
    	
        // HTTPリクエスト GET /(base-uri)/index.html に対して実行される。
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
        
        // HTTPリクエスト POST /(base-uri)/comment に対して実行される。
        public HttpResponse postComment(HttpRequest req, ExecutionContext ctx) {
            ctx.setSessionScopedVar("comment", req.getParam("comment")[0]);
            return new HttpResponse(303).setLocation("index.html");
        }
        
        // "do"で始まるハンドラはGET/POSTどちらのメソッドの場合でも呼ばれる。
        // したがって、次のいずれの場合もこのメソッドを実行される。
        // GET  /(base-uri)/cleanUp
        // POST /(base-uri)/cleanUp
        public HttpResponse doCleanUp(HttpRequest req, ExecutionContext ctx) {
            ctx.getSessionScopeMap().clear();
            return new HttpResponse(303).setLocation("index.html");
        }
    };
    
    private HttpRequestHandler dispatcher;
    private HttpRequest req;
    private HttpResponse res;
    private ExecutionContext ctx;
    
    @Before
    public void setUp() {
        dispatcher = new HttpMethodBinding(delegate);
        ctx        = new ExecutionContext();
    }
    
    @Test
    public void testHandle() {
        // GETリクエストでindex.htmlを要求。
        req = new MockHttpRequest(string());
        /****************************************
        GET /foo/bar/index.html HTTP/1.1
        *****************************************/
        res = dispatcher.handle(req, ctx);
        
        assertEquals(200, res.getStatusCode());
        assertEquals(
            Hereis.string().trim()
            /***************************************
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
            ***********************************/
          , res.toString().trim().replaceAll("\r\n", Builder.LS)
        );
        
        // POSTリクエストでデータを送信
        req = new MockHttpRequest(string());
        /********************************************
        POST /foo/bar/comment HTTP/1.1
        Content-Type: application/x-www-form-urlencoded
        
        comment=hogehoge
        ********************************************/
        
        res = dispatcher.handle(req, ctx);
        
        assertEquals(303, res.getStatusCode());
        assertEquals("index.html", res.getLocation());
        assertEquals("hogehoge", ctx.getSessionScopedVar("comment"));
        
        
        // セッションデータ削除(doCleanUp()が呼ばれる。)
        req = new MockHttpRequest(string());
        /********************************************
        POST /foo/bar/cleanUp HTTP/1.1
        *********************************************/
        assertEquals(1, ctx.getSessionScopeMap().size());
        
        res = dispatcher.handle(req, ctx);

        assertEquals(0, ctx.getSessionScopeMap().size());
        assertEquals(303, res.getStatusCode());
        assertEquals("index.html", res.getLocation());
        
        
        //GETメソッドを使用した場合でもdoCleanUp()が呼ばれる。
        req = new MockHttpRequest(string());
        /********************************************
        GET /foo/bar/cleanUp HTTP/1.1
        *********************************************/
        ctx.setSessionScopedVar("comment", "hogehoge");
        assertEquals(1, ctx.getSessionScopeMap().size());
        
        res = dispatcher.handle(req, ctx);
        
        assertEquals(0, ctx.getSessionScopeMap().size());
        assertEquals(303, res.getStatusCode());
        assertEquals("index.html", res.getLocation());
    }
    
    
    private static class HasStaticHandler {
        public static HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
            return new HttpResponse();
        }
    }
    @Test
    public void testThatBinderDelegatesRequestToProperMethod() {
        HttpRequestHandler handler = new HttpMethodBinding(new Object() {
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        });
        assertEquals(200, handler.handle(new MockHttpRequest(string()), ctx).getStatusCode());
        /***********************
        GET /index.html HTTP/1.1
        ************************/
        
        
        handler = new HttpMethodBinding(new Object() {
            HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        });
        try {
            handler.handle(new MockHttpRequest(string()), ctx);
            /***********************
            GET /index.html HTTP/1.1
            ************************/
            Assert.fail();
        } catch (Result.Error e) {
            assertEquals(404, e.getStatusCode());
        }

        try {
            handler.handle(new MockHttpRequest(string()), ctx);
            /***********************
            GET notfound HTTP/1.1
            ************************/
            Assert.fail();
        } catch (Result.Error e) {
            assertEquals(404, e.getStatusCode());
        }

        
        handler = new HttpMethodBinding(new HasStaticHandler());
        try {
            handler.handle(new MockHttpRequest(string()), ctx);
            /***********************
            GET /index.html HTTP/1.1
            ************************/
            Assert.fail();
        } catch (Result.Error e) {
            assertEquals(404, e.getStatusCode());
        }
        
        handler = new HttpMethodBinding(new Object() {
            HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx, String useless) {
                return new HttpResponse();
            }
        });
        
        try {
            handler.handle(new MockHttpRequest(string()), ctx);
            /***********************
            GET /index.html HTTP/1.1
            ************************/
            Assert.fail();
        } catch (Result.Error e) {
            assertEquals(404, e.getStatusCode());
        }

        
        handler = new HttpMethodBinding(new Object() {
            HttpResponse getIndexHtml(HttpRequest req, String ctx) {
                return new HttpResponse();
            }
        });
        try {
            handler.handle(new MockHttpRequest(string()), ctx);
            /***********************
            GET /index.html HTTP/1.1
            ************************/
            Assert.fail();
        } catch (Result.Error e) {
            assertEquals(404, e.getStatusCode());
        }
        
        handler = new HttpMethodBinding(new Object() {
            HttpResponse getIndexHtml(String req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        });
        try {
            handler.handle(new MockHttpRequest(string()), ctx);
            /***********************
            GET /index.html HTTP/1.1
            ************************/
            Assert.fail();
        } catch (Result.Error e) {
            assertEquals(404, e.getStatusCode());
        }
    }
    
    

}
