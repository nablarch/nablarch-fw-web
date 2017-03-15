package nablarch.fw.web.dispatcher;

import static nablarch.test.support.tool.Hereis.string;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;

import nablarch.common.web.MockHttpSession;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpMethodBinding;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.handler.HttpErrorHandler;
import nablarch.fw.web.handler.HttpRequestJavaPackageMapping;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;
import org.junit.Test;

public class HttpRequestJavaPackageMappingTest {

    private ExecutionContext ctx;

    public void setUp() {
        ctx = new ServletExecutionContext(initServletReq(), null, null);
    }
    
    private MockServletRequest initServletReq() {
        MockHttpSession session = new MockHttpSession();
        session.setId("session_id_test");
        MockServletRequest servletReq = new MockServletRequest();
        servletReq.setSession(session);
        return servletReq;
    }

    @Test
    public void testDefaultConstructor() throws Exception {
        ExecutionContext context = new ExecutionContext()
            .addHandler(new HttpErrorHandler())
            .addHandler(new HttpRequestJavaPackageMapping());
        
        HttpResponse response = context.handleNext(new MockHttpRequest(string()));
        /******************************************************
        GET /webapp/sample/SimpleWebApp/index.html HTTP/1.1
        ******************************************************/
        
        assertEquals(404, response.getStatusCode());
        
        context = new ExecutionContext()
                 .setMethodBinder(new HttpMethodBinding.Binder())
                 .addHandler(new HttpErrorHandler())
                 .addHandler(new HttpRequestJavaPackageMapping());
        response = context.handleNext(new MockHttpRequest(string()));
        /****************************************************************
        GET /nablarch/fw/web/sample/SimpleWebApp/index.html HTTP/1.1
        ****************************************************************/       
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testConstructorWithInvalidArgument() {
        try {
            new HttpRequestJavaPackageMapping(null, "nablarch.fw.web");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        
        try {
            new HttpRequestJavaPackageMapping("/webapp/", null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testAccessors() throws Exception {
        ExecutionContext context = new ExecutionContext()
            .setMethodBinder(new HttpMethodBinding.Binder())
            .addHandler(new HttpErrorHandler())
            .addHandler(new HttpRequestJavaPackageMapping()
                           .setBasePackage("nablarch.fw.web")
                           .setBasePath("/webapp/")
             );

        HttpResponse response = context.handleNext(new MockHttpRequest(string()));
        /******************************************************
        GET /webapp/sample/SimpleWebApp/index.html HTTP/1.1
        ******************************************************/
        
        assertEquals(200, response.getStatusCode());
        assertEquals("servlet://jsp/index.jsp", response.getContentPath().toString());
        
        // マルチバイト文字を含むリソースに対するアクセス
        String path = new URI("/webapp/sample/SimpleWebApp/画像.png").toASCIIString();
        context = new ExecutionContext()
        .setMethodBinder(new HttpMethodBinding.Binder())
        .addHandler(new HttpErrorHandler())
        .addHandler(new HttpRequestJavaPackageMapping()
                       .setBasePackage("nablarch.fw.web")
                       .setBasePath("/webapp/")
         );
        response = context.handleNext(new MockHttpRequest(string(path)));
        /*****************************************************
        GET ${path} HTTP/1.1
        ******************************************************/
        assertEquals(404, response.getStatusCode());
        // マルチバイト文字を含むメソッド／クラスに対するディスパッチはサポートしない。
        // ステータスコード404がかえされる。
         
        
        context = new ExecutionContext()
        .addHandler(new HttpErrorHandler())
        .addHandler(new HttpRequestJavaPackageMapping()
                       .setBasePackage("nablarch.fw.web")
                       .setBasePath("/webapp/")
         );
        response = context.handleNext(new MockHttpRequest(string()));
        /****************************************************************
        GET /nablarch/fw/web/sample/SimpleWebApp/index.html HTTP/1.1
        ****************************************************************/
        assertEquals(404, response.getStatusCode());


        try {
            new HttpRequestJavaPackageMapping().setBasePackage(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
                
        try {
            new HttpRequestJavaPackageMapping().setBasePath(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testHandle() {
        ExecutionContext context = new ExecutionContext()
            .setMethodBinder(new HttpMethodBinding.Binder())
            .addHandler(new HttpErrorHandler())
            .addHandler(new HttpRequestJavaPackageMapping("/webapp/", "nablarch.fw.web"));

        // nablarch.fw.web.sample.SimpleWebApp#getIndexHtml()に処理が委譲される。
        HttpRequest req = new MockHttpRequest("GET /webapp/sample/SimpleWebApp/index.html HTTP/1.1");
        
        HttpResponse res = context.handleNext(req);
        assertEquals(200, res.getStatusCode());
        assertEquals("servlet://jsp/index.jsp", res.getContentPath().toString());

        req = new MockHttpRequest("GET /webapp/foo/bar/SimpleWebApp/index.html HTTP/1.1");
        
        context = new ExecutionContext()
                 .addHandler(new HttpErrorHandler())
                 .addHandler(new HttpRequestJavaPackageMapping("/webapp/", "nablarch.fw.web"));
        
        res = context.handleNext(req);
        assertEquals(404, res.getStatusCode());
    }

    @Test
    public void testMappingToRootPackage() {
        ExecutionContext context;
        HttpResponse res;
        HttpRequest req;
        
        // ルート直下のパッケージ内のクラスへのマッピング
        context = new ExecutionContext()
                 .setMethodBinder(new HttpMethodBinding.Binder())
                 .addHandler(new HttpRequestJavaPackageMapping("/webapp/", "nablarch"));

        req = new MockHttpRequest("GET /webapp/TestAction2/message HTTP/1.1");
    
        res = context.handleNext(req);
        assertEquals(200, res.getStatusCode());
        assertEquals("this is a test message.", res.getBodyString());        
        
        // ルート(デフォルト)パッケージ上のクラスへのマッピング
        context = new ExecutionContext()
                 .setMethodBinder(new HttpMethodBinding.Binder())
                 .addHandler(new HttpRequestJavaPackageMapping("/webapp/", ""));

        req = new MockHttpRequest("GET /webapp/TestAction1/message HTTP/1.1");

        res = context.handleNext(req);
        assertEquals(200, res.getStatusCode());
        assertEquals("this is a test message.", res.getBodyString());          
        

    }

    @Test
    public void testProtectionFromInvalidAccess() {
        
        // baseUri外からのアクセスは無条件で404エラーとする。
        ExecutionContext context = new ExecutionContext()
            .addHandler(new HttpErrorHandler())
            .addHandler(new HttpRequestJavaPackageMapping("/webapp/", "nablarch.fw.web"));
        
        // パッケージ名を直接指定する。
        HttpResponse res = context.handleNext(new MockHttpRequest("GET /nablarch/fw/web/sample/SimpleWebApp/index.html HTTP/1.1"));
        
        assertEquals(404, res.getStatusCode());
        
        
        context = new ExecutionContext()
        .addHandler(new HttpErrorHandler())
        .addHandler(new HttpRequestJavaPackageMapping("/webapp/", "nablarch.fw.web"));
        
        // Java識別子で使用できない文字を含む場合は、無条件で404エラーとする。
        res = context.handleNext(new MockHttpRequest("GET /webapp/sample/%20SimpleWebApp/ HTTP/1.1"));
        assertEquals(404, res.getStatusCode());
        
        context = new ExecutionContext()
        .addHandler(new HttpErrorHandler())
        .addHandler(new HttpRequestJavaPackageMapping("/webapp/", "nablarch.fw.web"));
        
        // クラスパス上に存在しないクラス
        res = context.handleNext(new MockHttpRequest("GET /webapp/sample/Unknown/ HTTP/1.1"));
        assertEquals(404, res.getStatusCode());
    }

    public void testErrorHandling() {
        Handler<HttpRequest, HttpResponse> dispatcher = new HttpRequestJavaPackageMapping();
        
        try {
            // 抽象クラスもしくはインタフェース
            HttpResponse res = dispatcher.handle(new MockHttpRequest("GET /nablarch/fw/Handler/ HTTP/1.1"), ctx);
            fail();
        } catch (RuntimeException e) {
            assertEquals(InstantiationException.class, e.getCause().getClass());
        }
    }
}
