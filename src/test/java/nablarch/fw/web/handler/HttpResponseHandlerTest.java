package nablarch.fw.web.handler;

import nablarch.TestUtil;
import nablarch.common.handler.threadcontext.ThreadContextHandler;
import nablarch.common.web.handler.HttpAccessLogHandler;
import nablarch.common.web.handler.threadcontext.LanguageAttributeInHttpCookie;
import nablarch.common.web.handler.threadcontext.LanguageAttributeInHttpUtil;
import nablarch.core.ThreadContext;
import nablarch.core.log.LogUtil;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.download.encorder.DownloadFileNameEncoderEntry;
import nablarch.fw.web.download.encorder.DownloadFileNameEncoderFactory;
import nablarch.fw.web.download.encorder.MimeBDownloadFileNameEncoder;
import nablarch.fw.web.download.encorder.UrlDownloadFileNameEncoder;
import nablarch.fw.web.i18n.DirectoryBasedResourcePathRule;
import nablarch.fw.web.i18n.FilenameBasedResourcePathRule;
import nablarch.fw.web.i18n.MockServletContextCreator;
import nablarch.test.support.log.app.OnMemoryLogWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import static nablarch.test.support.tool.Hereis.string;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

/**
 * {@link HttpResponseHandlerTest}テスト。
 * @author Kiyohito Itoh
 */
public class HttpResponseHandlerTest {
    @Before
    public void setUp() {
        //ステータスコンバートのテストで使用するために、フォーマットを再設定する。
        LogUtil.removeAllObjectsBoundToContextClassLoader();
        System.setProperty(
                "httpAccessLogFormatter.endFormat",
                "max_memory = [$maxMemory$] free_memory = [$freeMemory$]"
                        + " start_time = [$startTime$] end_time = [$endTime$] execution_time = [$executionTime$]"
                        + " < sid = [$sessionId$] @@@@ END @@@@ url = [$url$] status_code = [$statusCode$] content_path = [$contentPath$]"
                        + " response_status_code = [$responseStatusCode$]");

        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("nablarch/fw/web/handler/cookie.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        ThreadContext.clear();
    }

    @After
    public void tearDown(){
        SystemRepository.clear();
        //ログのフォーマットのリセット。
        System.clearProperty("httpAccessLogFormatter.endFormat");
        LogUtil.removeAllObjectsBoundToContextClassLoader();
    }

    /**
     * サーブレットフォーワードのテスト。
     * JSPでSocketExceptionが発生した場合にFATALログが出力されないこと。
     * http://25.32.36.1/redmine/issues/5524
     */
    @Test
    public void testDoServletForwardForSocketException() {

        HttpServer server = TestUtil.createHttpServer()
            // ファイルで分けた場合のルートコンテキスト
            .setWarBasePath("classpath://nablarch/fw/web/handler/httpresponsehandler/socketexception")
            .addHandler("/*.do", new Object() {
                // /forward.do
                public HttpResponse getforwarddo(HttpRequest req, ExecutionContext ctx) {
                    String path = req.getParam("path")[0];
                    return new HttpResponse(path);
                }
            })
            .startLocal();

        OnMemoryLogWriter.clear();

        HttpResponse res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=runtime_exception.jsp HTTP/1.1
        ************************************************************/

        assertTrue(OnMemoryLogWriter.getMessages("writer.appLog").toString().contains("FATAL"));
        assertFalse(OnMemoryLogWriter.getMessages("writer.appLog").toString().contains("WARN"));
        assertThat(res.getStatusCode(), is(500));

        OnMemoryLogWriter.clear();

        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=socket_exception.jsp HTTP/1.1
        ************************************************************/

        assertFalse(OnMemoryLogWriter.getMessages("writer.appLog").toString().contains("FATAL"));
        assertTrue(OnMemoryLogWriter.getMessages("writer.appLog").toString().contains("WARN"));
        assertThat(res.getStatusCode(), is(500));
    }

    /**
     * サーブレットフォーワードのテスト。
     * 言語毎にファイルを分ける場合。
     */
    @Test
    public void testDoServletForwardForFilename() {
        
        LanguageAttributeInHttpCookie attribute = SystemRepository.get("languageAttribute");
        
        HttpServer server = TestUtil.createHttpServer()
            // ファイルで分けた場合のルートコンテキスト
            .setWarBasePath("classpath://nablarch/fw/web/handler/httpresponsehandler/filename")
            .addHandler(new ThreadContextHandler(attribute))
            .addHandler(new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                    LanguageAttributeInHttpUtil.keepLanguage(request, context, request.getParam("lang")[0]);
                    return context.handleNext(request);
                }
            })
            .addHandler("//*.css", new ResourceMapping("/", "servlet:///"))
            .addHandler("/*.do", new Object() {
                // /forward.do
                public HttpResponse getforwarddo(HttpRequest req, ExecutionContext ctx) {
                    String path = req.getParam("path")[0];
                    return new HttpResponse(path);
                }

                // /noextension.do
                public HttpResponse getnoextensiondo(HttpRequest req, ExecutionContext ctx) {
                    return new HttpResponse("404");
                }
            })
            .startLocal();
        
        // 言語毎にファイルを分ける場合の設定
        for (Handler<?, ?> h : server.getHandlerQueue()) {
            if (h instanceof HttpResponseHandler) {
                ((HttpResponseHandler) h).setContentPathRule(new FilenameBasedResourcePathRule(){{setServletContextCreator(new MockServletContextCreator());}});
                break;
            }
        }
        
        HttpResponse res;
        
        // URL encoding
        // "/i18n.jsp" -> "%2fi18n%2ejsp"
        // "i18n_default_only.jsp" -> "i18n_default_only%2ejsp"
        // "/i18n.css" -> "%2fi18n%2ecss"
        
        /*---------------------------------------------------------------------
        言語がjaの場合。(JSP)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=%2fi18n%2ejsp&lang=ja HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("jaのファイルが使用される。",
                   res.getBodyString().trim(), is("日本語のJSPファイル"));

        /*---------------------------------------------------------------------
        言語がesの場合。(JSP、リクエストURIと同じ階層の相対パス)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=i18n%2ejsp&lang=es HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("esのファイルが使用される。",
                   res.getBodyString().trim(), is("スペイン語のJSPファイル"));

        /*---------------------------------------------------------------------
        言語がesの場合。(JSP、リクエストURIより下の階層の相対パス)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=aaa%2fbbb%2fi18n%2ejsp&lang=es HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("esのファイルが使用される。",
                   res.getBodyString().trim(), is("スペイン語のJSPファイル(/aaa/bbb/i18n_es.jsp)"));
        
        /*---------------------------------------------------------------------
        言語がenの場合。(JSP)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=%2fi18n%2ejsp&lang=en HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("enのファイルが存在しないためデフォルトのファイルが使用される。",
                   res.getBodyString().trim(), is("デフォルトのJSPファイル"));

        /*---------------------------------------------------------------------
        デフォルトファイルしか存在しない場合(JSP)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=%2fi18n_default_only%2ejsp&lang=en HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("デフォルトのファイルが使用される。",
                   res.getBodyString().trim(), is("デフォルトしかないJSPファイル"));
        
        /*---------------------------------------------------------------------
        言語がjaの場合。(静的ファイル)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /i18n.css?lang=ja HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("jaのファイルが存在しないためデフォルトのファイルが使用される。",
                   res.getBodyString().trim(), is(".default {}"));
        
        /*---------------------------------------------------------------------
        言語がesの場合。(静的ファイル)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /i18n.css?lang=es HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("esのファイルが使用される。",
                   res.getBodyString().trim(), is(".spanish {}"));

        /*---------------------------------------------------------------------
        言語がenの場合。(静的ファイル)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /i18n.css?lang=en HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("enのファイルが存在しないためデフォルトのファイルが使用される。",
                   res.getBodyString().trim(), is(".default {}"));
        
        /*---------------------------------------------------------------------
        拡張子がないパスの場合。(JSP)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /noextension.do?lang=ja HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(404));
    }
    
    /**
     * サーブレットフォーワードのテスト。
     * 言語毎にディレクトリを分ける場合。
     */
    @Test
    public void testDoServletForwardForDirectory() {
        
        LanguageAttributeInHttpCookie attribute = SystemRepository.get("languageAttribute");
        
        HttpServer server = TestUtil.createHttpServer()
            // ディレクトリで分けた場合のルートコンテキスト
            .setWarBasePath("classpath://nablarch/fw/web/handler/httpresponsehandler/directory")
            .addHandler(new ThreadContextHandler(attribute))
            .addHandler(new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                    LanguageAttributeInHttpUtil.keepLanguage(request, context, request.getParam("lang")[0]);
                    return context.handleNext(request);
                }
            })
            .addHandler("//*.css", new ResourceMapping("/", "servlet:///"))
            .addHandler("/*.do", new Object() {
                // /forward.do
                public HttpResponse getforwarddo(HttpRequest req, ExecutionContext ctx) {
                    String path = req.getParam("path")[0];
                    return new HttpResponse(path);
                }

                // /noextension.do
                public HttpResponse getnoextensiondo(HttpRequest req, ExecutionContext ctx) {
                    return new HttpResponse("404");
                }
            })
            .startLocal();
        
        // 言語毎にディレクトリを分ける場合の設定
        for (Handler<?, ?> h : server.getHandlerQueue()) {
            if (h instanceof HttpResponseHandler) {
                ((HttpResponseHandler) h).setContentPathRule(new DirectoryBasedResourcePathRule(){{setServletContextCreator(new MockServletContextCreator());}});
                break;
            }
        }
        
        HttpResponse res;
        
        // URL encoding
        // "/i18n.jsp" -> "%2fi18n%2ejsp"
        // "i18n_default_only.jsp" -> "i18n_default_only%2ejsp"
        // "/i18n.css" -> "%2fi18n%2ecss"
        
        /*---------------------------------------------------------------------
        言語がjaの場合。(JSP)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=%2fi18n%2ejsp&lang=ja HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("jaのファイルが使用される。",
                   res.getBodyString().trim(), is("日本語のJSPファイル"));

        /*---------------------------------------------------------------------
        言語がesの場合。(JSP、リクエストURIと同じ階層の相対パス)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=i18n%2ejsp&lang=es HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("esのファイルが使用される。",
                   res.getBodyString().trim(), is("スペイン語のJSPファイル"));

        /*---------------------------------------------------------------------
        言語がesの場合。(JSP、リクエストURIより下の階層の相対パス)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=aaa%2fbbb%2fi18n%2ejsp&lang=es HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("esのファイルが使用される。",
                   res.getBodyString().trim(), is("スペイン語のJSPファイル(/aaa/bbb/i18n_es.jsp)"));
        
        /*---------------------------------------------------------------------
        言語がenの場合。(JSP)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=%2fi18n%2ejsp&lang=en HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("enのファイルが存在しないためデフォルトのファイルが使用される。",
                   res.getBodyString().trim(), is("デフォルトのJSPファイル"));

        /*---------------------------------------------------------------------
        デフォルトファイルしか存在しない場合(JSP)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=%2fi18n_default_only%2ejsp&lang=en HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("デフォルトのファイルが使用される。",
                   res.getBodyString().trim(), is("デフォルトしかないJSPファイル"));
        
        /*---------------------------------------------------------------------
        言語がjaの場合。(静的ファイル)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /i18n.css?lang=ja HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("jaのファイルが存在しないためデフォルトのファイルが使用される。",
                   res.getBodyString().trim(), is(".default {}"));
        
        /*---------------------------------------------------------------------
        言語がesの場合。(静的ファイル)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /i18n.css?lang=es HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("esのファイルが使用される。",
                   res.getBodyString().trim(), is(".spanish {}"));

        /*---------------------------------------------------------------------
        言語がenの場合。(静的ファイル)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /i18n.css?lang=en HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("enのファイルが存在しないためデフォルトのファイルが使用される。",
                   res.getBodyString().trim(), is(".default {}"));
        
        /*---------------------------------------------------------------------
        拡張子がないパスの場合。(JSP)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /noextension.do?lang=ja HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(404));
    }

    /**
     * サーブレットフォーワードのテスト。
     * デフォルト設定(言語毎にディレクトリを分ける場合)
     */
    @Test
    public void testDoServletForwardForDefault() {
        
        LanguageAttributeInHttpCookie attribute = SystemRepository.get("languageAttribute");
        
        HttpServer server = TestUtil.createHttpServer()
            // ディレクトリで分けた場合のルートコンテキスト
            .setWarBasePath("classpath://nablarch/fw/web/handler/httpresponsehandler/directory")
            .addHandler(new ThreadContextHandler(attribute))
            .addHandler(new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                    LanguageAttributeInHttpUtil.keepLanguage(request, context, request.getParam("lang")[0]);
                    return context.handleNext(request);
                }
            })
            .addHandler("//*.css", new ResourceMapping("/", "servlet:///"))
            .addHandler("/*.do", new Object() {
                // /forward.do
                public HttpResponse getforwarddo(HttpRequest req, ExecutionContext ctx) {
                    String path = req.getParam("path")[0];
                    return new HttpResponse(path);
                }

                // /noextension.do
                public HttpResponse getnoextensiondo(HttpRequest req, ExecutionContext ctx) {
                    return new HttpResponse("404");
                }
            })
            .startLocal();

        for (Handler<?, ?> h : server.getHandlerQueue()) {
            if (h instanceof HttpResponseHandler) {
                ((HttpResponseHandler) h).setContentPathRule(new DirectoryBasedResourcePathRule(){{setServletContextCreator(new MockServletContextCreator());}});
                break;
            }
        }
        
        HttpResponse res;
        
        // URL encoding
        // "/i18n.jsp" -> "%2fi18n%2ejsp"
        // "i18n_default_only.jsp" -> "i18n_default_only%2ejsp"
        // "/i18n.css" -> "%2fi18n%2ecss"
        
        /*---------------------------------------------------------------------
        言語がjaの場合。(JSP)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=%2fi18n%2ejsp&lang=ja HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("jaのファイルが使用される。",
                   res.getBodyString().trim(), is("日本語のJSPファイル"));

        /*---------------------------------------------------------------------
        言語がesの場合。(JSP、リクエストURIと同じ階層の相対パス)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=i18n%2ejsp&lang=es HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("esのファイルが使用される。",
                   res.getBodyString().trim(), is("スペイン語のJSPファイル"));

        /*---------------------------------------------------------------------
        言語がesの場合。(JSP、リクエストURIより下の階層の相対パス)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=aaa%2fbbb%2fi18n%2ejsp&lang=es HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("esのファイルが使用される。",
                   res.getBodyString().trim(), is("スペイン語のJSPファイル(/aaa/bbb/i18n_es.jsp)"));
        
        /*---------------------------------------------------------------------
        言語がenの場合。(JSP)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=%2fi18n%2ejsp&lang=en HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("enのファイルが存在しないためデフォルトのファイルが使用される。",
                   res.getBodyString().trim(), is("デフォルトのJSPファイル"));

        /*---------------------------------------------------------------------
        デフォルトファイルしか存在しない場合(JSP)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do?path=%2fi18n_default_only%2ejsp&lang=en HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("デフォルトのファイルが使用される。",
                   res.getBodyString().trim(), is("デフォルトしかないJSPファイル"));
        
        /*---------------------------------------------------------------------
        言語がjaの場合。(静的ファイル)
        ---------------------------------------------------------------------*/
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /i18n.css?lang=ja HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("jaのファイルが存在しないためデフォルトのファイルが使用される。",
                   res.getBodyString().trim(), is(".default {}"));
        
        /*---------------------------------------------------------------------
        言語がesの場合。(静的ファイル)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /i18n.css?lang=es HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("esのファイルが使用される。",
                   res.getBodyString().trim(), is(".spanish {}"));

        /*---------------------------------------------------------------------
        言語がenの場合。(静的ファイル)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /i18n.css?lang=en HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(200));
        assertThat("enのファイルが存在しないためデフォルトのファイルが使用される。",
                   res.getBodyString().trim(), is(".default {}"));
        
        /*---------------------------------------------------------------------
        拡張子がないパスの場合。(JSP)
        ---------------------------------------------------------------------*/
        
        res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /noextension.do?lang=ja HTTP/1.1
        ************************************************************/
        
        assertThat(res.getStatusCode(), is(404));
    }
    
    /**
     * レスポンスオブジェクトのContent-Typeヘッダに文字セットが設定されていた場合は、
     * その文字セットに対応するコンバータを用いてエンコーディングされることを確認。  
     */
    @Test
    public void testHandlingOfCharacterEncoding() throws Exception {
        HttpServer server = TestUtil.createHttpServer()
            .addHandler(new Object() {
                public HttpResponse getTest1(HttpRequest req, ExecutionContext ctx) {
                    return new HttpResponse()
                            .setStatusCode(200)
                            .setContentType("text/plain;charset=shift-jis")
                            .write("てすと１データ");
                }

                public HttpResponse getTest2(HttpRequest req, ExecutionContext ctx) {
                    return new HttpResponse()
                            .setStatusCode(200)
                            .setContentType("text/plain;charset=utf-8")
                            .write("てすと２データ");
                }

                public HttpResponse getTest3(HttpRequest req, ExecutionContext ctx) {
                    return new HttpResponse()
                            .setStatusCode(200)
                            .write("てすと３データ");
                }
            }).startLocal();
        
        
        //------------------ Shift-JIS エンコーディング ------------- //
        HttpResponse res = server.handle(
                new MockHttpRequest("GET /test1 HTTP/1.1")
                , null
        );
        
        String line = new BufferedReader(
            new InputStreamReader(res.getBodyStream(), Charset.forName("sjis"))
        ).readLine();
        
        assertEquals("てすと１データ", line);
        
        //------------------ UTF-8 エンコーディング ------------- //
        res = server.handle(
                new MockHttpRequest("GET /test2 HTTP/1.1")
                , null
        );
        
        line = new BufferedReader(
            new InputStreamReader(res.getBodyStream(), Charset.forName("utf-8"))
        ).readLine();
        
        assertEquals("てすと２データ", line);
        
        
        //--------------- デフォルトではUTF8を使用 ------------- //
        res = server.handle(
                new MockHttpRequest("GET /test3 HTTP/1.1")
                , null
        );
        
        line = new BufferedReader(
            new InputStreamReader(res.getBodyStream(), Charset.forName("utf-8"))
        ).readLine();
        
        assertEquals("てすと３データ", line);
    }
    
    /**
     * レスポンスオブジェクトのContent-Lengthヘッダに設定しない場合 
     */
    @Test
    public void testHandlingOfNoContentLengthSmall() throws Exception {
        HttpServer server = TestUtil.createHttpServer()
            .addHandler(new Object() {
                public HttpResponse getTest1(HttpRequest req, ExecutionContext ctx) {
                    return new HttpResponse()
                            .setStatusCode(200)
                            .setContentType("text/plain;charset=shift-jis")
                            .write("てすと１データ");
                }
            }).startLocal();
        
        HttpResponse res = server.handle(
                new MockHttpRequest("GET /test1 HTTP/1.1")
                , null
        );
        
        //HeaderにContent-Lengthが付与されていないこと。
        assertEquals(res.getHeader("Content-Length"), null);
        
    }
    
    /**
     * レスポンスオブジェクトのContent-Lengthヘッダに設定しない場合 
     */
    @Test
    public void testHandlingOfNoContentLengthLarge() throws Exception {
        HttpServer server = TestUtil.createHttpServer()
            .addHandler(new Object() {
                public HttpResponse getTest1(HttpRequest req, ExecutionContext ctx) {
                    return new HttpResponse()
                            .setStatusCode(200)
                            .setContentType("text/plain;charset=shift-jis")
                            .write(StringUtil.repeat("1234567890", 100));
                }
            }).startLocal();
        
        HttpResponse res = server.handle(
                new MockHttpRequest("GET /test1 HTTP/1.1")
                , null
        );
        
        //HeaderにContent-Lengthが付与されていないこと。
        assertEquals(res.getHeader("Content-Length"), null);
    }

    /**
     * レスポンスオブジェクトのContent-Lengthヘッダに設定する場合 
     */
    @Test
    public void testHandlingOfContentLengthSmall() throws Exception {
        
        HttpServer server = TestUtil.createHttpServer();
        server.getHandlerOf(HttpResponseHandler.class).setForceFlushAfterWritingHeaders(false);
        server.addHandler(new Object() {
                public HttpResponse getTest1(HttpRequest req, ExecutionContext ctx) {
                    return new HttpResponse()
                            .setStatusCode(200)
                            .setContentType("text/plain;charset=shift-jis")
                            .write("てすと１データ");
                }
            }).startLocal();
        
        //------------------ writeのパターン ------------- //
        HttpResponse res = server.handle(
                new MockHttpRequest("GET /test1 HTTP/1.1")
                , null
        );
        
        //HeaderにContent-Lengthが付与されていること
        assertEquals("てすと１データ".getBytes("shift-jis").length, Integer.parseInt(res.getHeader("Content-Length")));
    }
    
    /**
     * レスポンスオブジェクトのContent-Lengthヘッダに設定する場合 
     */
    @Test
    public void testHandlingOfContentLengthLarge() throws Exception {
        
        HttpServer server = TestUtil.createHttpServer();
        server.getHandlerOf(HttpResponseHandler.class).setForceFlushAfterWritingHeaders(false);
        server.addHandler(new Object() {
                public HttpResponse getTest1(HttpRequest req, ExecutionContext ctx) {
                    return new HttpResponse()
                            .setStatusCode(200)
                            .setContentType("text/plain;charset=shift-jis")
                            .write(StringUtil.repeat("1234567890", 2458));
                }
            }).startLocal();
        
        //------------------ writeのパターン ------------- //
        HttpResponse res = server.handle(
                new MockHttpRequest("GET /test1 HTTP/1.1")
                , null
        );
        
        //サイズが大きい場合はチャンクドエンコーディングが付与されていること
        assertEquals(res.getHeader("Content-Length"), null);
    }

    /**
     * レスポンスオブジェクトのContentTypeが未設定かつBodyが空で無い場合
     */
    @Test
    public void testHandlingOfContentTypeNone() throws Exception {
        HttpServer server = TestUtil.createHttpServer();
        server.getHandlerOf(HttpResponseHandler.class).setForceFlushAfterWritingHeaders(false);
        server.addHandler(new Object() {
            public HttpResponse getTest1(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse()
                        .setStatusCode(200)
                        .write("test string");
            }
        }).startLocal();

        //------------------ writeのパターン ------------- //
        HttpResponse res = server.handle(
                new MockHttpRequest("GET /test1 HTTP/1.1")
                , null
        );
        assertEquals("text/plain;charset=UTF-8", res.getHeader("Content-Type"));
    }

    /**
     * レスポンスオブジェクトのContentTypeが未設定かつBodyが空の場合
     */
    @Test
    public void testHandlingOfBodyEmptyContentTypeNone() throws Exception {
        HttpServer server = TestUtil.createHttpServer();
        server.getHandlerOf(HttpResponseHandler.class).setForceFlushAfterWritingHeaders(false);
        server.addHandler(new Object() {
            public HttpResponse getTest1(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse()
                        .setStatusCode(200);
            }
        }).startLocal();

        //------------------ writeのパターン ------------- //
        HttpResponse res = server.handle(
                new MockHttpRequest("GET /test1 HTTP/1.1")
                , null
        );
        assertNull(res.getHeader("Content-Type"));
    }
    
    /**
     * ダウンロードファイル名(Content-Dispositionヘッダ)の
     * エンコーディング処理のテスト
     */
    @Test
    public void testEncodingOfDownloadFileName() throws Exception {

        final DownloadFileNameEncoderEntry entry = new DownloadFileNameEncoderEntry();
        entry.setUserAgentPattern(".*MSIE.*");
        //省略時はURLエンコーダが利用される。
        
        final DownloadFileNameEncoderEntry entry1 = new DownloadFileNameEncoderEntry();
        entry1.setUserAgentPattern(".*WebKit.*");
        entry1.setEncoder(new UrlDownloadFileNameEncoder());
        
        final DownloadFileNameEncoderEntry entry2 = new DownloadFileNameEncoderEntry();
        entry2.setUserAgentPattern(".*Gecko.*");
        entry2.setEncoder(new MimeBDownloadFileNameEncoder());
        
        
        DownloadFileNameEncoderFactory f = new DownloadFileNameEncoderFactory();
        f.setDownloadFileNameEncoderEntries(new ArrayList<DownloadFileNameEncoderEntry>(){{
            add(entry);
            add(entry1);
            add(entry2);
        }});
        
        HttpServer server = TestUtil.createHttpServer();
        server.getHandlerOf(HttpResponseHandler.class).setDownloadFileNameEncoderFactory(f);
        server.addHandler(new Object() {
            public HttpResponse getTest1(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse()
                        .setStatusCode(200)
                        .setContentType("text/plain;charset=shift-jis")
                        .setContentDisposition("データファイル１")
                        .write("てすと１データ");
            }
        }).startLocal();
        
        
        //--------- UAの指定がなければURLエンコーディングを使用 ------//
        
        HttpResponse res = server.handle(
                new MockHttpRequest("GET /test1 HTTP/1.1"), null
        );
        
        String dispositionTemplate = "attachment; filename*=UTF-8''%1$s; filename=\"%2$s\"";
        
        String disposition = String.format(
            dispositionTemplate
          , new UrlDownloadFileNameEncoder().encode("データファイル１")
          , new UrlDownloadFileNameEncoder().encode("データファイル１")
        );
        
        assertEquals(disposition, res.getContentDisposition());
        
        
        
        //----- UA判定によりMimeエンコーディングを使用 ------//
        
        res = server.handle(
                new MockHttpRequest("GET /test1 HTTP/1.1").setHeaderMap(new HashMap() {{
                put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; ja; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 GTB7.1 ( .NET CLR 3.5.30729; .NET4.0C)");
            }}), null
        );
        disposition = String.format(
            dispositionTemplate
          , new UrlDownloadFileNameEncoder().encode("データファイル１")
          , new MimeBDownloadFileNameEncoder().encode("データファイル１")
        );
            
        assertEquals(disposition, res.getContentDisposition());
        
        
        //----- エンコーダの指定が省略されたエントリのテスト(URLエンコーダが使用される。) ------//
        
        res = server.handle(
                new MockHttpRequest("GET /test1 HTTP/1.1").setHeaderMap(new HashMap() {{
                put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            }}), null
        );
        disposition = String.format(
            dispositionTemplate
          , new UrlDownloadFileNameEncoder().encode("データファイル１")
          , new UrlDownloadFileNameEncoder().encode("データファイル１")
        );
            
        assertEquals(disposition, res.getContentDisposition());

        server = TestUtil.createHttpServer();
        server.getHandlerOf(HttpResponseHandler.class).setDownloadFileNameEncoderFactory(f);
        server.addHandler(new Object() {
                    public HttpResponse getTest1(HttpRequest req, ExecutionContext ctx) {
                        return new HttpResponse() {
                            @Override
                            public HttpResponse setContentDisposition(String fileName, boolean inline) {
                                super.setHeader(CONTENT_DISPOSITION, "filename=hoge");
                                return this;
                            }
                        }.setStatusCode(200)
                                .setContentType("text/plain;charset=shift-jis")
                                .setContentDisposition("データファイル１")
                                .write("てすと１データ");
                    }
                }).startLocal();
        res = server.handle(
                new MockHttpRequest("GET /test1 HTTP/1.1").setHeaderMap(new HashMap() {{
                    put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
                }}), null
        );
        assertEquals("filename=hoge", res.getContentDisposition());
    }

    /** リダイレクトの場合、リダイレクト先のURLにjsessionidが付与されること。 */
    @Test
    public void testJsessionidAddedWhenRedirected() {
        HttpServer server = TestUtil.createHttpServer()
                .setWarBasePath("classpath://nablarch/fw/web/sample/app/")
                .addHandler(new SessionConcurrentAccessHandler())
                .addHandler("/redirect", new HttpRequestHandler() {
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        return new HttpResponse().setContentPath("redirect:///index.jsp");
                    }
                })
                .startLocal();

        // 内部リソースへのリダイレクトの場合、jsessionidが付与されること。
        HttpResponse res = server.handle(new MockHttpRequest("GET /redirect HTTP/1.1"), null);
        assertThat("ステータスコードがリダイレクトであること。",
                res.getStatusCode(), is(302));
        String jsessionid = getCookieValue(res.getHeader("Set-Cookie"), "JSESSIONID");
        assertThat("jsessionidが発行されていること。", jsessionid, is(notNullValue()));
        assertThat("リダイレクト先URLにjsessionidが付与されていること。",
                res.getLocation(), containsString("jsessionid=" + jsessionid));

        // Cookieを使ってアクセス
        res = server.handle(new MockHttpRequest("GET /redirect HTTP/1.1\r\nCookie: JSESSIONID=" + jsessionid + "\r\n\r\n"), null);
        assertThat("リダイレクト先URLにjsessionidが付与されていないこと。",
                   res.getLocation(), not(containsString("jsessionid")));
    }

    /** 5u13から導入された形式のリダイレクトの場合、リダイレクト先のURLにjsessionidが付与されないこと。 */
    @Test
    public void testJsessionidNotAddedWhenRedirectedSince5u13NewStyle() {
        HttpServer server = TestUtil.createHttpServer()
                .setWarBasePath("classpath://nablarch/fw/web/sample/app/")
                .addHandler("/redirect", new HttpRequestHandler() {
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        //セッションを作る
                        ctx.getSessionScopeMap();
                        return new HttpResponse().setContentPath("redirect:http://foo/bar/index.jsp");
                    }
                })
                .startLocal();

        HttpResponse res = server.handle(new MockHttpRequest("GET /redirect HTTP/1.1"), null);
        assertThat("ステータスコードがリダイレクトであること。",
                res.getStatusCode(), is(302));
        String jsessionid = getCookieValue(res.getHeader("Set-Cookie"), "JSESSIONID");
        //セッションが作られること自体に問題はない
        assertThat("jsessionidが発行されていること。", jsessionid, is(notNullValue()));
        assertThat("リダイレクト先URLにjsessionidが付与されていないこと。",
                res.getLocation(), not(containsString("jsessionid=")));
    }

    /**
     * ステータスコード(303)を設定してリダイレクトを行う場合、そのステータスコードがレスポンスヘッダーに設定されること。
     * @throws Exception
     */
    @Test
    public void testRedirectWithStatusCode303() throws Exception {
        HttpServer server = TestUtil.createHttpServer()
                .setWarBasePath("classpath://nablarch/fw/web/sample/app/")
                .addHandler("/redirect", new HttpRequestHandler() {
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        return new HttpResponse(303).setContentPath("redirect:///index.jsp");
                    }
                })
                .startLocal();

        HttpResponse res = server.handle(new MockHttpRequest("GET /redirect HTTP/1.1"), null);
        assertThat("ステータスコードが設定したステータスコード(303)であること", res.getStatusCode(), is(303));
        assertThat("ロケーションヘッダが設定されていること", res.getLocation(), is("/index.jsp"));
    }
    
    /**
     * ステータスコード(301)を設定してリダイレクトを行う場合、そのステータスコードがレスポンスヘッダーに設定されること。
     * @throws Exception
     */
    @Test
    public void testRedirectWithStatusCode301() throws Exception {
        HttpServer server = TestUtil.createHttpServer()
                .setWarBasePath("classpath://nablarch/fw/web/sample/app/")
                .addHandler("/redirect", new HttpRequestHandler() {
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        return new HttpResponse(301).setContentPath("redirect:///index.jsp");
                    }
                })
                .startLocal();

        HttpResponse res = server.handle(new MockHttpRequest("GET /redirect HTTP/1.1"), null);
        assertThat("ステータスコードが設定したステータスコード(301)であること", res.getStatusCode(), is(301));
        assertThat("ロケーションヘッダが設定されていること", res.getLocation(), is("/index.jsp"));
    }
    
    /**
     * ステータスコード(307)を設定してリダイレクトを行う場合、そのステータスコードがレスポンスヘッダーに設定されること。
     * @throws Exception
     */
    @Test
    public void testRedirectWithStatusCode307() throws Exception {
        HttpServer server = TestUtil.createHttpServer()
                .setWarBasePath("classpath://nablarch/fw/web/sample/app/")
                .addHandler("/redirect", new HttpRequestHandler() {
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        return new HttpResponse(307).setContentPath("redirect:///index.jsp");
                    }
                })
                .startLocal();

        HttpResponse res = server.handle(new MockHttpRequest("GET /redirect HTTP/1.1"), null);
        assertThat("ステータスコードが設定したステータスコード(307)であること", res.getStatusCode(), is(307));
        assertThat("ロケーションヘッダが設定されていること", res.getLocation(), is("/index.jsp"));
    }
    
    /**
     * リダイレクト対象でないステータスコードを指定してリダイレクトのパスを設定した場合302に強制的に置き換わること
     * @throws Exception
     */
    @Test
    public void testRedirectWithStatusCodeInvalid() throws Exception {
        HttpServer server = TestUtil.createHttpServer()
                .setWarBasePath("classpath://nablarch/fw/web/sample/app/")
                .addHandler("/redirect", new HttpRequestHandler() {
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        return new HttpResponse(304).setContentPath("redirect:///index.jsp");
                    }
                })
                .startLocal();

        HttpResponse res = server.handle(new MockHttpRequest("GET /redirect HTTP/1.1"), null);
        assertThat("ステータスコードは302に置き換わる", res.getStatusCode(), is(302));
        assertThat("ロケーションヘッダが設定されていること", res.getLocation(), containsString("/index.jsp"));
    }
    
    /**
     * ステータスコードに100を設定した場合、それが正しくかえること。
     */
    @Test
    public void testStatusCode100() throws Exception {
        HttpServer server = TestUtil.createHttpServer()
                .setWarBasePath("classpath://nablarch/fw/web/sample/app/")
                .addHandler("/status100", new HttpRequestHandler() {
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        return new HttpResponse(100);
                    }
                })
                .startLocal();

        HttpResponse res = server.handle(new MockHttpRequest("GET /status100 HTTP/1.1"), null);
        assertThat("ステータスコードが設定したステータスコード(100)であること", res.getStatusCode(), is(100));
    }

    /**
     * サーブレットフォーワード時のステータス変換のテスト。
     * デフォルト設定の際、レスポンスに404を設定した場合、最終的なHTTPステータスコードも404であること。
     *
     * また、レスポンスコードの変換設定がHttpAccessLogHandlerにも反映されることを確認するため、
     * HttpAccessLogHandlerが出力するログについて、HttpResponseHandlerと齟齬が無いことを確認する。
     */
    @Test
    public void testDoServletForwardForStatusConvertForDefault() {
        HttpServer server = prepareHandlerForStatusConvert();

        OnMemoryLogWriter.clear();

        HttpResponse res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do HTTP/1.1
        ************************************************************/

        List<String> messages = OnMemoryLogWriter.getMessages("writer.accessLog");
        String endlog = messages.get(1);

        assertTrue(Pattern.compile("status_code = \\[404\\]").matcher(endlog).find());
        assertTrue(Pattern.compile("response_status_code = \\[404\\]").matcher(endlog).find());

        assertThat(res.getStatusCode(), is(404));
    }

    /**
     * サーブレットフォーワード時のステータス変換のテスト。
     * CONVERT_ALL_TO_200を明示的に指定した際、レスポンスに404を設定した場合、最終的なHTTPステータスコードも404であること。
     *
     * また、レスポンスコードの変換設定がHttpAccessLogHandlerにも反映されることを確認するため、
     * HttpAccessLogHandlerが出力するログについて、HttpResponseHandlerと齟齬が無いことを確認する。
     */
    @Test
    public void testDoServletForwardForStatusConvertForOnly400To200() {
        HttpServer server = prepareHandlerForStatusConvert();

        //CONVERT_ALL_TO_200を明示的に指定。
        server.getHandlerOf(HttpResponseHandler.class).setConvertMode("CONVERT_ONLY_400_TO_200");

        OnMemoryLogWriter.clear();

        HttpResponse res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do HTTP/1.1
        ************************************************************/

        List<String> messages = OnMemoryLogWriter.getMessages("writer.accessLog");
        String endlog = messages.get(1);

        assertTrue(Pattern.compile("status_code = \\[404\\]").matcher(endlog).find());
        assertTrue(Pattern.compile("response_status_code = \\[404\\]").matcher(endlog).find());

        assertThat(res.getStatusCode(), is(404));
    }


    /**
     * サーブレットフォーワード時のステータス変換のテスト。
     * 後方互換設定の際、レスポンスに404を設定した場合、最終的なHTTPステータスコードは200であること。
     *
     * また、レスポンスコードの変換設定がHttpAccessLogHandlerにも反映されることを確認するため、
     * HttpAccessLogHandlerが出力するログについて、HttpResponseHandlerと齟齬が無いことを確認する。
     */
    @Test
    public void testDoServletForwardForStatusConvertForConvertAllTo200() {
        HttpServer server = prepareHandlerForStatusConvert();

        //後方互換設定
        server.getHandlerOf(HttpResponseHandler.class).setConvertMode("CONVERT_ALL_TO_200");

        OnMemoryLogWriter.clear();

        HttpResponse res = server.handle(new MockHttpRequest(string()), null);
        /************************************************************
        GET /forward.do HTTP/1.1
        ************************************************************/

        List<String> messages = OnMemoryLogWriter.getMessages("writer.accessLog");
        String endlog = messages.get(1);

        assertTrue(Pattern.compile("status_code = \\[404\\]").matcher(endlog).find());
        assertTrue(Pattern.compile("response_status_code = \\[200\\]").matcher(endlog).find());


        assertThat(res.getStatusCode(), is(200));
    }

    /**
     * ステータスコンバートのテスト用のハンドラを用意する。
     * @return ハンドラ設定済みのHTTP Server
     */
    private HttpServer prepareHandlerForStatusConvert(){
        HttpServer server = TestUtil.createHttpServer()
        .setWarBasePath("classpath://nablarch/fw/web/handler/httpresponsehandler/dummy")
            .addHandler(new HttpAccessLogHandler())
            .addHandler("/*.do", new Object() {
                // /forward.do
                public HttpResponse getforwarddo(HttpRequest req, ExecutionContext ctx) {
                    return new HttpResponse(404,"dummy.jsp");
                }
            })
            .startLocal();

        return server;
    }

    private String getCookieValue(String cookieStr, String name) {
        int start = cookieStr.indexOf(name + "=");
        int end = cookieStr.indexOf(";");
        return cookieStr.substring(start + name.length() + 1, end);
    }

}
