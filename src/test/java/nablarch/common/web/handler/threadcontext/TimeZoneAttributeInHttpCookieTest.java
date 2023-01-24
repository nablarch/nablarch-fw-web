package nablarch.common.web.handler.threadcontext;

import nablarch.common.handler.threadcontext.ThreadContextHandler;
import nablarch.core.ThreadContext;
import nablarch.core.exception.IllegalConfigurationException;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.*;
import nablarch.fw.web.handler.HttpResponseHandler;
import nablarch.fw.web.servlet.ServletExecutionContext;
import nablarch.test.support.web.servlet.MockServletContext;
import nablarch.test.support.web.servlet.MockServletRequest;
import nablarch.test.support.web.servlet.MockServletResponse;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * {@link TimeZoneAttributeInHttpSupport}および{@link TimeZoneAttributeInHttpCookie}のテスト。
 * @author Kiyohito Itoh
 */
public class TimeZoneAttributeInHttpCookieTest {

    private static final String HTTP_SERVER_FACTORY_KEY = "httpServerFactory";

    @BeforeClass
    public static void setUpClass() {
        SystemRepository.clear();
    }

    @AfterClass
    public static void tearDownClass() {
        SystemRepository.clear();
    }
    
    @Before
    public void setUp() {
        setUpRepository("nablarch/common/web/handler/threadcontext/cookie.xml");
    }

    private void setUpRepository(String url) {
        XmlComponentDefinitionLoader loader =
            new XmlComponentDefinitionLoader(url);
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
    }
    
    /**
     * リクエストパラメータからタイムゾーンを取得するテスト。
     */
    @Test
    public void testGetTimeZoneOfUserChoice() {
        
        HttpRequestHandler action = new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                return null;
            }
        };
        
        MockServletRequest servletReq = new MockServletRequest();
        MockServletResponse servletRes = new MockServletResponse();
        MockServletContext servletCtx = new MockServletContext();
        HttpRequest req = new MockHttpRequest();
        
        TimeZoneAttributeInHttpCookie attribute = SystemRepository.get("timeZoneAttribute");
        ThreadContextHandler handler = new ThreadContextHandler(attribute);
        
        TimeZone timeZone;
        
        /********************************************************************************
        タイムゾーンパラメータが送信されない場合
        (paramNameプロパティ指定あり)
        ********************************************************************************/
        
        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        timeZone = ThreadContext.getTimeZone();
        assertThat("デフォルトのタイムゾーンが返される。", timeZone.getID(), is("Europe/Madrid"));
        
        /********************************************************************************
        サポート対象のタイムゾーンパラメータが送信される場合
        (paramNameプロパティ指定あり)
        ********************************************************************************/
        
        req = new MockHttpRequest().setParam("paramName_test", "Europe/Rome");
        
        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        timeZone = ThreadContext.getTimeZone();
        assertThat("デフォルトのタイムゾーンが返される。", timeZone.getID(), is("Europe/Madrid"));
        
        // アクションでの呼び出し
        TimeZoneAttributeInHttpUtil.keepTimeZone(req, createExecutionContext(servletReq, servletRes, servletCtx, action), "Europe/Rome");
        timeZone = ThreadContext.getTimeZone();
        assertThat("パラメータ送信されたタイムゾーンが返される。", timeZone.getID(), is("Europe/Rome"));
        Cookie cookie = servletRes.getCookies().get(0);
        assertThat("パラメータ送信されたタイムゾーンがクッキーに設定される。", cookie.getName(), is("cookieNameTest"));
        assertThat("パラメータ送信されたタイムゾーンがクッキーに設定される。", cookie.getValue(), is("Europe/Rome"));
        assertThat("デフォルトではsecure属性は付与されない。", cookie.getSecure(), is(false));
        /********************************************************************************
        サポート対象でないタイムゾーンパラメータが送信される場合
        (paramNameプロパティ指定あり)
        ********************************************************************************/
        
        req = new MockHttpRequest().setParam("paramName_test", "Europe/London");
        
        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        timeZone = ThreadContext.getTimeZone();
        assertThat("デフォルトのタイムゾーンが返される。", timeZone.getID(), is("Europe/Madrid"));

        // アクションでの呼び出し
        TimeZoneAttributeInHttpUtil.keepTimeZone(req, createExecutionContext(servletReq, servletRes, servletCtx, action), "Europe/London");
        timeZone = ThreadContext.getTimeZone();
        assertThat("デフォルトのタイムゾーンが返される。", timeZone.getID(), is("Europe/Madrid"));
    }

    private ServletExecutionContext createExecutionContext(MockServletRequest servletReq,
                                                           MockServletResponse servletRes,
                                                           MockServletContext servletCtx,
                                                           HttpRequestHandler handler) {
        return (ServletExecutionContext) new ServletExecutionContext(servletReq, servletRes, servletCtx).addHandler(handler);
    }
    
    
    /**
     * ユーザが選択したタイムゾーンを保持するテスト。
     */
    @Test
    public void testKeepTimeZone() {
        
        HttpRequestHandler action = new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                TimeZoneAttributeInHttpUtil.keepTimeZone(request, context, request.getParam("paramName_test")[0]);
                return null;
            }
        };
        
        MockServletRequest servletReq = new MockServletRequest();
        servletReq.setContextPath("/contentPath_test");
        MockServletResponse servletRes = new MockServletResponse();
        MockServletContext servletCtx = new MockServletContext();
        HttpRequest req = new MockHttpRequest().setParam("paramName_test", "Europe/Rome");

        TimeZoneAttributeInHttpCookie attribute = SystemRepository.get("timeZoneAttribute");
        ThreadContextHandler handler = new ThreadContextHandler(attribute);
        
        attribute.setDefaultTimeZone("Europe/Madrid");
        attribute.setSupportedTimeZones("America/New_York", "Europe/Rome", "Asia/Tokyo", "Europe/Madrid", "Asia/Shanghai");
        
        Cookie cookie;
        
        /********************************************************************************
        必須プロパティのみ設定した場合
        ********************************************************************************/
        
        attribute.setCookieName("cookieName_test");

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        
        cookie = servletRes.getCookies().get(0);
        
        assertThat("プロパティ指定された名前が設定される。", cookie.getName(), is("cookieName_test"));
        assertThat("パラメータ送信されたタイムゾーンが設定される。", cookie.getValue(), is("Europe/Rome"));
        assertThat("デフォルト値が設定される。", cookie.getPath(), is("/contentPath_test"));
        assertThat("デフォルト値が設定される。", cookie.getMaxAge(), is(-1));
        assertNull("ドメイン階層は設定されない。", cookie.getDomain());
        assertThat("デフォルトではsecure属性は付与されない。", cookie.getSecure(), is(false));

        /********************************************************************************
        アプリのコンテキストパスがブランクの場合
        ********************************************************************************/

        servletRes = new MockServletResponse();
        servletReq.setContextPath("");

        attribute.setCookieName("cookieName_test");

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));

        cookie = servletRes.getCookies().get(0);

        assertThat("プロパティ指定された名前が設定される。", cookie.getName(), is("cookieName_test"));
        assertThat("パラメータ送信されたタイムゾーンが設定される。", cookie.getValue(), is("Europe/Rome"));
        assertThat("/が設定される。", cookie.getPath(), is("/"));
        assertThat("デフォルト値が設定される。", cookie.getMaxAge(), is(-1));
        assertNull("ドメイン階層は設定されない。", cookie.getDomain());

        /********************************************************************************
        全てのプロパティを設定した場合
        ********************************************************************************/
        
        servletRes = new MockServletResponse();
        
        attribute.setCookieName("cookieName_test");
        attribute.setCookiePath("/aaa/bbb/ccc");
        attribute.setCookieDomain("xxx.yyy.zzz");
        attribute.setCookieMaxAge(31104000);
        attribute.setCookieSecure(true);

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        
        cookie = servletRes.getCookies().get(0);
        
        assertThat("プロパティ指定された名前が設定される。", cookie.getName(), is("cookieName_test"));
        assertThat("パラメータ送信されたタイムゾーンが設定される。", cookie.getValue(), is("Europe/Rome"));
        assertThat("プロパティ指定されたパス階層が設定される。", cookie.getPath(), is("/aaa/bbb/ccc"));
        assertThat("プロパティ指定された最長存続期間(秒単位)が設定される。", cookie.getMaxAge(), is(31104000));
        assertThat("プロパティ指定されたドメイン階層が設定される。", cookie.getDomain(), is("xxx.yyy.zzz"));
        assertThat("プロパティ指定されたsecure属性が付与される。", cookie.getSecure(), is(true));
    }
    
    /**
     * 保持しているタイムゾーンを取得するテスト。
     */
    @Test
    public void testGetKeepingTimeZone() {

        HttpRequestHandler action = new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                return null;
            }
        };
        
        MockServletRequest servletReq = new MockServletRequest();
        MockServletResponse servletRes = new MockServletResponse();
        MockServletContext servletCtx = new MockServletContext();
        HttpRequest req = new MockHttpRequest();

        TimeZoneAttributeInHttpCookie attribute = SystemRepository.get("timeZoneAttribute");
        ThreadContextHandler handler = new ThreadContextHandler(attribute);
        
        attribute.setDefaultTimeZone("Europe/Madrid");
        attribute.setSupportedTimeZones("America/New_York", "Europe/Rome", "Asia/Tokyo", "Europe/Madrid", "Asia/Shangha");
        attribute.setCookieName("cookieName_test");
        
        TimeZone timeZone;
        
        /********************************************************************************
        保持していない場合
        ********************************************************************************/

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        timeZone = ThreadContext.getTimeZone();
        
        assertThat("デフォルトのタイムゾーンが返される。", timeZone.getID(), is("Europe/Madrid"));
        
        /********************************************************************************
        サポート対象のタイムゾーンを保持している場合
        ********************************************************************************/
        
        servletReq = new MockServletRequest();
        servletReq.setCookies(new Cookie("other", "Asia/Tokyo"), new Cookie("cookieName_test", "Europe/Rome"));

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        timeZone = ThreadContext.getTimeZone();
        
        assertThat("保持しているタイムゾーンが返される。", timeZone.getID(), is("Europe/Rome"));
        
        /********************************************************************************
        サポート対象でないタイムゾーンを保持している場合
        ********************************************************************************/
        
        servletReq = new MockServletRequest();
        servletReq.setCookies(new Cookie("other", "Asia/Tokyo"), new Cookie("cookieName_test", "Europe/London"));

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        timeZone = ThreadContext.getTimeZone();
        
        assertThat("デフォルトのタイムゾーンが返される。", timeZone.getID(), is("Europe/Madrid"));

        /********************************************************************************
        クッキー配列のサイズが0の場合
        ********************************************************************************/
        
        servletReq = new MockServletRequest();
        servletReq.setCookies(new Cookie[0]);

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        timeZone = ThreadContext.getTimeZone();
        
        assertThat("デフォルトのタイムゾーンが返される。", timeZone.getID(), is("Europe/Madrid"));
    }

    /** Cookieにsecure属性を付与する設定の場合、Cookieにsecure属性が設定されること */
    @Test
    public void testSecureCookie() {
        // セキュア設定のコンポーネント定義を読み込み
        setUpRepository("nablarch/common/web/handler/threadcontext/cookie_secure.xml");
        HttpServer server = createServer();
        HttpResponse res = server.startLocal()
                                 .handle(new MockHttpRequest("GET / HTTP/1.1"), new ExecutionContext());

        // HttpCookie#valueOf(String)がおかしいのでtoStringしてアサート
        // Jetty9にてSet-Cookieの各ディレクティブの間に半角スペースが入るようになったため削除してからアサート
        assertThat("Cookieにsecure属性が付与されていること",
                   res.toString().replaceAll(" ", ""),
                   containsString(
                           "Set-Cookie:nablarch_timeZone=Asia/Tokyo;Path=/;Secure"));
    }

    /** Cookieにsecure属性を付与する設定でない場合、Cookieにsecure属性が設定されないこと */
    @Test
    public void testSecureCookieFalse() {
        HttpServer server = createServer();
        HttpResponse res = server.startLocal()
                                 .handle(new MockHttpRequest("GET / HTTP/1.1"), new ExecutionContext());
        // HttpCookie#valueOf(String)がおかしいのでtoStringしてアサート
        // Jetty9にてSet-Cookieの各ディレクティブの間に半角スペースが入るようになったため削除してからアサート
        assertThat(res.toString().replaceAll(" ", ""), containsString(
                "Set-Cookie:cookieNameTest=Asia/Tokyo;Path=/"));

        assertThat("Secure属性が付与されていないこと",
                   res.toString(),
                   not(containsString("Secure")));
    }

    /**
     * {@link HttpServer}を生成する。
     * @return クッキーの設定を行うHttpServer
     */
    private HttpServer createServer() {
        HttpServerFactory factory = SystemRepository.get(HTTP_SERVER_FACTORY_KEY);
        if (factory == null) {
            throw new IllegalConfigurationException("could not find component. name=[" + HTTP_SERVER_FACTORY_KEY + "].");
        }
        return factory.create().setHandlerQueue(Arrays.asList(
                new HttpResponseHandler(),
                new HttpRequestHandler() {
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        // クッキーに設定
                        TimeZoneAttributeInHttpUtil.keepTimeZone(req, ctx, "Asia/Tokyo");
                        return new HttpResponse(200);
                    }
                })
        );
    }


}
