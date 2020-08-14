package nablarch.common.web.handler.threadcontext;

import nablarch.common.handler.threadcontext.ThreadContextHandler;
import nablarch.common.web.handler.MockHttpSession;
import nablarch.core.ThreadContext;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;
import nablarch.test.support.web.servlet.MockServletContext;
import nablarch.test.support.web.servlet.MockServletRequest;
import nablarch.test.support.web.servlet.MockServletResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * {@link TimeZoneAttributeInHttpSession}のテスト。
 * @author Kiyohito Itoh
 */
public class TimeZoneAttributeInHttpSessionTest {
    
    @Before
    public void setUp() {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("nablarch/common/web/handler/threadcontext/session.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
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
        MockHttpSession session = new MockHttpSession();
        servletReq.setSession(session);
        MockServletResponse servletRes = new MockServletResponse();
        MockServletContext servletCtx = new MockServletContext();
        HttpRequest req = new MockHttpRequest().setParam("paramName_test", "Europe/Rome");
        
        TimeZoneAttributeInHttpSession attribute = SystemRepository.get("timeZoneAttribute");
        ThreadContextHandler handler = new ThreadContextHandler(attribute);
        
        attribute.setDefaultTimeZone("Europe/Madrid"); // spanish
        attribute.setSupportedTimeZones("America/New_York", "Europe/Rome", "Asia/Tokyo", "Europe/Madrid", "Asia/Shanghai"); // Italian(it), Chinese(zh)
        
        ExecutionContext ctx;
        
        /********************************************************************************
        キー名を指定していない場合
        ********************************************************************************/

        ctx = createExecutionContext(servletReq, servletRes, servletCtx, action);
        
        // ThareadContextHandlerでの呼び出し
        handler.handle(req, ctx);
        
        assertThat("デフォルトのキー名でタイムゾーンが設定される。", ctx.<Object>getSessionScopedVar(ThreadContext.TIME_ZONE_KEY).toString(), is("Europe/Rome"));
        
        /********************************************************************************
        キー名を指定した場合
        ********************************************************************************/
        
        servletReq = new MockServletRequest();
        session = new MockHttpSession();
        servletReq.setSession(session);
        ctx = createExecutionContext(servletReq, servletRes, servletCtx, action);
        
        attribute.setSessionKey("sessionKey_test");
        
        // ThareadContextHandlerでの呼び出し
        handler.handle(req, ctx);
        
        assertNull("デフォルトのキー名でタイムゾーンが設定されない。", ctx.getSessionScopedVar(ThreadContext.TIME_ZONE_KEY));
        assertThat("プロパティ指定されたキー名でタイムゾーンが設定される。", ctx.<Object>getSessionScopedVar("sessionKey_test").toString(), is("Europe/Rome"));
    }

    /**
     * アプリケーションのデータベースなどにユーザに紐付けてタイムゾーンを永続化しているケースを想定したテスト。
     */
    @Test
    public void testApplicationPersistenceTimeZone() {

        HttpRequestHandler action = new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                // 決め打ちでzhを設定する。
                TimeZoneAttributeInHttpUtil.keepTimeZone(request, context, "Asia/Shanghai");
                return null;
            }
        };

        MockServletRequest servletReq = new MockServletRequest();
        MockHttpSession session = new MockHttpSession();
        servletReq.setSession(session);
        MockServletResponse servletRes = new MockServletResponse();
        MockServletContext servletCtx = new MockServletContext();
        HttpRequest req = new MockHttpRequest();

        TimeZoneAttributeInHttpSession attribute = SystemRepository.get("timeZoneAttribute");
        ThreadContextHandler handler = new ThreadContextHandler(attribute);
        
        attribute.setDefaultTimeZone("Europe/Madrid"); // spanish
        attribute.setSupportedTimeZones("America/New_York", "Europe/Rome", "Asia/Tokyo", "Europe/Madrid", "Asia/Shanghai"); // Italian(it), Chinese(zh)
        
        TimeZone timeZone;
        
        /********************************************************************************
        アプリ側でタイムゾーンを設定する場合
        ********************************************************************************/

        ExecutionContext ctx = createExecutionContext(servletReq, servletRes, servletCtx, action);
        
        // ThareadContextHandlerでの呼び出し
        handler.handle(req, ctx);
        timeZone = ThreadContext.getTimeZone();
        
        assertThat("アプリで設定したタイムゾーンが返される。", timeZone.getID(), is("Asia/Shanghai"));
        assertThat("デフォルトのキー名でタイムゾーンが設定される。", ctx.<Object>getSessionScopedVar(ThreadContext.TIME_ZONE_KEY).toString(), is("Asia/Shanghai"));
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
        MockHttpSession session = new MockHttpSession();
        servletReq.setSession(session);
        MockServletResponse servletRes = new MockServletResponse();
        MockServletContext servletCtx = new MockServletContext();
        HttpRequest req = new MockHttpRequest();
        ServletExecutionContext ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);

        TimeZoneAttributeInHttpSession attribute = SystemRepository.get("timeZoneAttribute");
        ThreadContextHandler handler = new ThreadContextHandler(attribute);
        
        attribute.setDefaultTimeZone("Europe/Madrid"); // spanish
        attribute.setSupportedTimeZones("America/New_York", "Europe/Rome", "Asia/Tokyo", "Europe/Madrid", "Asia/Shanghai"); // Italian(it), Chinese(zh)
        
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
        (キー名を指定していない)
        ********************************************************************************/
        
        ctx.setSessionScopedVar(ThreadContext.TIME_ZONE_KEY, "Europe/Rome");

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        timeZone = ThreadContext.getTimeZone();
        
        assertThat("保持しているタイムゾーンが返される。", timeZone.getID(), is("Europe/Rome"));
        
        /********************************************************************************
        サポート対象でないタイムゾーンを保持している場合
        (キー名を指定していない)
        ********************************************************************************/
        
        ctx.setSessionScopedVar(ThreadContext.TIME_ZONE_KEY, "Europe/London");

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        timeZone = ThreadContext.getTimeZone();
        
        assertThat("デフォルトのタイムゾーンが返される。", timeZone.getID(), is("Europe/Madrid"));

        /********************************************************************************
        サポート対象のタイムゾーンを保持している場合
        (キー名を指定している)
        ********************************************************************************/
        
        attribute.setSessionKey("sessionKey_test");
        ctx.setSessionScopedVar("sessionKey_test", "Asia/Tokyo");

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        timeZone = ThreadContext.getTimeZone();
        
        assertThat("保持しているタイムゾーンが返される。", timeZone.getID(), is("Asia/Tokyo"));
        
        /********************************************************************************
        サポート対象でないタイムゾーンを保持している場合
        (キー名を指定している)
        ********************************************************************************/

        attribute.setSessionKey("sessionKey_test");
        ctx.setSessionScopedVar("sessionKey_test", "Europe/London");

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        timeZone = ThreadContext.getTimeZone();
        
        assertThat("デフォルトのタイムゾーンが返される。", timeZone.getID(), is("Europe/Madrid"));
    }
}
