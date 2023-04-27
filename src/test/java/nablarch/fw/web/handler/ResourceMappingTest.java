package nablarch.fw.web.handler;

import static nablarch.test.support.tool.Hereis.string;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import nablarch.test.support.log.app.OnMemoryLogWriter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ResourceMappingTest {
    File testFile = null;

    @Before
    public void setUp() {
        OnMemoryLogWriter.clear();
    }
    @After
    public void tearDown() {
        if (testFile != null) {
            testFile.delete();
        }
    }

    @Test
    public void testDefaultConstructor() {
        ExecutionContext ctx = new ExecutionContext();
        HttpRequestHandler handler = new ResourceMapping();
        
        try {
            handler.handle(new MockHttpRequest(string()), ctx);
            /***********************
            GET /etc/hosts HTTP/1.1
            ************************/
            fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
        Assert.assertTrue(OnMemoryLogWriter.getMessages("writer.memory").isEmpty());
    }
    
    @Test
    public void testConstructorWithIllegalArgument() {
        try {
            new ResourceMapping("/", "file://");
            fail();
        } catch(IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        
        try {
            new ResourceMapping("/", "classpath://../../../../META-INF/");
            fail();
        } catch(IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testHandle() throws Exception {
        // URI /webapp/resource/ 以下へのリクエストを、
        // コンテキストクラスパス nablarch.fw.web.dispatcher
        // 配下のファイルにマッピングする。
        HttpRequestHandler handler = new ResourceMapping(
          "/webapp/resource/", "classpath://nablarch/fw/web/handler/resourcemapping/"
        );
        
        ExecutionContext ctx = new ExecutionContext();
        HttpRequest req = new MockHttpRequest("GET /webapp/resource/test.css HTTP/1.1");
        
        HttpResponse res = handler.handle(req, ctx);
        Assert.assertEquals(200, res.getStatusCode());
        Assert.assertEquals("text/css", res.getContentType());
        Assert.assertEquals("classpath://nablarch/fw/web/handler/resourcemapping/test.css",
                res.getContentPath().toString());
        List<String> messages = OnMemoryLogWriter.getMessages("writer.memory");
        Assert.assertEquals(1, messages.size());
        Assert.assertTrue(messages.get(0).contains(
                "this request mapped to the resource path of 'classpath://nablarch/fw/web/handler/resourcemapping/test.css'"));
        OnMemoryLogWriter.clear();

        handler = new ResourceMapping(
                "/", "classpath://nablarch/fw/web/handler/resourcemapping/"
        );
        req = new MockHttpRequest("GET /test.css HTTP/1.1");
        res = handler.handle(req, ctx);
        Assert.assertEquals(200, res.getStatusCode());
        Assert.assertEquals("text/css", res.getContentType());
        Assert.assertEquals("classpath://nablarch/fw/web/handler/resourcemapping/test.css",
                res.getContentPath().toString());
        messages = OnMemoryLogWriter.getMessages("writer.memory");
        Assert.assertEquals(1, messages.size());
        Assert.assertTrue(messages.get(0).contains(
                "this request mapped to the resource path of 'classpath://nablarch/fw/web/handler/resourcemapping/test.css'"));

        // マルチバイト文字列を含むパスに対するマッピング
        handler = new ResourceMapping(
                "/画像/", "/app/img/"
        );
        req = new MockHttpRequest("GET " + new URI("/画像/secret.png").toASCIIString() + " HTTP/1.1");
        res = handler.handle(req, ctx);
        Assert.assertEquals(200, res.getStatusCode());
        Assert.assertEquals("image/png", res.getContentType());
        Assert.assertEquals("servlet:///app/img/secret.png",
                res.getContentPath().toString());
    }

    /**
     * {@link nablarch.fw.web.handler.ResourceMapping#handle(nablarch.fw.web.HttpRequest, nablarch.fw.ExecutionContext)}のテスト
     *
     * ベースパスが設定されていない場合処理に失敗すること。
     */
    @Test(expected = IllegalStateException.class)
    public void testHandleFail() {
        ResourceMapping mapping = new ResourceMapping();
        mapping.setBaseUri("/");
        mapping.handle(null, null);
    }

    /**
     * {@link nablarch.fw.web.handler.ResourceMapping#setBaseUri(String)}のテスト
     *
     * 不正な形式の場合、{@link java.lang.IllegalArgumentException}が送出される。
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBaseUri() {
        ResourceMapping mapping = new ResourceMapping();
        mapping.setBaseUri("://hogefuga");
    }

    /**
     * {@link nablarch.fw.web.handler.ResourceMapping#setBasePath(String)}のテスト。
     */
    @Test
    public void tsetSetBasePath() throws Exception {
        Field scheme = ResourceMapping.class.getDeclaredField("scheme");
        scheme.setAccessible(true);

        ResourceMapping mapping = new ResourceMapping();
        mapping.setBasePath("http://hoge.com");

        mapping.setBasePath("hoge.com");
        assertThat((String) scheme.get(mapping), is("servlet"));

        mapping.setBasePath("classpath://hoge.com");
        assertThat((String) scheme.get(mapping), is("classpath"));
    }
    
    @Test
    public void testIllegalAccess() {
        HttpRequestHandler handler = new ResourceMapping(
            "/webapp/resource/", "classpath://nablarch/fw/web/dispatcher/"
        );
        ExecutionContext ctx = new ExecutionContext();
        // ベースURIの外側からのアクセス
        HttpRequest req = new MockHttpRequest("GET /test.css HTTP/1.1");
        HttpResponse res = handler.handle(req, ctx);
        Assert.assertEquals(404, res.getStatusCode());
        
        // 存在しないファイルへのアクセス。
        req = new MockHttpRequest(string());
        /***************************************
        GET /webapp/resource/nothing.css HTTP/1.1
        ****************************************/
        res = handler.handle(req, ctx);
        Assert.assertEquals(404, res.getStatusCode());
    }
}
