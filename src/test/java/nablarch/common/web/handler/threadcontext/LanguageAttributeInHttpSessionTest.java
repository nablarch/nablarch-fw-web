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

import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * {@link LanguageAttributeInHttpSession}のテスト。
 * @author Kiyohito Itoh
 */
public class LanguageAttributeInHttpSessionTest {
    
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
     * ユーザが選択した言語を保持するテスト。
     */
    @Test
    public void testKeepLanguage() {

        HttpRequestHandler action = new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                LanguageAttributeInHttpUtil.keepLanguage(request, context, request.getParam("paramName_test")[0]);
                return null;
            }
        };
        
        MockServletRequest servletReq = new MockServletRequest();
        MockHttpSession session = new MockHttpSession();
        servletReq.setSession(session);
        MockServletResponse servletRes = new MockServletResponse();
        MockServletContext servletCtx = new MockServletContext();
        HttpRequest req = new MockHttpRequest().setParam("paramName_test", "it");
        
        LanguageAttributeInHttpSession attribute = SystemRepository.get("languageAttribute");
        ThreadContextHandler handler = new ThreadContextHandler(attribute);
        
        attribute.setDefaultLanguage("es"); // spanish
        attribute.setSupportedLanguages("en", "it", "ja", "es", "zh"); // Italian(it), Chinese(zh)
        
        ExecutionContext ctx;
        
        /********************************************************************************
        キー名を指定していない場合
        ********************************************************************************/

        ctx = createExecutionContext(servletReq, servletRes, servletCtx, action);
        
        // ThareadContextHandlerでの呼び出し
        handler.handle(req, ctx);
        
        assertThat("デフォルトのキー名で言語が設定される。", ctx.<Object>getSessionScopedVar(ThreadContext.LANG_KEY).toString(), is("it"));
        
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
        
        assertNull("デフォルトのキー名で言語が設定されない。", ctx.getSessionScopedVar(ThreadContext.LANG_KEY));
        assertThat("プロパティ指定されたキー名で言語が設定される。", ctx.<Object>getSessionScopedVar("sessionKey_test").toString(), is("it"));
    }

    /**
     * アプリケーションのデータベースなどにユーザに紐付けて言語を永続化しているケースを想定したテスト。
     */
    @Test
    public void testApplicationPersistenceLanguage() {

        HttpRequestHandler action = new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                // 決め打ちでzhを設定する。
                LanguageAttributeInHttpUtil.keepLanguage(request, context, "zh");
                return null;
            }
        };

        MockServletRequest servletReq = new MockServletRequest();
        MockHttpSession session = new MockHttpSession();
        servletReq.setSession(session);
        MockServletResponse servletRes = new MockServletResponse();
        MockServletContext servletCtx = new MockServletContext();
        HttpRequest req = new MockHttpRequest();

        LanguageAttributeInHttpSession attribute = SystemRepository.get("languageAttribute");
        ThreadContextHandler handler = new ThreadContextHandler(attribute);
        
        attribute.setDefaultLanguage("es"); // spanish
        attribute.setSupportedLanguages("en", "it", "ja", "es", "zh"); // Italian(it), Chinese(zh)
        
        Locale locale;
        
        /********************************************************************************
        アプリ側で言語を設定する場合
        ********************************************************************************/

        ExecutionContext ctx = createExecutionContext(servletReq, servletRes, servletCtx, action);
        
        // ThareadContextHandlerでの呼び出し
        handler.handle(req, ctx);
        locale = ThreadContext.getLanguage();
        
        assertThat("アプリで設定した言語が返される。", locale.getLanguage(), is("zh"));
        assertThat("デフォルトのキー名で言語が設定される。", ctx.<Object>getSessionScopedVar(ThreadContext.LANG_KEY).toString(), is("zh"));
    }
    
    /**
     * 保持している言語を取得するテスト。
     */
    @Test
    public void testGetKeepingLanguage() {

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

        LanguageAttributeInHttpSession attribute = SystemRepository.get("languageAttribute");
        ThreadContextHandler handler = new ThreadContextHandler(attribute);
        
        attribute.setDefaultLanguage("es"); // spanish
        attribute.setSupportedLanguages("en", "it", "ja", "es", "zh"); // Italian(it), Chinese(zh)
        
        Locale locale;
        
        /********************************************************************************
        保持していない場合
        ********************************************************************************/

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        locale = ThreadContext.getLanguage();
        
        assertThat("デフォルトの言語が返される。", locale.getLanguage(), is("es"));
        
        /********************************************************************************
        サポート対象の言語を保持している場合
        (キー名を指定していない)
        ********************************************************************************/
        
        ctx.setSessionScopedVar(ThreadContext.LANG_KEY, "it");

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        locale = ThreadContext.getLanguage();
        
        assertThat("保持している言語が返される。", locale.getLanguage(), is("it"));
        
        /********************************************************************************
        サポート対象でない言語を保持している場合
        (キー名を指定していない)
        ********************************************************************************/
        
        ctx.setSessionScopedVar(ThreadContext.LANG_KEY, "fr");

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        locale = ThreadContext.getLanguage();
        
        assertThat("デフォルトの言語が返される。", locale.getLanguage(), is("es"));

        /********************************************************************************
        サポート対象の言語を保持している場合
        (キー名を指定している)
        ********************************************************************************/
        
        attribute.setSessionKey("sessionKey_test");
        ctx.setSessionScopedVar("sessionKey_test", "ja");

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        locale = ThreadContext.getLanguage();
        
        assertThat("保持している言語が返される。", locale.getLanguage(), is("ja"));
        
        /********************************************************************************
        サポート対象でない言語を保持している場合
        (キー名を指定している)
        ********************************************************************************/

        attribute.setSessionKey("sessionKey_test");
        ctx.setSessionScopedVar("sessionKey_test", "fr");

        // ThareadContextHandlerでの呼び出し
        handler.handle(req, createExecutionContext(servletReq, servletRes, servletCtx, action));
        locale = ThreadContext.getLanguage();
        
        assertThat("デフォルトの言語が返される。", locale.getLanguage(), is("es"));
    }
}
