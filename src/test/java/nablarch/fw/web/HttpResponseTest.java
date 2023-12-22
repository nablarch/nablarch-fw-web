package nablarch.fw.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import nablarch.common.web.WebConfig;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.Builder;
import nablarch.test.support.tool.Hereis;

import org.junit.Before;
import org.junit.Test;

import jakarta.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpResponseTest {

    @Before
    public void setUp() {
        SystemRepository.clear();
    }

    @SuppressWarnings("DanglingJavadoc")
    @Test
    public void testDefaultConstructorAndAccessorsWorkProperly() {
        HttpResponse res = new HttpResponse();
        
        assertEquals(200       , res.getStatusCode());
        assertEquals("OK"      , res.getReasonPhrase());
        assertEquals("HTTP/1.1", res.getHttpVersion());
        assertNull(res.getContentType());
        assertEquals(Charset.forName("UTF-8"), res.getCharset());
        /**************************************
        HTTP/1.1 200 OK
        Content-Length: 0
        Content-Type: text/html;charset=UTF-8
        **************************************/
        
        res.setContentType("text/plain;charset=us-ascii");
        assertEquals(Charset.forName("us-ascii"), res.getCharset());
        res.setContentType("text/plain;charset= SJIS");
        assertEquals(Charset.forName("sjis"), res.getCharset());
        res.setContentType("text/plain;charset= us-ascii ");
        assertEquals(Charset.forName("us-ascii"), res.getCharset());
        res.setContentType("text/plain;charset= \"SJIS\" ");
        assertEquals(Charset.forName("sjis"), res.getCharset());
        
        res.setContentType("text/plain ; charset= \"utf-8\" ");
        assertEquals(Charset.forName("utf-8"), res.getCharset());

        res.setContentType(null);
        assertEquals(Charset.forName("utf-8"), res.getCharset());
    }

    @Test
    public void testAccessorsToHtttpVersion() {
        HttpResponse res = new HttpResponse();
        assertEquals("HTTP/1.1", res.getHttpVersion());
        res.setHttpVersion("HTTP/0.9");
        assertEquals("HTTP/0.9", res.getHttpVersion());
        
        try {
            res.setHttpVersion("HTTP1.1");
            fail();
        } catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testAccessorsToHttpStatus() {
        HttpResponse res = new HttpResponse();
        assertEquals(200, res.getStatusCode());
        
        res.setStatusCode(HttpResponse.Status.BAD_REQUEST.getStatusCode());
        assertEquals(400, res.getStatusCode());
        assertEquals(400, HttpResponse.Status.BAD_REQUEST.handle(null, null).getStatusCode());

        try {
            res.setStatusCode(10);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        
        try {
            res.setStatusCode(1000);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        res = new HttpResponse("forward:///any/path/");
        assertEquals(200, res.getStatusCode());
        res = new HttpResponse("http://foo.var/");
        assertEquals(302, res.getStatusCode());
        res = new HttpResponse("https://foo.var/");
        assertEquals(302, res.getStatusCode());
        res = new HttpResponse("redirect://xxx/yyy");
        assertEquals(302, res.getStatusCode());

    }

    @Test
    public void testAccessorsToHttpStatus5u13NewStyleRedirection() {
        HttpResponse res = new HttpResponse("redirect:http://action/menu");
        assertEquals(302, res.getStatusCode());

        res = new HttpResponse("redirect:URN:ISBN:978-4-7741-6931-6");
        assertEquals(302, res.getStatusCode());
    }

    @Test
    public void testGetMessage() {
        assertEquals("200: OK", new HttpResponse().getMessage());
        assertEquals("400: BAD_REQUEST", new HttpResponse(400).getMessage());
    }

    @Test
    public void testAccessorsToTransferEncoding() {
        HttpResponse res = new HttpResponse();
        assertNull(res.getTransferEncoding());
        res.setTransferEncoding("chunked");
        assertEquals("chunked", res.getTransferEncoding());
    }

    @Test
    public void testAccessorsToHttpCookie() {

        // 複数のクッキーが設定された場合
        HttpCookie cookie1 = new HttpCookie();
        cookie1.put("foo", "bar"); // 先頭を固定したいので1個だけ設定
        HttpResponse res = new HttpResponse().addCookie(cookie1);

        HttpCookie cookie2 = new HttpCookie();
        cookie2.put("hoge", "hogehoge");
        cookie2.put("fuga", "fugafuga");
        res = res.addCookie(cookie2);

        HttpCookie cookie3 = new HttpCookie();
        cookie3.put("egg", "egg");
        //noinspection deprecation
        res = res.setCookie(cookie3);

        List<Cookie> list = res.getCookieList();
        for (Cookie cookie : list) {
            if ("hoge".equals(cookie.getName())) {
                assertEquals(cookie.getValue(), "hogehoge");
            } else if ("huga".equals(cookie.getName())) {
                assertEquals(cookie.getValue(), "fugafuga");
            } else if ("foo".equals(cookie.getName())) {
                assertEquals(cookie.getValue(), "bar");
            } else if ("egg".equals(cookie.getName())) {
                assertEquals(cookie.getValue(), "egg");
            }
        }

        //noinspection deprecation
        Map.Entry<String, String> result = res.getCookie().entrySet().iterator().next();
        assertEquals(result.getKey(), "foo");
        assertEquals(result.getValue(), "bar");

        // クッキーが設定されなかった場合
        res = new HttpResponse();
        assertTrue(res.getCookieList().isEmpty());
        //noinspection deprecation
        assertNull(res.getCookie());
     }

    @SuppressWarnings("DanglingJavadoc")
    @Test
    public void testWritingToBodyBuffer() {
        
        HttpResponse res = new HttpResponse();
        assertEquals("0", res.getContentLength());
        
        res.write("Hello world!\n");

        assertEquals("Hello world!\n", res.getBodyString());
        assertEquals(
            Hereis.string().trim()
            /**************************************
            HTTP/1.1 200 OK
            Content-Length: 13
            Content-Type: text/plain;charset=UTF-8
            
            Hello world!
            *************************************/
          , res.toString().trim().replaceAll("\r\n", Builder.LS)
        );
    }


    /** ストリームにbyte配列を書き出し、toStringで内容を確認する。 */
    @Test
    public void testWritingToBodyOutputStream() throws Exception {
        HttpResponse res = new HttpResponse();
        String expectedString = "Hello world!\n" + "Hello world2!\n" + "Hello world3!\n";
        byte[] expectedBytes = expectedString.getBytes();
        
        res.write(expectedBytes);

        byte[] bytes = new byte[expectedBytes.length];
        InputStream input = res.getBodyStream();
        //noinspection ResultOfMethodCallIgnored
        input.read(bytes);
        
        assertEquals(expectedBytes.length, bytes.length);
        
        for(int i = 0; i < expectedBytes.length; i++){
            if(expectedBytes[i] != bytes[i]) fail();
        }
        
        assertTrue(res.toString().contains(expectedString));
    }

    @SuppressWarnings("DanglingJavadoc")
    @Test
    public void testParsingHttpResponseMessage() {
        HttpResponse res = HttpResponse.parse(Hereis.string());
        /***********************
        HTTP/1.1 200 OK
        ************************/
        assertEquals(200                       , res.getStatusCode());
        assertEquals("OK"                      , res.getReasonPhrase());
        assertNull(res.getContentType());
        assertEquals("HTTP/1.1"                , res.getHttpVersion());
    
        res = HttpResponse.parse(Hereis.string());
        /***********************
        HTTP/1.1 200 OK
        Content-Type: text/html
        
        Hello world!
        ************************/

        assertEquals(200             , res.getStatusCode());
        assertEquals("OK"            , res.getReasonPhrase());
        assertEquals("text/html"     , res.getContentType());
        assertEquals("HTTP/1.1"      , res.getHttpVersion());
        assertEquals("Hello world!"  , res.getBodyString().trim());
    }

    @SuppressWarnings("DanglingJavadoc")
    @Test
    public void testParsingMultilineHeaders() {
        HttpResponse res = HttpResponse.parse(Hereis.string());
        /***********************************************
        HTTP/1.1 400 Bad Request
        Host: www.example.com
        To: person1@domain1.org, person2@domain2.com,
            person3@domain3.net, person4@domain4.edu
        Content-Type: text/plain;charset=UTF-8
        
        Hello world!
        Hello world!
        ************************************************/
        
        assertEquals(400                        , res.getStatusCode());
        assertEquals("text/plain;charset=UTF-8" , res.getContentType());
        assertEquals("HTTP/1.1"                 , res.getHttpVersion());
        assertEquals(
            "Hello world!\nHello world!",
            res.getBodyString().trim()
        );
        
        assertEquals(4, res.getHeaderMap().size());
        String[] customHeader = res.getHeader("To").split("\\s*,\\s*");
        assertEquals(4, customHeader.length);
        assertEquals("person1@domain1.org", customHeader[0]);
        assertEquals("person4@domain4.edu", customHeader[3]);
    }

    @Test
    public void testConvertingServletCookieToHttpCookie() {
        HttpResponse res = new HttpResponse();
        res.addCookie(new CookieBuilder("cookie00", "value00").build());
        res.addCookie(new CookieBuilder("cookie01", "value01").setMaxAge(3600).build());
        res.addCookie(new CookieBuilder("cookie02", "value02").setDomain("example.com").build());
        res.addCookie(new CookieBuilder("cookie03", "value03").setPath("/").build());
        res.addCookie(new CookieBuilder("cookie04", "value04").setSecure(true).build());

        List<HttpCookie> results = res.getHttpCookies();
        assertEquals(5, results.size());

        for(HttpCookie cookie : results) {
            if (cookie.containsKey("cookie00")) {
                assertEquals("value00", cookie.get("cookie00"));
                assertNull(cookie.getMaxAge());
                assertNull(cookie.getDomain());
                assertNull(cookie.getPath());
                assertFalse(cookie.isSecure());
            } else if (cookie.containsKey("cookie01")) {
                assertEquals("value01", cookie.get("cookie01"));
                assertEquals(3600, (int) cookie.getMaxAge());
                assertNull(cookie.getDomain());
                assertNull(cookie.getPath());
                assertFalse(cookie.isSecure());
            } else if (cookie.containsKey("cookie02")) {
                assertEquals("value02", cookie.get("cookie02"));
                assertNull(cookie.getMaxAge());
                assertEquals("example.com", cookie.getDomain());
                assertNull(cookie.getPath());
                assertFalse(cookie.isSecure());
            } else if (cookie.containsKey("cookie03")) {
                assertEquals("value03", cookie.get("cookie03"));
                assertNull(cookie.getMaxAge());
                assertNull(cookie.getDomain());
                assertEquals("/", cookie.getPath());
                assertFalse(cookie.isSecure());
            } else if (cookie.containsKey("cookie04")) {
                assertEquals("value04", cookie.get("cookie04"));
                assertNull(cookie.getMaxAge());
                assertNull(cookie.getDomain());
                assertNull(cookie.getPath());
                assertTrue(cookie.isSecure());
            } else {
                fail();
            }

        }
    }

    private static class CookieBuilder {
        private final HttpCookie cookie;
        public CookieBuilder(String key, String value) {
            cookie = new HttpCookie();
            cookie.put(key, value);
        }
        public CookieBuilder setMaxAge(int maxAge) {
            cookie.setMaxAge(maxAge);
            return this;
        }
        public CookieBuilder setDomain(String domain) {
            cookie.setDomain(domain);
            return this;
        }
        public CookieBuilder setPath(String path) {
            cookie.setPath(path);
            return this;
        }
        public CookieBuilder setSecure(boolean secure) {
            cookie.setSecure(secure);
            return this;
        }
        public HttpCookie build() {
            return cookie;
        }
    }


    @SuppressWarnings("DanglingJavadoc")
    @Test
    public void testParsingMultilineSetCookieHeaders() {
        HttpResponse res = HttpResponse.parse(Hereis.string());
        /***********************************************
        HTTP/1.1 400 Bad Request
        Set-Cookie: cookie00="value00"
        Set-Cookie: cookie01=value01; Max-Age=3600; Path=/; Domain=example.com; Secure; HttpOnly

        Hello world!
        ************************************************/

        assertEquals(400                        , res.getStatusCode());
        assertEquals("HTTP/1.1"                 , res.getHttpVersion());
        assertEquals("Hello world!"             , res.getBodyString().trim());
        assertEquals(3, res.getHeaderMap().size());

        assertEquals(2, res.getCookieList().size());
        assertEquals("cookie00", res.getCookieList().get(0).getName());
        assertEquals("value00", res.getCookieList().get(0).getValue());

        assertEquals("cookie01", res.getCookieList().get(1).getName());
        assertEquals("value01", res.getCookieList().get(1).getValue());
        assertEquals(3600, res.getCookieList().get(1).getMaxAge());
        assertEquals("/", res.getCookieList().get(1).getPath());
        assertEquals("example.com", res.getCookieList().get(1).getDomain());
        assertTrue(res.getCookieList().get(1).getSecure());
        // 以下は実行できない
        // assertEquals(true, Cookie.class.getMethod("isHttpOnly").invoke(res.getCookieList().get(1)));
    }

    @SuppressWarnings("DanglingJavadoc")
    @Test
    public void testThrowsErrorWhenItReadsIllegalResponseFormat() {
        try {
            HttpResponse res = HttpResponse.parse(Hereis.string());
            /************************
            HTTP1.1 400 Bad Request
            Host: www.example.com
            
            Hello world!
            Hello world!
            *************************/
            fail();
        } catch (RuntimeException e) {
            assertTrue(true);
        }
        
        try {
            HttpResponse res = HttpResponse.parse(Hereis.string());
            /*************************
            HTTP/1.1 400 Bad Request
            Host/ www.example.com
            **************************/
            fail();
        } catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testSetContentPath() {
        HttpResponse res = new HttpResponse();
        res.setContentPath("servlet://test/hoge.jsp");
        assertEquals("servlet://test/hoge.jsp", res.getContentPath().toString());
    }

    @Test
    public void testSetContentType() {
        HttpResponse res = new HttpResponse();
        res.setHeader("Content-Type", null);
        res.setContentType("text/csv: charset=Shift_JIS");
        assertEquals("text/csv: charset=Shift_JIS", res.getHeader("Content-Type"));
    }

    @Test
    public void testGetContentType() {
        HttpResponse res = new HttpResponse();
        res.setHeader("Content-Type", "text/csv: charset=Shift_JIS");
       assertEquals("text/csv: charset=Shift_JIS", res.getContentType());
    }

    @Test
    public void testGetContentTypeAfterGetBodyStream() {
        HttpResponse res = new HttpResponse();
        res.getBodyStream();
        assertNull(res.getContentType());
    }

    @Test
    public void testGetContentTypeAfterSetContentPath() {
        HttpResponse res = new HttpResponse();
        res.setContentPath("");
        assertEquals("application/octet-stream",res.getContentType());
    }

    @Test
    public void testGetContentTypeAfterSetBodyStream() {
        HttpResponse res = new HttpResponse();
        ByteArrayInputStream inputStream = new ByteArrayInputStream("テスト".getBytes());
        res.setBodyStream(inputStream);
        assertEquals("text/plain;charset=UTF-8",res.getContentType());
    }

    @Test
    public void testGetContentTypeAfterWrite() {
        HttpResponse res = new HttpResponse();
        byte[] expectedBytes = "Hello world!".getBytes();
        res.write(expectedBytes);
        assertEquals("text/plain;charset=UTF-8",res.getContentType());
    }

    @Test
    public void testGetContentTypeNullAddDefaultContentTypeForNoBodyResponseDefault() {
        HttpResponse res = new HttpResponse();
        assertNull(res.getContentType());
    }

    @SuppressWarnings("DanglingJavadoc")
    @Test
    public void testGetContentTypeExistBodyWithAddDefaultContentTypeForNoBodyResponseDefault() {
        HttpResponse res = HttpResponse.parse(Hereis.string());
        /***********************
         HTTP/1.1 200 OK

         Hello world!
         ************************/
        assertEquals("text/plain;charset=UTF-8", res.getContentType());
    }

    @Test
    public void testGetContentTypeWithAddDefaultContentTypeForNoBodyResponseTrue() {
        final WebConfig webConfig = new WebConfig();
        webConfig.setAddDefaultContentTypeForNoBodyResponse(true);
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                final Map<String, Object> result = new HashMap<String, Object>();
                result.put("webConfig", webConfig);
                return result;
            }
        });

        HttpResponse res = new HttpResponse();
        assertEquals("text/plain;charset=UTF-8", res.getContentType());
    }

    @Test
    public void testSetContentDisposition() {
        HttpResponse res = new HttpResponse();
        res.setHeader("Content-Disposition", null);
        res.setHeader("Content-Type", null);
        
        res.setContentDisposition("テストファイル①.css");
        assertEquals("attachment; filename=\"テストファイル①.css\"", res.getHeader("Content-Disposition"));
        assertEquals("text/css", res.getHeader("Content-Type"));
        
    }

    @Test
    public void testSetContentDispositionAttachment() {
        HttpResponse res = new HttpResponse();
        res.setHeader("Content-Disposition", null);
        res.setHeader("Content-Type", null);
        
        res.setContentDisposition("テストファイル②.css", false);
        assertEquals("attachment; filename=\"テストファイル②.css\"", res.getHeader("Content-Disposition"));
        assertEquals("text/css", res.getHeader("Content-Type"));
    }

    @Test
    public void testSetContentDispositionInline() {
        HttpResponse res = new HttpResponse();
        res.setHeader("Content-Disposition", null);
        res.setHeader("Content-Type", null);
        
        res.setContentDisposition("テストファイル③.jpg", true);
        assertEquals("inline; filename=\"テストファイル③.jpg\"", res.getHeader("Content-Disposition"));
        assertEquals("image/jpeg", res.getHeader("Content-Type"));
    }

    @Test
    public void testSetContentDispositionAfterSetContentType() {
        HttpResponse res = new HttpResponse();
        res.setHeader("Content-Type", "text/csv: charset=Shift_JIS");
        res.setHeader("Content-Disposition", null);
        
        res.setContentDisposition("テストファイル①.csv");
        assertEquals("attachment; filename=\"テストファイル①.csv\"", res.getHeader("Content-Disposition"));
        assertEquals("text/csv: charset=Shift_JIS", res.getHeader("Content-Type"));
    }

    @Test
    public void testSetBodyStream() {
        HttpResponse res = new HttpResponse();
        ByteArrayInputStream inputStream = new ByteArrayInputStream("テスト".getBytes());
        res.setBodyStream(inputStream);

        assertSame(inputStream, res.getBodyStream());
    }

}
