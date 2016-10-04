package nablarch.fw.web;

import static nablarch.fw.web.ResourceLocator.isValidPath;
import static nablarch.test.support.tool.Hereis.string;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;

import nablarch.core.ThreadContext;
import nablarch.core.util.Builder;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.handler.InternalMonitor;
import nablarch.test.IgnoringLS;
import nablarch.test.support.tool.Hereis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author ssd
 *
 */
public class ResourceLocatorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected void setUp() throws Exception {
        ThreadContext.clear();
    }

    @Test
    public void testSyntaxOfResourcePath() {
        // valid pattern
        assertTrue( isValidPath("file:///"));
        assertTrue( isValidPath("file:///etc/hosts"));
        assertTrue( isValidPath("file:///etc/hosts.allow"));
        assertTrue( isValidPath("file://.bashrc"));
        assertTrue( isValidPath("file://./test.log"));

        // invalid pattern
        assertFalse(isValidPath("file://../etc/hosts.allow"));
        assertFalse(isValidPath("file:///etc/hosts..allow"));
        assertFalse(isValidPath("file://~/bashrc"));

        // windows OS
        assertTrue(isValidPath("file://C:\\USER\\DOCS\\LETTER.TXT"));
        assertTrue(isValidPath("file://C:/USER/DOCS/LETTER.TXT"));
        assertTrue(isValidPath("file://A:PICTURE.JPG"));

        new HttpResponse().setContentPath("https://www.example.com/secure.html");
        new HttpResponse("https://www.example.com/secure.html");
        // url (outside of the thread context)

    }

    @Test
    public void testSyntaxOfResourcePathWithTheRedirectionScheme() {

        ResourceLocator redirection = null;

        redirection = new HttpResponse("http://www.example.com/index.html")
                         .getContentPath();

        assertEquals("http://www.example.com/index.html", redirection.toString());
        assertEquals("http",        redirection.getScheme());
        assertEquals("/index.html", redirection.getPath());
        assertEquals("/",           redirection.getDirectory());
        assertEquals("index.html",  redirection.getResourceName());

        redirection = new HttpResponse("https://www.example.com/secure.html")
                         .getContentPath();

        assertEquals("https://www.example.com/secure.html", redirection.toString());
        assertEquals("https",        redirection.getScheme());
        assertEquals("/secure.html", redirection.getPath());
        assertEquals("/",            redirection.getDirectory());
        assertEquals("secure.html",  redirection.getResourceName());


        redirection = new HttpResponse("http://www.example.co.jp/test/index.html")
                         .getContentPath();

        assertEquals("http://www.example.co.jp/test/index.html", redirection.toString());
        assertEquals("http",             redirection.getScheme());
        assertEquals("/test/index.html", redirection.getPath());
        assertEquals("/test/",           redirection.getDirectory());
        assertEquals("index.html",       redirection.getResourceName());


        redirection = new HttpResponse("http://www.example.co.jp/")
                         .getContentPath();

        assertEquals("http://www.example.co.jp/", redirection.toString());
        assertEquals("http",  redirection.getScheme());
        assertEquals("/",     redirection.getPath());
        assertEquals("/",     redirection.getDirectory());
        assertEquals("",      redirection.getResourceName());


        redirection = new HttpResponse("https://www.google.com/calendar")
                         .getContentPath();

        assertEquals("https://www.google.com/calendar", redirection.toString());
        assertEquals("https",     redirection.getScheme());
        assertEquals("/calendar", redirection.getPath());
        assertEquals("/",         redirection.getDirectory());
        assertEquals("calendar",  redirection.getResourceName());

        redirection = new HttpResponse("https://www.google.com/calendar/")
                         .getContentPath();

        assertEquals("https://www.google.com/calendar/", redirection.toString());
        assertEquals("https",      redirection.getScheme());
        assertEquals("/calendar/", redirection.getPath());
        assertEquals("/calendar/", redirection.getDirectory());
        assertEquals("",           redirection.getResourceName());


        redirection = new HttpResponse("http://www.example.co.jp")
                         .getContentPath();

        assertEquals("http://www.example.co.jp", redirection.toString());
        assertEquals("http",                     redirection.getScheme());
        assertEquals("",                         redirection.getPath());
        assertEquals("",                         redirection.getDirectory());
        assertEquals("",                         redirection.getResourceName());

        redirection = new HttpResponse("http://www.example.org/index.html?hoge=fuga&piyo=poyo")
                         .getContentPath();

        assertEquals("http://www.example.org/index.html?hoge=fuga&piyo=poyo"
                   , redirection.toString());
        assertEquals("http", redirection.getScheme());
        assertEquals("/index.html?hoge=fuga&piyo=poyo"
                   , redirection.getPath());
        assertEquals("/", redirection.getDirectory());
        assertEquals("index.html?hoge=fuga&piyo=poyo"
                   , redirection.getResourceName());


        redirection = new HttpResponse("http://www.example.org/index.html")
                         .getContentPath();

        assertEquals("http://www.example.org/index.html"
                   , redirection.toString());
        assertEquals("http", redirection.getScheme());
        assertEquals("/index.html"
                   , redirection.getPath());
        assertEquals("/", redirection.getDirectory());
        assertEquals("index.html"
                   , redirection.getResourceName());



        redirection = new HttpResponse("http://www.example.org/index.html?hoge=")
                         .getContentPath();

        assertEquals("http://www.example.org/index.html?hoge="
                   , redirection.toString());
        assertEquals("http", redirection.getScheme());
        assertEquals("/index.html?hoge="
                   , redirection.getPath());
        assertEquals("/", redirection.getDirectory());
        assertEquals("index.html?hoge="
                   , redirection.getResourceName());

        redirection = new HttpResponse("http://www.example.org/index.html?hoge==")
                         .getContentPath();

        assertEquals("http://www.example.org/index.html?hoge=="
                   , redirection.toString());
        assertEquals("http", redirection.getScheme());
        assertEquals("/index.html?hoge=="
                   , redirection.getPath());
        assertEquals("/", redirection.getDirectory());
        assertEquals("index.html?hoge=="
                   , redirection.getResourceName());
        assertEquals("www.example.org", redirection.getHostname());

        redirection = new HttpResponse("http://www.example.org/index.html#id1?hoge=fuga&piyo=poyo")
                         .getContentPath();

        assertEquals("http://www.example.org/index.html#id1?hoge=fuga&piyo=poyo"
                   , redirection.toString());
        assertEquals("http", redirection.getScheme());
        assertEquals("/index.html#id1?hoge=fuga&piyo=poyo"
                   , redirection.getPath());
        assertEquals("/", redirection.getDirectory());
        assertEquals("index.html#id1?hoge=fuga&piyo=poyo"
                   , redirection.getResourceName());

        redirection = new HttpResponse("http://www.example.org/index.html?hoge=fuga")
                         .getContentPath();

        assertEquals("http://www.example.org/index.html?hoge=fuga"
                   , redirection.toString());
        assertEquals("http", redirection.getScheme());
        assertEquals("/index.html?hoge=fuga"
                   , redirection.getPath());
        assertEquals("/", redirection.getDirectory());
        assertEquals("index.html?hoge=fuga"
                   , redirection.getResourceName());

        redirection = new HttpResponse("http://www.example.org?hoge=fuga")
                         .getContentPath();

        assertEquals("http://www.example.org?hoge=fuga"
                   , redirection.toString());
        assertEquals("http", redirection.getScheme());
        assertEquals("?hoge=fuga"
                   , redirection.getPath());
        assertEquals("", redirection.getDirectory());
        assertEquals("?hoge=fuga"
                   , redirection.getResourceName());

        redirection = new HttpResponse("http://www.example.org#hoge?foo=fuga")
                         .getContentPath();

        assertEquals("http://www.example.org#hoge?foo=fuga"
                   , redirection.toString());
        assertEquals("http", redirection.getScheme());
        assertEquals("#hoge?foo=fuga"
                   , redirection.getPath());
        assertEquals("", redirection.getDirectory());
        assertEquals("#hoge?foo=fuga"
                   , redirection.getResourceName());

        redirection = new HttpResponse("http://www.example.org:80?foo=fuga")
                         .getContentPath();

        assertEquals("http://www.example.org:80?foo=fuga"
                   , redirection.toString());
        assertEquals("http", redirection.getScheme());
        assertEquals("?foo=fuga"
                   , redirection.getPath());
        assertEquals("", redirection.getDirectory());
        assertEquals("?foo=fuga"
                   , redirection.getResourceName());

        redirection = new HttpResponse("http://www.example.org/?foo=fuga")
                        .getContentPath();

        assertEquals("http://www.example.org/?foo=fuga"
          , redirection.toString());
        assertEquals("http", redirection.getScheme());
        assertEquals("/?foo=fuga"
          , redirection.getPath());
        assertEquals("/", redirection.getDirectory());
        assertEquals("?foo=fuga"
          , redirection.getResourceName());
    }

    @Test
    public void testThatThrowsBadRequestErrorAgainstInvalidRequestPath() {

        try {
            ResourceLocator.valueOf("file://../etc/hosts.allow");
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof HttpErrorResponse);
            assertEquals(
              400
            , ((HttpErrorResponse)e).getResponse().getStatusCode()
            );
        }

        // 多彩なスキーム
        try {
            ResourceLocator.valueOf("itms://itunes.apple.com/jp/genre/id34");
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof HttpErrorResponse);
            assertEquals(
              400
            , ((HttpErrorResponse)e).getResponse().getStatusCode()
            );
        }

        // ユーザ名、パスワード
        try {
            ResourceLocator.valueOf("http://username@password@www.example.com/");
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof HttpErrorResponse);
            assertEquals(
              400
            , ((HttpErrorResponse)e).getResponse().getStatusCode()
            );
        }

        // 日本語ドメイン
        try {
            ResourceLocator.valueOf("http://総務省.jp/");
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof HttpErrorResponse);
            assertEquals(
              400
            , ((HttpErrorResponse)e).getResponse().getStatusCode()
            );
        }
    }

    @Test
    public void testDefaultScheme() {
        assertTrue(isValidPath("/jsp/index.jsp"));
        assertEquals(
            "servlet",
            ResourceLocator.valueOf("/jsp/index.jsp").getScheme()
        );

        assertTrue(isValidPath("index.jsp"));
        assertEquals(
            "servlet",
            ResourceLocator.valueOf("index.jsp").getScheme()
        );
        assertTrue(isValidPath("./index.jsp"));
        assertEquals(
            "servlet",
            ResourceLocator.valueOf("./index.jsp").getScheme()
        );
    }

    @Test
    public void testLocateAFileWithRelativePath() {
        File cssfile = null;
        try {
            cssfile = Hereis.file("./test.css");
            /***************************
            body {
                color: black;
            }
            **************************/
            ResourceLocator locator = ResourceLocator.valueOf(
                "file://test.css"
            );

            assertTrue(cssfile.canRead());
            assertTrue(locator.exists());
            assertEquals("test.css", locator.getResourceName());

        } finally {
            cssfile.delete();
        }
    }

    @Test
    public void testLocateAFileWithAbsolutePath() {
        File cssfile = null;
        try {
            cssfile = Hereis.file("/var/tmp/test.css");
            /***************************
            body {
                color: black;
            }
            **************************/
            ResourceLocator locator = ResourceLocator.valueOf(
                "file:///var/tmp/test.css"
            );

            assertTrue(cssfile.canRead());
            assertTrue(locator.exists());
            assertEquals("test.css", locator.getResourceName());

        } finally {
            cssfile.delete();
        }
    }

    @Test
    public void testLocateAResourceWithClasspath() throws Exception {
        ResourceLocator indexJsp = ResourceLocator.valueOf(
            "classpath://nablarch/fw/web/sample/app/jsp/index.jsp"
        );

        assertTrue(indexJsp.exists());
        assertTrue(
            indexJsp.getRealPath()
                    .endsWith("nablarch/fw/web/sample/app/jsp/index.jsp")
        );

        BufferedReader reader = new BufferedReader(indexJsp.getReader());
        StringBuilder jspText = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            jspText.append(line).append(Builder.LS);
        }

        assertEquals(Hereis.string(), jspText.toString());
        /*****************************************************
        <%@ page pageEncoding="UTF-8" %>
        <html>
          <head>
            <title>Greeting Service</title>
          </head>
          <body>
            <%= request.getAttribute("greeting") %>
          </body>
        </html>
        ****************************************************/
    }

    @Test
    public void testLocateServletWithAbsoluteForwardName() throws Exception {
        HttpServer server = new HttpServer()
        .setWarBasePath("classpath://nablarch/fw/web/sample/app/")
        .setServletContextPath("/")
        .addHandler(new InternalMonitor())
        .addHandler("/path/to/somewhere/Greeting", new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                ctx.setRequestScopedVar("greeting", "Hello World!");
                return new HttpResponse(201).setContentPath(
                    "servlet:///jsp/index.jsp"
                );
            }
        })
        .startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(string()), null);
        /************************************************
        GET /path/to/somewhere/Greeting HTTP/1.1
        ************************************************/

        assertEquals(201, res.getStatusCode());
        assertEquals(201, InternalMonitor.response.getStatusCode());
        assertThat(res.getBodyString().trim(), IgnoringLS.equals(Hereis.string().trim()));
        /*****************************************************
        <html>
          <head>
            <title>Greeting Service</title>
          </head>
          <body>
            Hello World!
          </body>
        </html>
        ****************************************************/

        server
        .addHandler(new InternalMonitor())
        .addHandler(
            "/path/to/somewhere/ja/Greeting"
          , new HttpRequestHandler() {
                @Override
                public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                    ctx.setRequestScopedVar("greeting", "こんにちは");
                    return new HttpResponse(201).setContentPath(
                        "servlet:///ja/jsp/index.jsp"
                    );
                }
            }
        );

        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************
        GET /path/to/somewhere/ja/Greeting HTTP/1.1
        ************************************************/

        assertEquals(201, res.getStatusCode());
        assertEquals(201, InternalMonitor.response.getStatusCode());
        assertThat(res.getBodyString()
                .trim(), IgnoringLS.equals(Hereis.string()
                .trim()));
        /*****************************************************
        <html>
          <head>
            <title>元気にあいさつ</title>
          </head>
          <body>
            こんにちは
          </body>
        </html>
        ****************************************************/
    }

    @Test
    public void testLocateServletWithRelativeForwardName() {
        HttpServer server = new HttpServer()
        .setWarBasePath("classpath://nablarch/fw/web/sample/app/")
        .setServletContextPath("/")
        .addHandler(new InternalMonitor())
        .addHandler("/Greeting", new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                ctx.setRequestScopedVar("greeting", "Hello World!");
                return new HttpResponse(201).setContentPath(
                    "servlet://jsp/index.jsp"
                );
            }
        })
        .startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(string()), null);
        /*************************
        GET /Greeting HTTP/1.1
        **************************/

        assertEquals(201, res.getStatusCode());
        assertEquals(201, InternalMonitor.response.getStatusCode());
        assertThat(res.getBodyString().trim(), IgnoringLS.equals(Hereis.string().trim()));
        /*****************************************************
        <html>
          <head>
            <title>Greeting Service</title>
          </head>
          <body>
            Hello World!
          </body>
        </html>
        ****************************************************/


        server
        .addHandler(new InternalMonitor())
        .addHandler("/ja/Greeting", new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                ctx.setRequestScopedVar("greeting", "こんにちは");
                return new HttpResponse(201).setContentPath(
                    "servlet://jsp/index.jsp"
                );
            }
        });

        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************
        GET /ja/Greeting HTTP/1.1
        ************************************************/

        assertEquals(201, res.getStatusCode());
        assertEquals(201, InternalMonitor.response.getStatusCode());
        assertThat(res.getBodyString()
                .trim(), IgnoringLS.equals(Hereis.string()
                .trim()));
        /*****************************************************
        <html>
          <head>
            <title>元気にあいさつ</title>
          </head>
          <body>
            こんにちは
          </body>
        </html>
        ****************************************************/
    }


    /**
     * リダイレクション処理のテスト
     */
    @Test
    public void testRedirection() {
        HttpServer server = new HttpServer()
        .setWarBasePath("classpath://nablarch/fw/web/sample/app/")
        .setServletContextPath("/app")
        .addHandler("/test/Greeting", new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                ctx.invalidateSession();
                ctx.setRequestScopedVar("greeting", "Hello World!");
                return new HttpResponse().setContentPath(
                    "redirect:///index.jsp"
                );
            }
        })
        .addHandler("/test/Greeting2", new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                ctx.setRequestScopedVar("greeting", "Hello World!");
                return new HttpResponse().setContentPath(
                    "redirect://index.jsp"
                );
            }
        })
        .addHandler("/test/Greeting3", new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                ctx.setRequestScopedVar("greeting", "Hello World!");
                return new HttpResponse().setContentPath(
                    "http://www.example.com/index.html?hoge=fuga"
                );
            }
        })
        .addHandler("/test/Greeting4", new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                ctx.setRequestScopedVar("greeting", "Hello World!");
                return new HttpResponse().setContentPath(
                    "https://www.example.com/secure.html"
                );
            }
        })
        .startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(string()), null);
        /*************************
        GET /app/test/Greeting HTTP/1.1
        **************************/

        assertEquals(302, res.getStatusCode());
        // Locationはサーブレットコンテナ出力
        assertEquals("http://127.0.0.1/app/index.jsp", res.getLocation());


        res = server.handle(new MockHttpRequest(string()), null);
        /*************************
        GET /app/test/Greeting2 HTTP/1.1
        **************************/

        assertEquals(302, res.getStatusCode());
        // Locationはサーブレットコンテナ出力
        assertEquals("http://127.0.0.1/app/test/index.jsp", res.getLocation());

        res = server.handle(new MockHttpRequest(string()), null);
        /*************************
        GET /app/test/Greeting3 HTTP/1.1
        **************************/

        assertEquals(302, res.getStatusCode());
        // サーブレットコンテキスト外部へのリダイレクト
        assertEquals("http://www.example.com/index.html?hoge=fuga", res.getLocation());


        res = server.handle(new MockHttpRequest(string()), null);
        /*************************
        GET /app/test/Greeting4 HTTP/1.1
        **************************/

        assertEquals(302, res.getStatusCode());
        // サーブレットコンテキスト外部へのリダイレクト(https)
        assertEquals("https://www.example.com/secure.html", res.getLocation());
    }


    @Test
    public void testIsRelative() {
        assertFalse(ResourceLocator.valueOf("file:///").isRelative());
        assertTrue( ResourceLocator.valueOf("file://etc/hosts").isRelative());
        assertFalse(ResourceLocator.valueOf("classpath:///com/example/Test/").isRelative());
        assertFalse(ResourceLocator.valueOf("classpath://com/example/Test/").isRelative());
    }

    @Test
    public void testGetRealPath() {
        
        if(getOsName().contains("windows")){
            File current = new File(".");
            final String drive = current.getAbsolutePath()
                                            .substring(0, 3);
            assertEquals(ResourceLocator.valueOf("file:///").getRealPath(), drive);
            assertEquals(
                drive + "etc\\hosts",
                ResourceLocator.valueOf("file:///etc/hosts").getRealPath()
            );
        } else {
            assertEquals(ResourceLocator.valueOf("file:///").getRealPath(), "/");
            assertEquals(
                "/etc/hosts",
                ResourceLocator.valueOf("file:///etc/hosts").getRealPath()
            );
        }
        
        assertTrue(
                ResourceLocator.valueOf("classpath://nablarch/fw/web/")
                               .getRealPath()
                               .matches("\\/.*\\/nablarch\\/fw\\/web\\/")
            );
    }

    @Test
    public void testThrowsErrorWhenRealPathOfServletOfForwardPathIsCalculated() {
        try {
            ResourceLocator.valueOf("servlet:///jsp/index.jsp")
                           .getRealPath();
            fail();
        } catch(Throwable e) {
            assertEquals(UnsupportedOperationException.class, e.getClass());
        }

        try {
            ResourceLocator.valueOf("forward:///app/user/")
                           .getRealPath();
            fail();
        } catch(Throwable e) {
            assertEquals(UnsupportedOperationException.class, e.getClass());
        }

        try {
            ResourceLocator.valueOf("redirect:///app/user/")
                           .getRealPath();
            fail();
        } catch(Throwable e) {
            assertEquals(UnsupportedOperationException.class, e.getClass());
        }
    }

    @Test
    public void testExists() throws Exception {
        final File file = temporaryFolder.newFile();
        assertTrue (ResourceLocator.valueOf("forward:///any/path/").exists());
        assertTrue (ResourceLocator.valueOf("servlet:///any/path/").exists());
        assertFalse(ResourceLocator.valueOf("file:///not/exists.file/")
                .exists());
        assertTrue(ResourceLocator.valueOf("file://" + file.getCanonicalFile()
                .toURI()
                .getPath())
                .exists());
        assertFalse(ResourceLocator.valueOf("classpath://not/exists/")
                .exists());
        assertFalse(ResourceLocator.valueOf("classpath://nablarch/fw/web/").exists());
        assertFalse(ResourceLocator.valueOf("redirect:///any/path/").exists());
        assertFalse(ResourceLocator.valueOf("http://hoge.com/any/path/").exists());
        assertFalse(ResourceLocator.valueOf("https://hoge.com/any/path/").exists());
    }

    @Test
    public void testGetReader() throws Exception {
        final File file = temporaryFolder.newFile();
        assertNotNull(ResourceLocator.valueOf("file://" + file.getCanonicalFile().toURI().getPath()).getReader());
        assertNotNull(
            ResourceLocator.valueOf(
                "classpath://nablarch/fw/web/servlet/docroot/WEB-INF/web.xml"
            ).getReader()
        );

        try {
            ResourceLocator.valueOf("classpath://not/exists/resource.xml").getReader();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }

        try {
            ResourceLocator.valueOf("file:///not/exists.file/").getReader();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }

        try {
        	ResourceLocator.valueOf("forward:///any/path/").getReader();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }

        try {
        	ResourceLocator.valueOf("servlet:///any/path/").getReader();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }

        try {
            ResourceLocator.valueOf("redirect:///any/path/").getReader();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }

        try {
            ResourceLocator.valueOf("http://hoge.com/any/path/").getReader();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }

        try {
            ResourceLocator.valueOf("https://hoge.com/any/path/").getReader();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }
    }

    @Test
    public void testGetInputStream() throws Exception {
        final File file = temporaryFolder.newFile();
        assertNotNull(ResourceLocator.valueOf("file://" + file.getCanonicalFile().toURI().getPath()).getInputStream());
        assertNotNull(
            ResourceLocator.valueOf(
                "classpath://nablarch/fw/web/servlet/docroot/WEB-INF/web.xml"
            ).getInputStream()
        );

        try {
            ResourceLocator.valueOf("classpath://not/exists/resource.xml").getInputStream();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }

        try {
            ResourceLocator.valueOf("file:///not/exists.file/").getInputStream();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }

        try {
            ResourceLocator.valueOf("forward:///any/path/").getInputStream();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }

        try {
            ResourceLocator.valueOf("servlet:///any/path/").getInputStream();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }

        try {
            ResourceLocator.valueOf("redirect:///any/path/").getInputStream();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }

        try {
            ResourceLocator.valueOf("http://hoge.com/any/path/").getInputStream();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }

        try {
            ResourceLocator.valueOf("https://hoge.com/any/path/").getInputStream();
            fail();
        } catch(Throwable e) {
            assertEquals(FileNotFoundException.class, e.getClass());
        }
    }

    @Test
    public void testIsRedirect() {
        assertFalse(ResourceLocator.valueOf("classpath://not/exists/resource.xml").isRedirect());
        assertFalse(ResourceLocator.valueOf("file:///not/exists.file").isRedirect());
        assertFalse(ResourceLocator.valueOf("forward:///any/path/").isRedirect());
        assertFalse(ResourceLocator.valueOf("servlet:///any/path/").isRedirect());
        assertTrue(ResourceLocator.valueOf("redirect://hoge/fuga").isRedirect());
        assertTrue(ResourceLocator.valueOf("http://foo.var/").isRedirect());
        assertTrue(ResourceLocator.valueOf("https://foo.var/").isRedirect());
    }
    
    /**
     * OS名を取得する。
     * @return OS名
     */
    private String getOsName() {
        return System.getProperty("os.name").toLowerCase();
    }
}
